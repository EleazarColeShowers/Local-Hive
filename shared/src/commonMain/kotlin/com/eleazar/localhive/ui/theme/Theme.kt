package com.eleazar.localhive.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import localhive.shared.generated.resources.Res
import localhive.shared.generated.resources.nunito_bold
import localhive.shared.generated.resources.nunito_extrabold
import localhive.shared.generated.resources.nunito_medium
import localhive.shared.generated.resources.nunito_regular
import localhive.shared.generated.resources.nunito_semibold
import org.jetbrains.compose.resources.Font

private val DarkColorScheme = darkColorScheme(
    primary = Color(0xFF4CAF50),
    secondary = Color(0xFFF2B531),
    tertiary = Color(0xFFE08A6A),
    background = Color(0xFF121212),
    surface = Color(0xFF1E1E1E),
    onBackground = Color(0xFFE6E6E6),
    onSurface = Color(0xFFE6E6E6),
    surfaceVariant = Color(0xFF2A261D),
    onSurfaceVariant = Color(0xFFE6E6E6),
    // Menus, dialogs and sheets draw on these; Material's defaults are tinted purple.
    surfaceContainerLowest = Color(0xFF161616),
    surfaceContainerLow = Color(0xFF1E1E1E),
    surfaceContainer = Color(0xFF232323),
    surfaceContainerHigh = Color(0xFF282828),
    surfaceContainerHighest = Color(0xFF2E2E2E)
)

private val LightColorScheme = lightColorScheme(
    primary = HiveGreen,
    secondary = DeepHoney,
    tertiary = Clay,
    background = LightGold,
    surface = Color(0xFFFFFFFF),
    onBackground = DarkGray,
    onSurface = DarkGray,
    surfaceVariant = LightGold,
    onSurfaceVariant = DarkGray,
    surfaceContainerLowest = Color(0xFFFFFFFF),
    surfaceContainerLow = Color(0xFFFFFFFF),
    surfaceContainer = Color(0xFFFFFFFF),
    surfaceContainerHigh = Color(0xFFFFFFFF),
    surfaceContainerHighest = Color(0xFFFFFFFF)
)

/** Nunito everywhere: its rounded letterforms carry most of the app's friendliness. */
@Composable
private fun nunito(): FontFamily = FontFamily(
    Font(Res.font.nunito_regular, FontWeight.Normal),
    Font(Res.font.nunito_medium, FontWeight.Medium),
    Font(Res.font.nunito_semibold, FontWeight.SemiBold),
    Font(Res.font.nunito_bold, FontWeight.Bold),
    Font(Res.font.nunito_extrabold, FontWeight.ExtraBold),
)

private fun Typography.withFamily(family: FontFamily): Typography {
    fun TextStyle.f() = copy(fontFamily = family)
    return copy(
        displayLarge = displayLarge.f(), displayMedium = displayMedium.f(), displaySmall = displaySmall.f(),
        headlineLarge = headlineLarge.f(), headlineMedium = headlineMedium.f(), headlineSmall = headlineSmall.f(),
        titleLarge = titleLarge.f(), titleMedium = titleMedium.f(), titleSmall = titleSmall.f(),
        bodyLarge = bodyLarge.f(), bodyMedium = bodyMedium.f(), bodySmall = bodySmall.f(),
        labelLarge = labelLarge.f(), labelMedium = labelMedium.f(), labelSmall = labelSmall.f(),
    )
}

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
    val family = nunito()
    val typography = remember(family) { Typography().withFamily(family) }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = typography,
        content = content
    )
}
