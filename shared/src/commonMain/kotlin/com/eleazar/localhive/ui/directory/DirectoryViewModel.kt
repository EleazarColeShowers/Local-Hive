package com.eleazar.localhive.ui.directory

import com.eleazar.localhive.ui.util.friendlyMessage
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.eleazar.localhive.domain.AuthRepository
import com.eleazar.localhive.domain.EstateRepository
import com.eleazar.localhive.domain.UserRepository
import com.eleazar.localhive.domain.model.User
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Estates up to this size load whole, from the phone's saved copy, and search on the phone.
 * Bigger ones load a page at a time and search on the server, so nobody downloads every profile.
 */
internal const val DIRECTORY_FULL_LOAD_MAX = 300
private const val PAGE_SIZE = 50
private const val SEARCH_LIMIT = 30
private const val SEARCH_DEBOUNCE_MILLIS = 300L

class DirectoryViewModel(
    private val userRepository: UserRepository,
    private val authRepository: AuthRepository,
    private val estateRepository: EstateRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(DirectoryUiState())
    val uiState = _uiState.asStateFlow()

    private var estateId: String? = null
    private var membersJob: Job? = null
    private var pageJob: Job? = null
    private var searchJob: Job? = null
    /** The estate's member count has already prompted one refresh; it can drift, so only once. */
    private var refreshedForCount = false

    init {
        val userId = authRepository.getCurrentUserId()
        _uiState.update { it.copy(currentUserId = userId) }
        userId?.let { loadMembers(it) }
    }

    private fun loadMembers(userId: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            userRepository.getUser(userId, cacheFirst = true).onSuccess { user ->
                val estate = user.estateId ?: return@onSuccess _uiState.update { it.copy(isLoading = false) }
                estateId = estate
                var started = false
                estateRepository.observeEstate(estate).collect { e ->
                    if (e == null) return@collect
                    _uiState.update { it.copy(adminIds = e.adminIds.toSet(), memberCount = e.memberCount) }
                    // Whether to page is settled by the first look at the estate, so the list
                    // doesn't switch modes under someone as people join.
                    if (!started) {
                        started = true
                        if (e.memberCount > DIRECTORY_FULL_LOAD_MAX) {
                            _uiState.update { it.copy(paged = true) }
                            loadPage(estate, reset = true)
                        } else {
                            collectMembers(estate, forceRefresh = false)
                        }
                        return@collect
                    }
                    // Someone joined or left since the saved copy: fetch it fresh, once.
                    val loaded = _uiState.value
                    if (!loaded.paged && !loaded.isLoading && !refreshedForCount && e.memberCount != loaded.members.size) {
                        refreshedForCount = true
                        collectMembers(estate, forceRefresh = true)
                    }
                }
            }.onFailure {
                _uiState.update { state -> state.copy(isLoading = false, error = it.friendlyMessage()) }
            }
        }
    }

    /** Pull to refresh: always goes to the server. */
    fun refresh() {
        val estate = estateId ?: return
        _uiState.update { it.copy(isRefreshing = true) }
        if (_uiState.value.paged) loadPage(estate, reset = true) else collectMembers(estate, forceRefresh = true)
    }

    private fun collectMembers(estateId: String, forceRefresh: Boolean) {
        membersJob?.cancel()
        membersJob = viewModelScope.launch {
            userRepository.observeDirectory(estateId, forceRefresh)
                .onCompletion { _uiState.update { it.copy(isRefreshing = false, isLoading = false) } }
                .catch { e -> _uiState.update { it.copy(error = e.friendlyMessage()) } }
                .collect { members ->
                    _uiState.update { state ->
                        state.copy(
                            members = members,
                            filteredMembers = matching(members, state.searchQuery, state.currentUserId),
                            isLoading = false
                        )
                    }
                }
        }
    }

    /** Paged estates: the list scrolled near its end. */
    fun loadMore() {
        val estate = estateId ?: return
        val state = _uiState.value
        if (!state.paged || !state.canLoadMore || state.isLoadingMore || state.searchQuery.isNotBlank()) return
        loadPage(estate, reset = false)
    }

    private fun loadPage(estateId: String, reset: Boolean) {
        if (!reset && pageJob?.isActive == true) return
        pageJob?.cancel()
        pageJob = viewModelScope.launch {
            _uiState.update { it.copy(isLoadingMore = !reset) }
            val after = if (reset) null else _uiState.value.members.lastOrNull()?.id
            userRepository.directoryPage(estateId, after, PAGE_SIZE)
                .onSuccess { page ->
                    _uiState.update { state ->
                        val members = if (reset) page else state.members + page
                        state.copy(
                            members = members,
                            filteredMembers = if (state.searchQuery.isBlank()) matching(members, "", state.currentUserId) else state.filteredMembers,
                            canLoadMore = page.size == PAGE_SIZE,
                            isLoading = false,
                            isLoadingMore = false,
                            isRefreshing = false
                        )
                    }
                }
                .onFailure { e ->
                    _uiState.update {
                        it.copy(isLoading = false, isLoadingMore = false, isRefreshing = false, error = e.friendlyMessage())
                    }
                }
        }
    }

    fun loadUserProfile(userId: String) {
        viewModelScope.launch {
            userRepository.getUser(userId).onSuccess { user ->
                _uiState.update { it.copy(selectedUser = user) }
            }
        }
    }

    fun search(query: String) {
        _uiState.update { state ->
            state.copy(searchQuery = query, filteredMembers = matching(state.members, query, state.currentUserId))
        }
        if (!_uiState.value.paged) return

        // Paged: what's loaded shows straight away, then the server's matches join it.
        searchJob?.cancel()
        val estate = estateId ?: return
        if (query.isBlank()) {
            _uiState.update { it.copy(isSearching = false) }
            return
        }
        searchJob = viewModelScope.launch {
            delay(SEARCH_DEBOUNCE_MILLIS)
            _uiState.update { it.copy(isSearching = true) }
            val found = userRepository.searchDirectory(estate, query, SEARCH_LIMIT).getOrDefault(emptyList())
            _uiState.update { state ->
                if (state.searchQuery != query) return@update state
                val local = matching(state.members, query, state.currentUserId)
                val remote = found.filter { it.id != state.currentUserId && local.none { l -> l.id == it.id } }
                state.copy(filteredMembers = local + remote, isSearching = false)
            }
        }
    }

    /** Everyone but the user, narrowed to names containing [query]. */
    private fun matching(members: List<User>, query: String, currentUserId: String?): List<User> =
        members.filter { user ->
            user.id != currentUserId && (
                query.isBlank() ||
                    user.displayName?.contains(query, ignoreCase = true) == true ||
                    user.username?.contains(query, ignoreCase = true) == true
                )
        }

    fun clearSelectedUser() = _uiState.update { it.copy(selectedUser = null) }
}
