package com.eleazar.localhive.ui.directory

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.eleazar.localhive.domain.AuthRepository
import com.eleazar.localhive.domain.EstateRepository
import com.eleazar.localhive.domain.UserRepository
import com.eleazar.localhive.domain.model.User
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class DirectoryViewModel(
    private val userRepository: UserRepository,
    private val authRepository: AuthRepository,
    private val estateRepository: EstateRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(DirectoryUiState())
    val uiState = _uiState.asStateFlow()

    private var estateId: String? = null
    private var membersJob: Job? = null
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
                collectMembers(estate, forceRefresh = false)
                estateRepository.observeEstate(estate).collect { e ->
                    if (e == null) return@collect
                    _uiState.update { it.copy(adminIds = e.adminIds.toSet()) }
                    // Someone joined or left since the saved copy: fetch it fresh, once.
                    val loaded = _uiState.value
                    if (!loaded.isLoading && !refreshedForCount && e.memberCount != loaded.members.size) {
                        refreshedForCount = true
                        collectMembers(estate, forceRefresh = true)
                    }
                }
            }.onFailure {
                _uiState.update { state -> state.copy(isLoading = false, error = it.message) }
            }
        }
    }

    /** Pull to refresh: always goes to the server. */
    fun refresh() {
        val estate = estateId ?: return
        _uiState.update { it.copy(isRefreshing = true) }
        collectMembers(estate, forceRefresh = true)
    }

    private fun collectMembers(estateId: String, forceRefresh: Boolean) {
        membersJob?.cancel()
        membersJob = viewModelScope.launch {
            userRepository.observeDirectory(estateId, forceRefresh)
                .onCompletion { _uiState.update { it.copy(isRefreshing = false, isLoading = false) } }
                .catch { e -> _uiState.update { it.copy(error = e.message) } }
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
