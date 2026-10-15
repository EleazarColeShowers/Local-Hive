package com.eleazar.localhive.ui.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.eleazar.localhive.domain.AuthRepository
import com.eleazar.localhive.domain.EstateRepository
import com.eleazar.localhive.domain.StorageRepository
import com.eleazar.localhive.domain.UserRepository
import com.eleazar.localhive.domain.model.Estate
import com.eleazar.localhive.domain.model.User
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ProfileUiState(
    val user: User? = null,
    val estate: Estate? = null,
    val isLoading: Boolean = false,
    val error: String? = null
)

class ProfileViewModel(
    private val authRepository: AuthRepository,
    private val userRepository: UserRepository,
    private val estateRepository: EstateRepository,
    private val storageRepository: StorageRepository
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
            // Stay current with admin changes, including a new invite code.
            combine(
                estateRepository.observeEstate(estateId),
                estateRepository.observeInviteCode(estateId)
            ) { live, code -> live?.copy(inviteCode = code.orEmpty()) }.collect { live ->
                if (live != null) _uiState.update { it.copy(estate = live) }
            }
        }
    }

    fun updateProfile(displayName: String, username: String, bio: String, imageUri: String? = null) {
        val user = _uiState.value.user ?: return
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }

            // An unchanged avatar keeps the URL already on the profile.
            val imageUrl = imageUri?.let { uri ->
                storageRepository.uploadProfileImage(user.id, uri).fold(
                    onSuccess = { it },
                    onFailure = { e ->
                        _uiState.update {
                            it.copy(isLoading = false, error = e.message ?: "Couldn't upload your photo")
                        }
                        return@launch
                    }
                )
            } ?: user.profileImageUrl

            val updated = user.copy(
                displayName = displayName,
                username = username,
                bio = bio,
                profileImageUrl = imageUrl
            )
            userRepository.updateUser(updated).fold(
                onSuccess = { _uiState.update { it.copy(user = updated, isLoading = false) } },
                onFailure = { e -> _uiState.update { it.copy(error = e.message, isLoading = false) } }
            )
        }
    }

}
