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
import androidx.compose.runtime.remember
import com.eleazar.localhive.data.platform.isIos
import com.eleazar.localhive.domain.AppConfig
import com.eleazar.localhive.domain.AppConfigRepository
import com.eleazar.localhive.domain.AppSettingsRepository
import com.eleazar.localhive.domain.isOutdated
import com.eleazar.localhive.ui.platform.rememberPlatformActions
import com.eleazar.localhive.ui.update.UpdateRequiredScreen
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

    val configRepository: AppConfigRepository = koinInject()
    val config by remember(configRepository) { configRepository.observeConfig() }.collectAsStateWithLifecycle(AppConfig())
    val installed = rememberPlatformActions().appVersion

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
        // Over everything, once the estate's data has moved on past what this version understands.
        if (isOutdated(installed, config.minVersion)) {
            val storeUrl = if (isIos) config.iosUrl.ifBlank { APP_STORE_URL } else config.androidUrl.ifBlank { PLAY_STORE_URL }
            UpdateRequiredScreen(installed = installed, storeUrl = storeUrl)
        }
    }
}

private const val PLAY_STORE_URL = "https://play.google.com/store/apps/details?id=com.eleazar.localhive"
private const val APP_STORE_URL = "https://apps.apple.com/app/id6832065326"
