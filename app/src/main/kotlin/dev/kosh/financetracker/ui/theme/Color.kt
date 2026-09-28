package dev.kosh.financetracker.ui.theme

import androidx.compose.ui.graphics.Color

/** Semantic color tokens. Compose against these names, never a raw hex, outside this file. */
object KoshColors {
    val Background = Color(0xFF0B0B0F)
    val Surface = Color(0xFF16161D)
    val SurfaceVariant = Color(0xFF1E1E27)
    val Border = Color(0xFF2A2A35)

    val Foreground = Color(0xFFF5F5F7)
    val MutedForeground = Color(0xFF9CA3AF)

    val GradientStart = Color(0xFF7C3AED)
    val GradientEnd = Color(0xFFDB2777)
    val Primary = Color(0xFF8B5CF6)

    val Success = Color(0xFF34D399)
    val Destructive = Color(0xFFF87171)
    val Warning = Color(0xFFFBBF24)
    val Info = Color(0xFF60A5FA)

    val AvatarPalette = listOf(
        Color(0xFF8B5CF6), Color(0xFFEC4899), Color(0xFF3B82F6),
        Color(0xFF14B8A6), Color(0xFFF59E0B), Color(0xFFEF4444),
        Color(0xFF10B981), Color(0xFF6366F1),
    )
}
