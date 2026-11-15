package com.kmab.deft.core.designsystem

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

// Type scale values are the approved design tokens.
@Suppress("MagicNumber")
object DeftTypography {
    val ui =
        FontFamily(
            Font(R.font.space_grotesk_regular, FontWeight.Normal),
            Font(R.font.space_grotesk_medium, FontWeight.Medium),
            Font(R.font.space_grotesk_semibold, FontWeight.SemiBold),
        )
    val telemetry = FontFamily(Font(R.font.space_mono_regular), Font(R.font.space_mono_bold, FontWeight.Bold))

    private fun style(
        size: Int,
        height: Int,
        weight: FontWeight = FontWeight.Normal,
        mono: Boolean = false,
    ) = TextStyle(
        fontFamily = if (mono) telemetry else ui,
        fontSize = size.sp,
        lineHeight = height.sp,
        fontWeight = weight,
    )

    val celebrate = style(52, 52, FontWeight.SemiBold).copy(letterSpacing = (-1.56).sp)
    val display = style(32, 38, FontWeight.SemiBold)
    val headline = style(22, 28, FontWeight.SemiBold)
    val title = style(17, 24, FontWeight.SemiBold)
    val body = style(14, 21)
    val label = style(13, 18, FontWeight.Medium)
    val cardTitle = style(12, 16, FontWeight.Medium)
    val mono = style(11, 15, mono = true)
    val monoSmall = style(10, 13, mono = true)
    val statLarge = style(24, 28, FontWeight.Bold, mono = true)
    val statMedium = style(17, 22, FontWeight.Bold, mono = true)
    val material =
        Typography(
            displayLarge = celebrate,
            displayMedium = display,
            displaySmall = display,
            headlineLarge = headline,
            headlineMedium = headline,
            headlineSmall = headline,
            titleLarge = title,
            titleMedium = title,
            titleSmall = cardTitle,
            bodyLarge = body,
            bodyMedium = body,
            bodySmall = mono,
            labelLarge = label,
            labelMedium = label,
            labelSmall = monoSmall,
        )
}
