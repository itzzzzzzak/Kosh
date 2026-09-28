package dev.kosh.financetracker.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val KoshDarkColorScheme = darkColorScheme(
    background = KoshColors.Background,
    surface = KoshColors.Surface,
    surfaceVariant = KoshColors.SurfaceVariant,
    onBackground = KoshColors.Foreground,
    onSurface = KoshColors.Foreground,
    onSurfaceVariant = KoshColors.MutedForeground,
    primary = KoshColors.Primary,
    onPrimary = Color.White,
    error = KoshColors.Destructive,
    outline = KoshColors.Border,
)

@Composable
fun KoshTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = KoshDarkColorScheme,
        typography = KoshTypography,
        content = content,
    )
}
