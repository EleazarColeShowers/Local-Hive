package com.eleazar.localhive.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.eleazar.localhive.domain.AppSettingsRepository
import com.eleazar.localhive.domain.ThemeMode
import com.eleazar.localhive.ui.navigation.AppLocalHiveScreens
import com.eleazar.localhive.ui.navigation.LocalHiveNavGraph
import com.eleazar.localhive.ui.theme.LocalHiveTheme
import org.koin.compose.koinInject

/**
 * The whole app, on both platforms, in the theme the user picked in Settings.
 *
 * @param onDarkThemeChange lets the platform match its own chrome (status bar icons) to the theme
 * @param overlay drawn over the app inside its theme, for platform prompts such as an update dialog
 */
@Composable
fun LocalHiveApp(
    hasPendingShare: Boolean = false,
    pendingLink: String? = null,
    onLinkHandled: () -> Unit = {},
    onDarkThemeChange: (darkTheme: Boolean) -> Unit = {},
    overlay: @Composable () -> Unit = {}
) {
    val appSettings: AppSettingsRepository = koinInject()
    val themeMode by appSettings.themeMode.collectAsStateWithLifecycle()
    val darkTheme = when (themeMode) {
        ThemeMode.DARK -> true
        ThemeMode.LIGHT -> false
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
    }
    SideEffect { onDarkThemeChange(darkTheme) }

    LocalHiveTheme(darkTheme = darkTheme) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {
            LocalHiveNavGraph(
                screens = AppLocalHiveScreens,
                hasPendingShare = hasPendingShare,
                pendingLink = pendingLink,
                onLinkHandled = onLinkHandled
            )
        }
        overlay()
    }
}
