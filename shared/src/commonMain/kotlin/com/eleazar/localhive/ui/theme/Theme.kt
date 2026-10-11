package com.eleazar.localhive.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val DarkColorScheme = darkColorScheme(
    primary = HiveGreen,
    secondary = DeepHoney,
    background = DarkGray,
    surface = DarkGray
)

private val LightColorScheme = lightColorScheme(
    primary = HiveGreen,
    secondary = DeepHoney,
    background = LightGold,
    surface = LightGray
)

@Composable
fun LocalHiveTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) {
        DarkColorScheme
    } else {
        LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        content = content
    )
}