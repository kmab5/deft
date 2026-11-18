package com.kmab.deft.library.scanner

import com.kmab.deft.core.model.MetadataException
import com.kmab.deft.format.epub.EpubMetadataExtractor
import com.kmab.deft.format.epub.TestEpubBuilder
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class PlatformEpubParsingTest {
    @Test fun platformParserRejectsDeclarationsAndUnresolvedEntities() {
        val unsafe =
            listOf(
                "<!DOCTYPE package [<!ENTITY x SYSTEM 'file:///private-data'>]><package>&x;</package>",
                "<!DOCTYPE package SYSTEM 'https://example.invalid/unsafe.dtd'><package/>",
                "<!DOCTYPE package [<!ENTITY x 'custom'>]><package>&x;</package>",
                "<package>&undefined;</package>",
            )
        for (xml in unsafe) {
            val epub = TestEpubBuilder().apply { rawOpf = xml }.build()
            assertThrows(MetadataException::class.java) {
                EpubMetadataExtractor().extract(epub.inputStream(), "unsafe.epub")
            }
        }
    }

    @Test fun platformParserPreservesEscapingAndDrmLabels() {
        val extractor = EpubMetadataExtractor()
        val ordinary = TestEpubBuilder().title("A & B < C").build()
        assertEquals("A & B < C", extractor.extract(ordinary.inputStream(), "book.epub").title)
        val protected = TestEpubBuilder().encryption("http://www.w3.org/2001/04/xmlenc#aes128-cbc").build()
        assertTrue(extractor.extract(protected.inputStream(), "protected.epub").drmProtected)
        val fonts = TestEpubBuilder().encryption("http://www.idpf.org/2008/embedding").build()
        assertFalse(extractor.extract(fonts.inputStream(), "fonts.epub").drmProtected)
    }
}
