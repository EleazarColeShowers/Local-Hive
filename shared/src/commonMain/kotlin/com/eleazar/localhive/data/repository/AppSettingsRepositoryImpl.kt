package com.eleazar.localhive.data.repository

import com.eleazar.localhive.data.platform.KeyValueStore
import com.eleazar.localhive.domain.AppSettingsRepository
import com.eleazar.localhive.domain.ThemeMode
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class AppSettingsRepositoryImpl(
    private val store: KeyValueStore
) : AppSettingsRepository {

    private val _themeMode = MutableStateFlow(
        store.getString(KEY_THEME_MODE)?.let { saved ->
            runCatching { ThemeMode.valueOf(saved) }.getOrNull()
        } ?: ThemeMode.SYSTEM
    )
    override val themeMode: StateFlow<ThemeMode> = _themeMode.asStateFlow()

    private val _pushNotificationsEnabled = MutableStateFlow(store.getBoolean(KEY_PUSH_ENABLED, true))
    override val pushNotificationsEnabled: StateFlow<Boolean> = _pushNotificationsEnabled.asStateFlow()

    override suspend fun setThemeMode(mode: ThemeMode) {
        store.putString(KEY_THEME_MODE, mode.name)
        _themeMode.value = mode
    }

    override suspend fun setPushNotificationsEnabled(enabled: Boolean) {
        store.putBoolean(KEY_PUSH_ENABLED, enabled)
        _pushNotificationsEnabled.value = enabled
    }

    private companion object {
        const val KEY_THEME_MODE = "theme_mode"
        const val KEY_PUSH_ENABLED = "push_notifications_enabled"
    }
}
