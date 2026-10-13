package com.eleazar.localhive.ui.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.eleazar.localhive.domain.AuthRepository
import com.eleazar.localhive.domain.UserRepository
import com.eleazar.localhive.domain.model.User
import com.google.firebase.messaging.FirebaseMessaging
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

data class AuthUiState(
    val isLoading: Boolean = false,
    val user: User? = null,
    val error: String? = null,
    val reset: PasswordResetState = PasswordResetState.Idle
)

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val userRepository: UserRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(AuthUiState())
    val uiState = _uiState.asStateFlow()

    fun login(email: String, password: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            authRepository.login(email, password).fold(
                onSuccess = { user ->
                    _uiState.update { it.copy(isLoading = false, user = user) }
                    syncFcmToken(user.id)
                },
                onFailure = { e -> _uiState.update { it.copy(isLoading = false, error = e.message) } }
            )
        }
    }

    fun sendPasswordReset(email: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(reset = PasswordResetState.Sending) }
            val next = authRepository.sendPasswordReset(email).fold(
                onSuccess = { PasswordResetState.Sent(email) },
                onFailure = { e -> PasswordResetState.Failed(e.message ?: "Couldn't send the reset email. Try again.") }
            )
            _uiState.update { it.copy(reset = next) }
        }
    }

    fun clearPasswordReset() {
        _uiState.update { it.copy(reset = PasswordResetState.Idle) }
    }

    fun signup(email: String, username: String, password: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            authRepository.signup(email, username, password).fold(
                onSuccess = { user ->
                    _uiState.update { it.copy(isLoading = false, user = user) }
                    syncFcmToken(user.id)
                },
                onFailure = { e -> _uiState.update { it.copy(isLoading = false, error = e.message) } }
            )
        }
    }

    fun saveProfile(displayName: String, username: String, bio: String) {
        val currentUser = _uiState.value.user ?: return
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            val updated = currentUser.copy(displayName = displayName, username = username, bio = bio)
            authRepository.saveUserProfile(updated).fold(
                onSuccess = { _uiState.update { it.copy(isLoading = false, user = updated) } },
                onFailure = { e -> _uiState.update { it.copy(isLoading = false, error = e.message) } }
            )
        }
    }

    /**
     * onNewToken only fires when FCM rotates a token, which on a fresh install happens
     * before anyone has signed in — so without this the first session is never reachable
     * by push.
     */
    private suspend fun syncFcmToken(userId: String) {
        try {
            val token = FirebaseMessaging.getInstance().token.await()
            userRepository.updateFcmToken(userId, token)
        } catch (_: Exception) {
            // Best-effort: SplashViewModel retries on the next launch.
        }
    }

    fun clearUser() {
        _uiState.update { it.copy(user = null) }
    }

    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }
}
