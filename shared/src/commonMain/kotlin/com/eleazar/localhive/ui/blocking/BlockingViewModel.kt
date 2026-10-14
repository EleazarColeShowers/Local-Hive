package com.eleazar.localhive.ui.blocking

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.eleazar.localhive.domain.AuthRepository
import com.eleazar.localhive.domain.UserRepository
import com.eleazar.localhive.domain.model.BlockedUser
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class BlockingUiState(
    val blocked: List<BlockedUser> = emptyList(),
    val isLoading: Boolean = true,
    val error: String? = null
) {
    fun isBlocked(userId: String?): Boolean = userId != null && blocked.any { it.id == userId }
}

/** Blocking and unblocking, for any screen that offers it. */
class BlockingViewModel(
    authRepository: AuthRepository,
    private val userRepository: UserRepository
) : ViewModel() {

    private val userId = authRepository.getCurrentUserId()

    private val _uiState = MutableStateFlow(BlockingUiState())
    val uiState = _uiState.asStateFlow()

    init {
        userId?.let { id ->
            viewModelScope.launch {
                userRepository.observeBlockedUsers(id).collect { blocked ->
                    _uiState.update { it.copy(blocked = blocked, isLoading = false) }
                }
            }
        }
    }

    fun block(targetId: String, name: String, photoUrl: String?) {
        val id = userId ?: return
        viewModelScope.launch {
            userRepository.blockUser(id, BlockedUser(id = targetId, name = name, photoUrl = photoUrl))
                .onFailure { e -> _uiState.update { it.copy(error = e.message ?: "Couldn't block them") } }
        }
    }

    fun unblock(targetId: String) {
        val id = userId ?: return
        viewModelScope.launch {
            userRepository.unblockUser(id, targetId)
                .onFailure { e -> _uiState.update { it.copy(error = e.message ?: "Couldn't unblock them") } }
        }
    }

    fun clearError() = _uiState.update { it.copy(error = null) }
}
