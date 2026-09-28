package dev.kosh.financetracker.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

/**
 * Semantic colors Material3's ColorScheme has no slot for (income/expense) plus the
 * accent-on-cream text color, which differs from the accent's own fill color.
 */
data class KoshExtendedColors(
    val income: Color,
    val expense: Color,
    val accentText: Color,
)

private val LightExtendedColors = KoshExtendedColors(
    income = KoshColors.LightIncome,
    expense = KoshColors.LightExpense,
    accentText = KoshColors.LightAccentText,
)

private val DarkExtendedColors = KoshExtendedColors(
    income = KoshColors.DarkIncome,
    expense = KoshColors.DarkExpense,
    accentText = KoshColors.Gold,
)

private val LocalKoshExtendedColors = staticCompositionLocalOf { LightExtendedColors }

object KoshExtendedTheme {
    val colors: KoshExtendedColors
        @Composable get() = LocalKoshExtendedColors.current
}

private val KoshLightColorScheme = lightColorScheme(
    background = KoshColors.LightBackground,
    surface = KoshColors.LightSurface,
    surfaceVariant = KoshColors.LightSurface,
    onBackground = KoshColors.LightText,
    onSurface = KoshColors.LightText,
    onSurfaceVariant = KoshColors.LightMuted,
    primary = KoshColors.LightAccentText,
    onPrimary = Color.White,
    outline = KoshColors.LightBorder,
)

private val KoshDarkColorScheme = darkColorScheme(
    background = KoshColors.DarkBackground,
    surface = KoshColors.DarkSurface,
    surfaceVariant = KoshColors.DarkSurface,
    onBackground = KoshColors.DarkText,
    onSurface = KoshColors.DarkText,
    onSurfaceVariant = KoshColors.DarkMuted,
    primary = KoshColors.Gold,
    onPrimary = KoshColors.Charcoal,
    outline = KoshColors.DarkBorder,
)

@Composable
fun KoshTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val colorScheme = if (darkTheme) KoshDarkColorScheme else KoshLightColorScheme
    val extendedColors = if (darkTheme) DarkExtendedColors else LightExtendedColors

    CompositionLocalProvider(LocalKoshExtendedColors provides extendedColors) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = KoshTypography,
            content = content,
        )
    }
}
