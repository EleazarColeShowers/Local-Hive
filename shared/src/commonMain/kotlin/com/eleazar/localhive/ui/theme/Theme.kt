package com.eleazar.localhive.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = Color(0xFF4CAF50),
    secondary = DeepHoney,
    background = Color(0xFF121212),
    surface = Color(0xFF1E1E1E),
    onBackground = Color(0xFFE6E6E6),
    onSurface = Color(0xFFE6E6E6),
    surfaceVariant = Color(0xFF2A261D),
    onSurfaceVariant = Color(0xFFE6E6E6)
)

private val LightColorScheme = lightColorScheme(
    primary = HiveGreen,
    secondary = DeepHoney,
    background = LightGold,
    surface = Color(0xFFFFFFFF),
    onBackground = DarkGray,
    onSurface = DarkGray,
    surfaceVariant = LightGold,
    onSurfaceVariant = DarkGray
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
