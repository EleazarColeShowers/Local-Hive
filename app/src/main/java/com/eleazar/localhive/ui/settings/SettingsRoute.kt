package com.eleazar.localhive.ui.settings

import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.colorResource
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.eleazar.localhive.R
import com.eleazar.localhive.domain.ThemeMode
import com.eleazar.localhive.ui.profile.ProfileColors

/**
 * App-module entry point: owns the ViewModel, navigation side effects and
 * package info, and feeds the shared, platform-free [SettingsScreen].
 */
@Composable
fun SettingsRoute(
    onBack: () -> Unit,
    onLoggedOut: () -> Unit,
    onLeftEstate: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    val colors = ProfileColors(
        hiveGreen = colorResource(id = R.color.hivegreen),
        honeyGold = colorResource(id = R.color.honeygold),
        backgroundGold = colorResource(id = R.color.backgroundgold),
        lightGold = colorResource(id = R.color.lightgold),
        darkGray = colorResource(id = R.color.darkgray)
    )

    LaunchedEffect(uiState.isLoggedOut) { if (uiState.isLoggedOut) onLoggedOut() }
    LaunchedEffect(uiState.hasLeftEstate) { if (uiState.hasLeftEstate) onLeftEstate() }

    val versionName = remember {
        runCatching {
            context.packageManager.getPackageInfo(context.packageName, 0).versionName
        }.getOrNull() ?: "—"
    }

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
        onLeaveEstate = { viewModel.leaveEstate() },
        onLogout = { viewModel.logout() }
    )
}