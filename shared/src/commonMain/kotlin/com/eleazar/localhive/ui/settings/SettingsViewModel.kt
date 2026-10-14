package com.eleazar.localhive.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.eleazar.localhive.data.remote.PushTokenSync
import com.eleazar.localhive.domain.AppSettingsRepository
import com.eleazar.localhive.domain.AuthRepository
import com.eleazar.localhive.domain.EstateRepository
import com.eleazar.localhive.domain.ThemeMode
import com.eleazar.localhive.domain.UserRepository
import com.eleazar.localhive.domain.model.Estate
import com.eleazar.localhive.domain.model.User
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class SettingsUiState(
    val user: User? = null,
    val estate: Estate? = null,
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val pushNotificationsEnabled: Boolean = true,
    val lightAlerts: Boolean = false,
    val isLoading: Boolean = false,
    val error: String? = null,
    val isLoggedOut: Boolean = false,
    val hasLeftEstate: Boolean = false,
    val isDeletingAccount: Boolean = false,
    val deleteAccountError: String? = null
)

class SettingsViewModel(
    private val authRepository: AuthRepository,
    private val userRepository: UserRepository,
    private val estateRepository: EstateRepository,
    private val appSettingsRepository: AppSettingsRepository,
    private val pushTokenSync: PushTokenSync
) : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            appSettingsRepository.themeMode.collect { mode -> _uiState.update { it.copy(themeMode = mode) } }
        }
        viewModelScope.launch {
            appSettingsRepository.pushNotificationsEnabled.collect { enabled ->
                _uiState.update { it.copy(pushNotificationsEnabled = enabled) }
            }
        }
        loadProfile()
    }

    private fun loadProfile() {
        val userId = authRepository.getCurrentUserId() ?: return
        viewModelScope.launch {
            userRepository.getUser(userId).fold(
                onSuccess = { user ->
                    _uiState.update { it.copy(user = user, lightAlerts = user.lightAlerts) }
                    user.estateId?.let { loadEstate(it) }
                },
                onFailure = { /* non-critical, ignore */ }
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

    fun setThemeMode(mode: ThemeMode) {
        viewModelScope.launch { appSettingsRepository.setThemeMode(mode) }
    }

    fun setPushNotificationsEnabled(enabled: Boolean) {
        viewModelScope.launch {
            appSettingsRepository.setPushNotificationsEnabled(enabled)
            // Puts the device's token on the profile or takes it off, so the Worker stops sending too.
            authRepository.getCurrentUserId()?.let { pushTokenSync.sync(it) }
        }
    }

    fun setLightAlerts(enabled: Boolean) {
        val userId = authRepository.getCurrentUserId() ?: return
        _uiState.update { it.copy(lightAlerts = enabled) }
        viewModelScope.launch {
            userRepository.setLightAlerts(userId, enabled).onFailure {
                _uiState.update { it.copy(lightAlerts = !enabled) }
            }
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

    /** On success the user is signed out, so the screen heads back to the start like a logout. */
    fun deleteAccount(password: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isDeletingAccount = true, deleteAccountError = null) }
            authRepository.deleteAccount(password).fold(
                onSuccess = { _uiState.update { it.copy(isDeletingAccount = false, isLoggedOut = true) } },
                onFailure = { e ->
                    _uiState.update {
                        it.copy(isDeletingAccount = false, deleteAccountError = e.message ?: "Couldn't delete your account. Try again.")
                    }
                }
            )
        }
    }

    fun clearDeleteAccountError() = _uiState.update { it.copy(deleteAccountError = null) }

    fun logout() {
        viewModelScope.launch {
            authRepository.logout()
            _uiState.update { it.copy(isLoggedOut = true) }
        }
    }
}
