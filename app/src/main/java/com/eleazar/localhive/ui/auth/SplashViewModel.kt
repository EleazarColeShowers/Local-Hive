package com.eleazar.localhive.ui.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.eleazar.localhive.domain.AuthRepository
import com.eleazar.localhive.domain.EstateRepository
import com.eleazar.localhive.domain.UserRepository
import com.google.firebase.messaging.FirebaseMessaging
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

@HiltViewModel
class SplashViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val estateRepository: EstateRepository,
    private val userRepository: UserRepository
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
            // Neither of these decides where to go, so they run alongside rather than in front.
            launch { syncFcmToken(userId) }

            // Cache-first: a returning user's profile is already on the device, so routing
            // doesn't wait on the network.
            userRepository.getUser(userId, cacheFirst = true).fold(
                onSuccess = { user ->
                    val estateId = user.estateId
                    if (estateId == null) {
                        _authState.value = AuthState.LoggedInNoEstate
                    } else {
                        _authState.value = AuthState.LoggedInWithEstate
                        launch { dropEstateIfDeleted(estateId, userId) }
                    }
                },
                onFailure = { _authState.value = AuthState.NotLoggedIn }
            )
        }
    }

    /** An estate that was deleted while the user was away; they'll be routed to pick a new one next launch. */
    private suspend fun dropEstateIfDeleted(estateId: String, userId: String) {
        if (estateRepository.estateExists(estateId) == false) {
            estateRepository.leaveEstate(estateId, userId)
        }
    }

    private suspend fun syncFcmToken(userId: String) {
        try {
            val token = FirebaseMessaging.getInstance().token.await()
            userRepository.updateFcmToken(userId, token)
        } catch (_: Exception) {
            // Best-effort: a stale/missing token just means push targeting lags until the next refresh.
        }
    }
}
