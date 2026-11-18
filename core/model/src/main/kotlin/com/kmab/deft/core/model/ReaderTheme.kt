package com.kmab.deft.core.model

import kotlin.math.pow

data class ReaderTheme(
    val id: String,
    val name: String,
    val background: String,
    val text: String,
    val accent: String,
)

object ReaderThemes {
    val builtIn =
        listOf(
            ReaderTheme("light", "light", "#FBF9F4", "#1B1B1F", "#7D34B2"),
            ReaderTheme("sepia", "sepia", "#F1E7D0", "#4A3B28", "#7D34B2"),
            ReaderTheme("dark", "dark", "#17171B", "#D8D5CC", "#C373FF"),
            ReaderTheme("black", "black", "#000000", "#B8B6AE", "#C373FF"),
        )

    fun find(
        id: String,
        customThemes: List<ReaderTheme> = emptyList(),
    ): ReaderTheme? = builtIn.firstOrNull { it.id == id } ?: customThemes.firstOrNull { it.id == id }

    /** Returns corrective copy for editors; built-in IDs cannot be overwritten. */
    fun validationError(theme: ReaderTheme): String? =
        when {
            !ID.matches(theme.id) || builtIn.any { it.id == theme.id } -> "Choose a new custom theme."
            theme.name.isBlank() || theme.name.length > MAX_NAME -> "Use a theme name between 1 and 40 characters."
            listOf(theme.background, theme.text, theme.accent).any { !COLOR.matches(it) } ->
                "Use six-digit colors, such as #17171B."
            contrast(
                theme.background,
                theme.text,
            ) < MIN_TEXT_CONTRAST -> "Text needs stronger contrast with the background."
            contrast(
                theme.background,
                theme.accent,
            ) < MIN_ACCENT_CONTRAST -> "Links need stronger contrast with the background."
            else -> null
        }

    @Suppress("MagicNumber") // WCAG relative luminance constants and RGB bit positions.
    fun contrast(
        first: String,
        second: String,
    ): Double {
        fun luminance(hex: String): Double {
            val rgb = hex.removePrefix("#").toIntOrNull(16) ?: return 0.0

            fun channel(shift: Int): Double {
                val value = ((rgb shr shift) and 255) / 255.0
                return if (value <= 0.04045) value / 12.92 else ((value + 0.055) / 1.055).pow(2.4)
            }
            return 0.2126 * channel(16) + 0.7152 * channel(8) + 0.0722 * channel(0)
        }
        val a = luminance(first)
        val b = luminance(second)
        return (maxOf(a, b) + 0.05) / (minOf(a, b) + 0.05)
    }

    private val COLOR = Regex("#[0-9a-fA-F]{6}")
    private val ID = Regex("[A-Za-z0-9_-]{1,80}")
    private const val MAX_NAME = 40
    private const val MIN_TEXT_CONTRAST = 4.5
    private const val MIN_ACCENT_CONTRAST = 4.5
}
