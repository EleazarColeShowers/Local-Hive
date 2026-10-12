package com.eleazar.localhive.ui.estate

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

data class EstateColors(
    val hiveGreen: Color = Color(0xFF4CAF50),
    val backgroundGold: Color = Color(0xFFFFF59D),
    val darkGray: Color = Color(0xFF2C2C2C),
    val cardBackground: Color = Color.White,
    val surfaceGradientEnd: Color = Color(0xFFFFF8E7)
)

@Composable
fun rememberEstateColors(): EstateColors {
    val colorScheme = MaterialTheme.colorScheme
    val isDark = colorScheme.onBackground.red > 0.5f
    return EstateColors(
        hiveGreen = colorScheme.primary,
        backgroundGold = colorScheme.background,
        darkGray = colorScheme.onBackground,
        cardBackground = colorScheme.surface,
        surfaceGradientEnd = if (isDark) Color(0xFF1A1813) else Color(0xFFFFF8E7)
    )
}
