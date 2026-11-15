package com.kmab.deft.core.designsystem

import androidx.compose.ui.graphics.Color
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.pow

class ContrastTest {
    private fun luminance(c: Color): Double {
        fun channel(v: Float) = if (v <= 0.04045) v / 12.92 else ((v + 0.055) / 1.055).pow(2.4)
        return 0.2126 * channel(c.red) + 0.7152 * channel(c.green) + 0.0722 * channel(c.blue)
    }

    private fun check(
        fg: Color,
        bg: Color,
        minimum: Double,
    ) {
        val values = listOf(luminance(fg), luminance(bg)).sorted()
        val ratio = (values[1] + 0.05) / (values[0] + 0.05)
        assertTrue("$fg on $bg: $ratio < $minimum", ratio >= minimum)
    }

    @Test fun enabledTextAndMeaningfulGraphicsMeetContrast() {
        listOf(DeftColors.Ink, DeftColors.Paper).forEach { c ->
            listOf(c.bg, c.surface, c.surface2).forEach { background ->
                listOf(c.text, c.muted, c.accentText, c.finishedText, c.pausedText, c.error).forEach {
                    check(it, background, 4.5)
                }
            }
            check(c.onAccent, c.accent, 4.5)
            listOf(c.bg, c.surface, c.surface2).forEach { background ->
                listOf(c.accent, c.finished, c.paused, c.abandoned).forEach { check(it, background, 3.0) }
            }
        }
        DeftReaderThemes.all.forEach {
            check(it.text, it.bg, 4.5)
            check(it.accent, it.bg, 4.5)
        }
    }
}
