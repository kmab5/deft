@file:OptIn(org.readium.r2.shared.ExperimentalReadiumApi::class)

package com.kmab.deft.format.readium

import com.kmab.deft.core.model.ReaderAlignment
import com.kmab.deft.core.model.ReaderMargins
import com.kmab.deft.core.model.ReaderPreferences
import com.kmab.deft.core.model.ReaderThemes
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import org.readium.r2.navigator.preferences.TextAlign
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class NavigatorPreferencesTest {
    @Test fun openDyslexicUsesDeftsSameOriginFamilyWithoutReadiumsBuiltinCollision() {
        assertEquals(
            "Deft OpenDyslexic",
            ReaderPreferences(fontFamily = "OpenDyslexic")
                .toNavigatorPreferences(ReaderThemes.builtIn.first())
                .fontFamily
                ?.name,
        )
    }

    @Test fun originalAlignmentLeavesPublisherParagraphAlignmentIntact() {
        assertNull(
            ReaderPreferences(alignment = ReaderAlignment.ORIGINAL)
                .toNavigatorPreferences(ReaderThemes.builtIn.first())
                .textAlign,
        )
    }

    @Test fun publisherFontCanBeKeptWhileOtherTypographyAndBlackThemeApply() {
        val mapped =
            ReaderPreferences(
                fontFamily = "original",
                fontSize = 1.4,
                lineHeight = 1.8,
                margins = ReaderMargins.LARGE,
                alignment = ReaderAlignment.JUSTIFY,
                hyphenation = false,
                scroll = true,
            ).toNavigatorPreferences(ReaderThemes.builtIn.single { it.id == "black" })
        assertNull(mapped.fontFamily)
        assertEquals(1.4, requireNotNull(mapped.fontSize), 0.0)
        assertEquals(1.8, requireNotNull(mapped.lineHeight), 0.0)
        assertEquals(1.5, requireNotNull(mapped.pageMargins), 0.0)
        assertEquals(TextAlign.JUSTIFY, mapped.textAlign)
        assertEquals(false, mapped.hyphens)
        assertEquals(true, mapped.scroll)
        assertEquals(android.graphics.Color.BLACK, mapped.backgroundColor?.int)
    }
}
