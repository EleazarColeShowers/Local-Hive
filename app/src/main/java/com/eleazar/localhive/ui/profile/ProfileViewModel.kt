package com.eleazar.localhive.ui.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.eleazar.localhive.domain.AuthRepository
import com.eleazar.localhive.domain.EstateRepository
import com.eleazar.localhive.domain.UserRepository
import com.eleazar.localhive.domain.model.Estate
import com.eleazar.localhive.domain.model.User
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ProfileUiState(
    val user: User? = null,
    val estate: Estate? = null,
    val isLoading: Boolean = false,
    val error: String? = null,
    val isLoggedOut: Boolean = false,
    val hasLeftEstate: Boolean = false
)

@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val userRepository: UserRepository,
    private val estateRepository: EstateRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ProfileUiState())
    val uiState = _uiState.asStateFlow()

    init {
        loadProfile()
    }

    private fun loadProfile() {
        val userId = authRepository.getCurrentUserId() ?: return
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            userRepository.getUser(userId).fold(
                onSuccess = { user ->
                    _uiState.update { it.copy(user = user, isLoading = false) }
                    user.estateId?.let { loadEstate(it) }
                },
                onFailure = { e -> _uiState.update { it.copy(error = e.message, isLoading = false) } }
            )
        }
    }

    private fun loadEstate(estateId: String) {
        viewModelScope.launch {
            estateRepository.getEstate(estateId).fold(
                onSuccess = { estate -> _uiState.update { it.copy(estate = estate) } },
                onFailure = { /* non-critical, ignore */ }
            )
        }
    }

    fun updateProfile(displayName: String, username: String, bio: String) {
        val user = _uiState.value.user ?: return
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            val updated = user.copy(displayName = displayName, username = username, bio = bio)
            userRepository.updateUser(updated).fold(
                onSuccess = { _uiState.update { it.copy(user = updated, isLoading = false) } },
                onFailure = { e -> _uiState.update { it.copy(error = e.message, isLoading = false) } }
            )
        }
    }

    fun leaveEstate() {
        val user = _uiState.value.user ?: return
        val estateId = user.estateId ?: return
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            estateRepository.leaveEstate(estateId, user.id).fold(
                onSuccess = { _uiState.update { it.copy(isLoading = false, hasLeftEstate = true, estate = null) } },
                onFailure = { e -> _uiState.update { it.copy(isLoading = false, error = e.message) } }
            )
        }
    }

    fun logout() {
        viewModelScope.launch {
            authRepository.logout()
            _uiState.update { it.copy(isLoggedOut = true) }
        }
    }
}
