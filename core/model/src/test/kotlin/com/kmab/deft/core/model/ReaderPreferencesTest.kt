package com.kmab.deft.core.model

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class ReaderPreferencesTest {
    @Test fun nonFiniteAndOutOfRangePreferencesHaveSafeDefaults() {
        val invalid =
            ReaderPreferences(
                fontSize = Double.NaN,
                lineHeight = Double.POSITIVE_INFINITY,
                brightness = Float.NaN,
                fontFamily = " ",
            ).normalized()
        assertEquals(ReaderPreferences(), invalid)
        assertEquals(3.0, ReaderPreferences(fontSize = 99.0).normalized().fontSize)
        assertEquals(0f, ReaderPreferences(brightness = -1f).normalized().brightness)
    }

    @Test fun builtInThemesMatchReadablePalettesAndFollowSystemSelection() {
        assertEquals(listOf("light", "sepia", "dark", "black"), ReaderThemes.builtIn.map { it.id })
        ReaderThemes.builtIn.forEach {
            assertTrue(ReaderThemes.contrast(it.background, it.text) >= 4.5)
            assertTrue(ReaderThemes.contrast(it.background, it.accent) >= 4.5)
        }
        val prefs = ReaderPreferences(followSystem = true, lightThemeId = "sepia", darkThemeId = "black")
        assertEquals("sepia", prefs.activeTheme(false).id)
        assertEquals("black", prefs.activeTheme(true).id)
    }

    @Test fun customThemesRejectReservedIdsBadColorsAndUnreadableText() {
        val valid = ReaderTheme("custom-1", "night", "#000000", "#FFFFFF", "#C373FF")
        assertNull(ReaderThemes.validationError(valid))
        assertNotNull(ReaderThemes.validationError(valid.copy(id = "light")))
        assertNotNull(ReaderThemes.validationError(valid.copy(background = "javascript:alert(1)")))
        assertNotNull(ReaderThemes.validationError(valid.copy(text = "#111111")))
        assertNotNull(ReaderThemes.validationError(valid.copy(accent = "#111111")))
    }
}
