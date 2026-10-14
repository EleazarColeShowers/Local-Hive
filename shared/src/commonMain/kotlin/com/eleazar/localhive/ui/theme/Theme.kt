package com.eleazar.localhive.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// Neutral whites and greys carry the layout, the way most big apps do; green and honey are
// kept for the things that should stand out (buttons, links, badges, announcements).
private val DarkColorScheme = darkColorScheme(
    primary = Color(0xFF5CBF6A),
    onPrimary = Color(0xFF00390F),
    primaryContainer = Color(0xFF1F4D2B),
    onPrimaryContainer = Color(0xFFBDEBC4),
    secondary = Color(0xFFF2B531),
    onSecondary = Color(0xFF3D2A00),
    secondaryContainer = Color(0xFF4A3A10),
    onSecondaryContainer = Color(0xFFFFE7A8),
    tertiary = Color(0xFFE08A6A),
    background = Color(0xFF101211),
    onBackground = Color(0xFFE7E9E7),
    surface = Color(0xFF101211),
    onSurface = Color(0xFFE7E9E7),
    surfaceVariant = Color(0xFF1E211F),
    onSurfaceVariant = Color(0xFFA9AEAA),
    outline = Color(0xFF444845),
    outlineVariant = Color(0xFF2A2D2B),
    // Menus, dialogs and sheets draw on these; Material's defaults are tinted purple.
    surfaceContainerLowest = Color(0xFF0B0D0C),
    surfaceContainerLow = Color(0xFF161917),
    surfaceContainer = Color(0xFF1A1D1B),
    surfaceContainerHigh = Color(0xFF222523),
    surfaceContainerHighest = Color(0xFF2C2F2D)
)

private val LightColorScheme = lightColorScheme(
    primary = HiveGreen,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFD6EED9),
    onPrimaryContainer = Color(0xFF0B3D1A),
    secondary = DeepHoney,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFFFE7A8),
    onSecondaryContainer = Color(0xFF3D2A00),
    tertiary = Clay,
    background = Color.White,
    onBackground = Color(0xFF1A1C1B),
    surface = Color.White,
    onSurface = Color(0xFF1A1C1B),
    surfaceVariant = Color(0xFFF1F2F0),
    onSurfaceVariant = Color(0xFF5E6360),
    outline = Color(0xFFC9CCC9),
    outlineVariant = Color(0xFFE8E9E7),
    surfaceContainerLowest = Color.White,
    surfaceContainerLow = Color(0xFFF7F8F6),
    surfaceContainer = Color(0xFFF2F3F1),
    surfaceContainerHigh = Color(0xFFEDEEEC),
    surfaceContainerHighest = Color(0xFFE7E8E6)
)

@Composable
fun LocalHiveTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    // Material's default type, which is the platform's own font: Roboto on Android, San Francisco on iOS.
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme,
        typography = Typography(),
        content = content
    )
}
