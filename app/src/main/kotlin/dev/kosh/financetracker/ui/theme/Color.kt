package dev.kosh.financetracker.ui.theme

import androidx.compose.ui.graphics.Color

/** Kosh brand palette — see Kosh_Brand_Kit/theme/colors.json for the source of truth. */
object KoshColors {
    val Charcoal = Color(0xFF171C20)
    val Gold = Color(0xFFD5A64F)
    val Cream = Color(0xFFF7F5F0)

    val LightBackground = Color(0xFFF7F5F0)
    val LightSurface = Color(0xFFFFFFFF)
    val LightText = Color(0xFF1D2528)
    val LightMuted = Color(0xFF5E686B)
    val LightBorder = Color(0xFFDCE1DF)
    val LightAccentText = Color(0xFF966A1C)
    val LightIncome = Color(0xFF276B5A)
    val LightExpense = Color(0xFFA14343)

    val DarkBackground = Color(0xFF111619)
    val DarkSurface = Color(0xFF1D2528)
    val DarkText = Color(0xFFF6F5F1)
    val DarkMuted = Color(0xFFAEB8B7)
    val DarkBorder = Color(0xFF394247)
    val DarkIncome = Color(0xFF7FD1AF)
    val DarkExpense = Color(0xFFF0A29A)

    /** Qualitative palette for the category breakdown bar/legend only — data series
     * need to be visually distinguishable, unlike the rest of the UI which reserves
     * color for semantic meaning (gold = emphasis, green/red = income/expense). */
    val ChartPalette = listOf(
        Gold,
        Color(0xFF5B8A87),
        Color(0xFFC97B84),
        Color(0xFF6E8FB0),
        Color(0xFF9A8C6B),
    )
}
