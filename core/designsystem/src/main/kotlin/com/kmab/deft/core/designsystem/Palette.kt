package com.kmab.deft.core.designsystem

import androidx.compose.ui.graphics.Color

/**
 * kmab brand tint scales; original source and license in plans/dependency-sources.md. The original brand palette
 * is literal color data.
 */
@Suppress("MagicNumber")
object Palette {
    val purple =
        mapOf(
            50 to Color(0xFFF9F1FF),
            100 to Color(0xFFF1DFFF),
            200 to Color(0xFFE3BEFF),
            300 to Color(0xFFD49AFF),
            400 to Color(0xFFC373FF),
            500 to Color(0xFFB24BFF),
            600 to Color(0xFF9940DB),
            700 to Color(0xFF7D34B2),
            800 to Color(0xFF60288A),
            900 to Color(0xFF441C61),
        )
    val green =
        mapOf(
            50 to Color(0xFFEFFDF6),
            100 to Color(0xFFDCF9EA),
            200 to Color(0xFFB9F4D5),
            300 to Color(0xFF92EEBD),
            400 to Color(0xFF68E7A4),
            500 to Color(0xFF3DE08A),
            600 to Color(0xFF34C177),
            700 to Color(0xFF2B9D61),
            800 to Color(0xFF21794B),
            900 to Color(0xFF175534),
        )
    val amber =
        mapOf(
            50 to Color(0xFFFEFAF1),
            100 to Color(0xFFFDF4E0),
            200 to Color(0xFFFBEAC0),
            300 to Color(0xFFF9DE9E),
            400 to Color(0xFFF7D177),
            500 to Color(0xFFF5C451),
            600 to Color(0xFFD3A946),
            700 to Color(0xFFAC8939),
            800 to Color(0xFF846A2C),
            900 to Color(0xFF5D4A1F),
        )
    val coverTints =
        listOf(
            Color(0xFF31253D),
            Color(0xFF26372F),
            Color(0xFF3A3223),
            Color(0xFF2C3240),
            Color(0xFF3B2934),
            Color(0xFF273B3A),
            Color(0xFF39302A),
            Color(0xFF33313D),
        )
}
