package com.eleazar.localhive.ui.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.eleazar.localhive.domain.AuthRepository
import com.eleazar.localhive.domain.model.User
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ProfileSetUpUiState(
    val isLoading: Boolean = false,
    val isDone: Boolean = false,
    val error: String? = null
)

@HiltViewModel
class ProfileSetUpViewModel @Inject constructor(
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ProfileSetUpUiState())
    val uiState = _uiState.asStateFlow()

    fun saveProfile(displayName: String, bio: String) {
        val userId = authRepository.getCurrentUserId() ?: return
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            val user = User(id = userId, displayName = displayName, bio = bio)
            authRepository.saveUserProfile(user).fold(
                onSuccess = { _uiState.update { it.copy(isLoading = false, isDone = true) } },
                onFailure = { e -> _uiState.update { it.copy(isLoading = false, error = e.message) } }
            )
        }
    }
}
