package com.kmab.deft.core.designsystem

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color

@Immutable
data class DeftColors(
    val bg: Color,
    val surface: Color,
    val surface2: Color,
    val line: Color,
    val line2: Color,
    val text: Color,
    val muted: Color,
    val dim: Color,
    val accent: Color,
    val accentText: Color,
    val onAccent: Color,
    val finished: Color,
    val finishedText: Color,
    val paused: Color,
    val pausedText: Color,
    val abandoned: Color,
    val error: Color,
    val scrim: Color,
) {
    val reading: Color get() = accent

    companion object {
        val Ink =
            DeftColors(
                Color(0xFF0E0E11),
                Color(0xFF17171B),
                Color(0xFF1E1E24),
                Color(0xFF2A2A31),
                Color(0xFF3A3A44),
                Color(0xFFEDEBE4),
                Color(0xFF9A9AA6),
                Color(0xFF66666F),
                Color(0xFFC373FF),
                Color(0xFFC373FF),
                Color(0xFF0E0E11),
                Color(0xFF3DE08A),
                Color(0xFF3DE08A),
                Color(0xFFF5C451),
                Color(0xFFF5C451),
                Color(0xFF9A9AA6),
                Color(0xFFFF6B6B),
                Color(0xFF0E0E11).copy(alpha = 0.85f),
            )
        val Paper =
            DeftColors(
                Color(0xFFF4F1EA),
                Color(0xFFFBF9F4),
                Color(0xFFEDE9DF),
                Color(0xFFDDD8CC),
                Color(0xFFC9C3B5),
                Color(0xFF17171B),
                Color(0xFF5E5E68),
                Color(0xFF8A8A93),
                Color(0xFFB24BFF),
                Color(0xFF933AD3),
                Color(0xFF0E0E11),
                Color(0xFF29965C),
                Color(0xFF207549),
                Color(0xFF9F8033),
                Color(0xFF7E652A),
                Color(0xFF5E5E68),
                Color(0xFFBA3C36),
                Color(0xFF17171B).copy(alpha = 0.6f),
            )
    }
}

@Immutable
data class DeftReaderTheme(
    val bg: Color,
    val text: Color,
    val accent: Color,
)

// Reader theme colors are literal design tokens.
@Suppress("MagicNumber")
object DeftReaderThemes {
    val Light = DeftReaderTheme(Color(0xFFFBF9F4), Color(0xFF1B1B1F), Color(0xFF7D34B2))
    val Sepia = DeftReaderTheme(Color(0xFFF1E7D0), Color(0xFF4A3B28), Color(0xFF7D34B2))
    val Dark = DeftReaderTheme(Color(0xFF17171B), Color(0xFFD8D5CC), Color(0xFFC373FF))
    val Black = DeftReaderTheme(Color.Black, Color(0xFFB8B6AE), Color(0xFFC373FF))
    val all = listOf(Light, Sepia, Dark, Black)
}
