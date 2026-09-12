package com.eleazar.localhive.domain

import kotlinx.coroutines.flow.StateFlow

enum class ThemeMode { SYSTEM, LIGHT, DARK }

interface AppSettingsRepository {
    val themeMode: StateFlow<ThemeMode>
    val pushNotificationsEnabled: StateFlow<Boolean>

    suspend fun setThemeMode(mode: ThemeMode)
    suspend fun setPushNotificationsEnabled(enabled: Boolean)
}
