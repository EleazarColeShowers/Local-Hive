package com.eleazar.localhive.ui.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.eleazar.localhive.domain.AuthRepository
import com.eleazar.localhive.domain.StorageRepository
import com.eleazar.localhive.domain.model.User
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ProfileSetUpUiState(
    val isLoading: Boolean = false,
    val isDone: Boolean = false,
    val error: String? = null
)

class ProfileSetUpViewModel(
    private val authRepository: AuthRepository,
    private val storageRepository: StorageRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ProfileSetUpUiState())
    val uiState = _uiState.asStateFlow()

    fun saveProfile(details: ProfileDetails, imageUri: String? = null) {
        val userId = authRepository.getCurrentUserId() ?: return
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }

            val imageUrl = imageUri?.let { uri ->
                storageRepository.uploadProfileImage(userId, uri).fold(
                    onSuccess = { it },
                    onFailure = { e ->
                        _uiState.update {
                            it.copy(isLoading = false, error = e.message ?: "Couldn't upload your photo")
                        }
                        return@launch
                    }
                )
            }

            val user = User(
                id = userId,
                displayName = details.displayName.trim(),
                bio = details.bio.trim(),
                profileImageUrl = imageUrl,
                address = details.address.trim().ifBlank { null },
                occupation = details.occupation.trim().ifBlank { null },
                phone = details.phone.trim().ifBlank { null }
            )
            authRepository.saveUserProfile(user).fold(
                onSuccess = { _uiState.update { it.copy(isLoading = false, isDone = true) } },
                onFailure = { e -> _uiState.update { it.copy(isLoading = false, error = e.message) } }
            )
        }
    }
}
