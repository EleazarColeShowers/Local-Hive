package com.eleazar.localhive.ui.settings

import androidx.compose.runtime.*
import org.koin.compose.viewmodel.koinViewModel
import org.koin.compose.koinInject
import com.eleazar.localhive.data.remote.LocalHiveApi
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.eleazar.localhive.domain.ThemeMode
import com.eleazar.localhive.ui.components.LeaveRole
import com.eleazar.localhive.ui.platform.rememberPlatformActions
import com.eleazar.localhive.ui.profile.ProfileColors
import com.eleazar.localhive.domain.model.Report
import com.eleazar.localhive.domain.model.ReportSubject
import com.eleazar.localhive.ui.report.rememberReporter

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
    viewModel: SettingsViewModel = koinViewModel(),
    api: LocalHiveApi = koinInject()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val platform = rememberPlatformActions()
    val report = rememberReporter()

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
        sosSound = uiState.sosSound,
        onSosSoundChange = viewModel::setSosSound,
        sosPausedUntil = uiState.sosPausedUntil,
        onPauseSos = viewModel::pauseSos,
        onLeaveEstate = { viewModel.leaveEstate() },
        leaveRole = uiState.estate?.let { LeaveRole.of(it, uiState.user?.id) } ?: LeaveRole.MEMBER,
        onLogout = { viewModel.logout() },
        onBlockedUsers = onBlockedUsers,
        isDeletingAccount = uiState.isDeletingAccount,
        deleteAccountError = uiState.deleteAccountError,
        onDeleteAccount = viewModel::deleteAccount,
        onDeleteAccountDismissed = viewModel::clearDeleteAccountError,
        policyLinks = listOf(
            "Terms of Use" to "terms",
            "Privacy Policy" to "privacy",
            "Child Safety Standards" to "child-safety"
        ).mapNotNull { (title, page) -> api.pageLink(page)?.let { title to it } },
        onSendFeedback = { reason, details ->
            report(Report(subject = ReportSubject.APP, reason = reason, details = details))
        }
    )
}