@file:OptIn(org.readium.r2.shared.ExperimentalReadiumApi::class)

package com.kmab.deft.feature.reader

import com.kmab.deft.core.model.ReaderFont
import com.kmab.deft.core.model.ReaderFontFace
import com.kmab.deft.core.model.ReaderPreferences
import com.kmab.deft.core.model.ReaderThemes
import com.kmab.deft.feature.readersettings.ReaderFontCatalog
import com.kmab.deft.format.readium.navigatorConfiguration
import com.kmab.deft.format.readium.toNavigatorPreferences
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import org.readium.r2.navigator.epub.EpubNavigatorFragment
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class NavigatorFontCatalogTest {
    private val theme = ReaderThemes.builtIn.first()

    @Test fun realBundledCatalogCanConfigureTheNavigator() {
        val configuration = navigatorConfiguration(ReaderFontCatalog.bundled, theme)

        assertEquals(11, declarations(configuration).size)
        assertEquals(listOf("reader-fonts/.*"), configuration.servedAssets)
    }

    @Test fun originalUsesPublisherFontsWithoutAnEmptyCustomDeclaration() {
        val original = ReaderFontCatalog.bundled.single { it.id == "original" }
        val configuration = navigatorConfiguration(listOf(original), theme)

        assertEquals(emptyList<Any>(), declarations(configuration))
        assertNull(ReaderPreferences(fontFamily = original.id).toNavigatorPreferences(theme).fontFamily)
    }

    @Test fun importedFontsRemainRegisteredAlongsideEveryBundledFamily() {
        val imported =
            ReaderFont(
                id = "imported-test",
                name = "Imported test",
                faces = listOf(ReaderFontFace(privateFilePath = "/private/reader-fonts/test.ttf")),
                imported = true,
            )
        val configuration = navigatorConfiguration(ReaderFontCatalog.bundled + imported, theme)

        assertEquals(12, declarations(configuration).size)
        assertEquals(
            imported.id,
            ReaderPreferences(fontFamily = imported.id).toNavigatorPreferences(theme).fontFamily?.name,
        )
    }

    // Readium exposes registration but keeps its resulting declarations internal.
    private fun declarations(configuration: EpubNavigatorFragment.Configuration): List<*> =
        configuration.javaClass
            .getDeclaredField("fontFamilyDeclarations")
            .apply { isAccessible = true }
            .get(configuration) as List<*>
}
