package com.eleazar.localhive.ui.settings

import androidx.compose.runtime.*
import org.koin.compose.viewmodel.koinViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.eleazar.localhive.domain.ThemeMode
import com.eleazar.localhive.ui.components.LeaveRole
import com.eleazar.localhive.ui.platform.rememberPlatformActions
import com.eleazar.localhive.ui.profile.ProfileColors

/**
 * Entry point: owns the ViewModel, navigation side effects and the app version, and feeds the
 * platform-free [SettingsScreen].
 */
@Composable
fun SettingsRoute(
    onBack: () -> Unit,
    onLoggedOut: () -> Unit,
    onLeftEstate: () -> Unit,
    onBlockedUsers: () -> Unit,
    viewModel: SettingsViewModel = koinViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val platform = rememberPlatformActions()

    val colors = com.eleazar.localhive.ui.profile.rememberProfileColors()

    LaunchedEffect(uiState.isLoggedOut) { if (uiState.isLoggedOut) onLoggedOut() }
    LaunchedEffect(uiState.hasLeftEstate) { if (uiState.hasLeftEstate) onLeftEstate() }

    val versionName = remember(platform) { platform.appVersion }

    SettingsScreen(
        userEmail = uiState.user?.email,
        estateName = uiState.estate?.name,
        themeMode = when (uiState.themeMode) {
            ThemeMode.SYSTEM -> SettingsThemeMode.SYSTEM
            ThemeMode.LIGHT -> SettingsThemeMode.LIGHT
            ThemeMode.DARK -> SettingsThemeMode.DARK
        },
        pushNotificationsEnabled = uiState.pushNotificationsEnabled,
        versionName = versionName,
        error = uiState.error,
        colors = colors,
        onBack = onBack,
        onThemeModeChange = { mode ->
            viewModel.setThemeMode(
                when (mode) {
                    SettingsThemeMode.SYSTEM -> ThemeMode.SYSTEM
                    SettingsThemeMode.LIGHT -> ThemeMode.LIGHT
                    SettingsThemeMode.DARK -> ThemeMode.DARK
                }
            )
        },
        onPushNotificationsChange = { viewModel.setPushNotificationsEnabled(it) },
        lightAlerts = uiState.lightAlerts,
        onLightAlertsChange = viewModel::setLightAlerts,
        onLeaveEstate = { viewModel.leaveEstate() },
        leaveRole = uiState.estate?.let { LeaveRole.of(it, uiState.user?.id) } ?: LeaveRole.MEMBER,
        onLogout = { viewModel.logout() },
        onBlockedUsers = onBlockedUsers,
        isDeletingAccount = uiState.isDeletingAccount,
        deleteAccountError = uiState.deleteAccountError,
        onDeleteAccount = viewModel::deleteAccount,
        onDeleteAccountDismissed = viewModel::clearDeleteAccountError
    )
}