package com.eleazar.localhive.data.repository

import android.content.Context
import androidx.core.content.edit
import com.eleazar.localhive.domain.AppSettingsRepository
import com.eleazar.localhive.domain.ThemeMode
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AppSettingsRepositoryImpl @Inject constructor(
    @ApplicationContext context: Context
) : AppSettingsRepository {

    private val prefs = context.getSharedPreferences("app_settings", Context.MODE_PRIVATE)

    private val _themeMode = MutableStateFlow(
        prefs.getString(KEY_THEME_MODE, null)?.let { saved ->
            runCatching { ThemeMode.valueOf(saved) }.getOrNull()
        } ?: ThemeMode.SYSTEM
    )
    override val themeMode: StateFlow<ThemeMode> = _themeMode.asStateFlow()

    private val _pushNotificationsEnabled = MutableStateFlow(prefs.getBoolean(KEY_PUSH_ENABLED, true))
    override val pushNotificationsEnabled: StateFlow<Boolean> = _pushNotificationsEnabled.asStateFlow()

    override suspend fun setThemeMode(mode: ThemeMode) {
        prefs.edit { putString(KEY_THEME_MODE, mode.name) }
        _themeMode.value = mode
    }

    override suspend fun setPushNotificationsEnabled(enabled: Boolean) {
        prefs.edit { putBoolean(KEY_PUSH_ENABLED, enabled) }
        _pushNotificationsEnabled.value = enabled
    }

    private companion object {
        const val KEY_THEME_MODE = "theme_mode"
        const val KEY_PUSH_ENABLED = "push_notifications_enabled"
    }
}
