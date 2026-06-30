package com.eleazar.localhive.ui.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.eleazar.localhive.domain.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SplashViewModel @Inject constructor(
    private val authRepository: AuthRepository
) : ViewModel() {

    sealed class AuthState {
        object Loading : AuthState()
        object NotLoggedIn : AuthState()
        object LoggedInNoEstate : AuthState()
        object LoggedInWithEstate : AuthState()
    }

    private val _authState = MutableStateFlow<AuthState>(AuthState.Loading)
    val authState = _authState.asStateFlow()

    init {
        checkAuthState()
    }

    private fun checkAuthState() {
        viewModelScope.launch {
            val userId = authRepository.getCurrentUserId()
            if (userId == null) {
                _authState.value = AuthState.NotLoggedIn
                return@launch
            }
            authRepository.getUserProfile(userId).fold(
                onSuccess = { user ->
                    _authState.value = if (user.estateId != null)
                        AuthState.LoggedInWithEstate
                    else
                        AuthState.LoggedInNoEstate
                },
                onFailure = { _authState.value = AuthState.NotLoggedIn }
            )
        }
    }
}
