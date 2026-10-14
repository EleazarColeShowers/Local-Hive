package com.eleazar.localhive.ui.polls

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.eleazar.localhive.domain.AuthRepository
import com.eleazar.localhive.domain.EstateRepository
import com.eleazar.localhive.domain.PollRepository
import com.eleazar.localhive.domain.UserRepository
import com.eleazar.localhive.domain.model.Poll
import com.eleazar.localhive.ui.util.nowMillis
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** The most choices a poll can offer. */
const val MAX_POLL_OPTIONS = 6

data class PollsUiState(
    val estateId: String? = null,
    val currentUserId: String? = null,
    val isAdmin: Boolean = false,
    /** Newest first. */
    val polls: List<Poll> = emptyList(),
    /** The option the user picked, by poll id. */
    val myVotes: Map<String, Int> = emptyMap(),
    val isLoading: Boolean = true,
    val isSaving: Boolean = false,
    val pollCreated: Boolean = false,
    val error: String? = null
) {
    fun openPolls(now: Long = nowMillis()): List<Poll> = polls.filter { it.isOpen(now) }
}

class PollsViewModel(
    private val authRepository: AuthRepository,
    private val userRepository: UserRepository,
    private val estateRepository: EstateRepository,
    private val pollRepository: PollRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(PollsUiState())
    val uiState = _uiState.asStateFlow()

    private var userName = "Admin"

    init {
        val userId = authRepository.getCurrentUserId()
        _uiState.update { it.copy(currentUserId = userId) }
        if (userId != null) viewModelScope.launch {
            val user = userRepository.getUser(userId, cacheFirst = true).getOrNull()
            val estateId = user?.estateId
            if (estateId == null) {
                _uiState.update { it.copy(isLoading = false) }
                return@launch
            }
            userName = user.displayName ?: user.username ?: userName
            _uiState.update { it.copy(estateId = estateId) }
            launch {
                // Live, so being made (or unmade) an admin shows straight away.
                estateRepository.observeEstate(estateId).collect { estate ->
                    _uiState.update { it.copy(isAdmin = estate?.isAdmin(userId) == true) }
                }
            }
            launch {
                pollRepository.observePolls(estateId)
                    .catch { e -> _uiState.update { it.copy(isLoading = false, error = e.message) } }
                    .collect { polls -> _uiState.update { it.copy(polls = polls, isLoading = false) } }
            }
            launch {
                pollRepository.observeMyVotes(estateId, userId)
                    .catch { /* Without them every poll just offers its vote buttons. */ }
                    .collect { votes -> _uiState.update { it.copy(myVotes = it.myVotes + votes) } }
            }
        }
    }

    /** @param durationMillis how long voting stays open, or null until an admin closes it */
    fun createPoll(question: String, options: List<String>, durationMillis: Long?) {
        val state = _uiState.value
        val estateId = state.estateId ?: return
        val userId = state.currentUserId ?: return
        val choices = options.map { it.trim() }.filter { it.isNotEmpty() }.distinct()
        if (question.isBlank() || choices.size < 2 || state.isSaving) return
        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true, error = null) }
            val now = nowMillis()
            val poll = Poll(
                question = question.trim(),
                options = choices.take(MAX_POLL_OPTIONS),
                createdBy = userId,
                createdByName = userName,
                createdAt = now,
                closesAt = durationMillis?.let { now + it } ?: 0L
            )
            pollRepository.createPoll(estateId, poll)
                .onSuccess { _uiState.update { it.copy(isSaving = false, pollCreated = true) } }
                .onFailure { e -> _uiState.update { it.copy(isSaving = false, error = e.message ?: "Couldn't create the poll") } }
        }
    }

    fun consumePollCreated() {
        _uiState.update { it.copy(pollCreated = false) }
    }

    /** Shows the vote straight away, and takes it back if it doesn't go through. */
    fun vote(pollId: String, option: Int) {
        val state = _uiState.value
        val estateId = state.estateId ?: return
        val userId = state.currentUserId ?: return
        if (pollId in state.myVotes) return
        _uiState.update { it.copy(myVotes = it.myVotes + (pollId to option), error = null) }
        viewModelScope.launch {
            pollRepository.vote(estateId, pollId, userId, option).onFailure { e ->
                _uiState.update {
                    it.copy(myVotes = it.myVotes - pollId, error = e.message ?: "Couldn't record your vote")
                }
            }
        }
    }

    fun closePoll(pollId: String) {
        val estateId = _uiState.value.estateId ?: return
        viewModelScope.launch {
            pollRepository.closePoll(estateId, pollId)
                .onFailure { e -> _uiState.update { it.copy(error = e.message ?: "Couldn't close the poll") } }
        }
    }

    fun deletePoll(pollId: String) {
        val estateId = _uiState.value.estateId ?: return
        viewModelScope.launch {
            pollRepository.deletePoll(estateId, pollId)
                .onFailure { e -> _uiState.update { it.copy(error = e.message ?: "Couldn't delete the poll") } }
        }
    }
}
