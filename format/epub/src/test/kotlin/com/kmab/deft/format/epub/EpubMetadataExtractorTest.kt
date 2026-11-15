package com.kmab.deft.format.epub

import com.kmab.deft.core.model.BookFormat
import com.kmab.deft.core.model.MetadataException
import org.junit.jupiter.api.Assertions.assertArrayEquals
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class EpubMetadataExtractorTest {
    private fun extract(builder: TestEpubBuilder) =
        EpubMetadataExtractor().extract(builder.build().inputStream(), "book.epub")

    @Test fun `title and creators retain display order`() {
        val result =
            extract(
                TestEpubBuilder()
                    .title(
                        "Mistborn",
                    ).creator("Brandon Sanderson", "Sanderson, Brandon")
                    .creator("Other Author"),
            )
        assertEquals("Mistborn", result.title)
        assertEquals(listOf("Brandon Sanderson", "Other Author"), result.authors)
        assertEquals(setOf(BookFormat.EPUB), EpubMetadataExtractor().formats)
    }

    @Test fun `calibre series`() {
        val r = extract(TestEpubBuilder().calibreSeries("Mistborn", 2.0))
        assertEquals("Mistborn", r.seriesName)
        assertEquals(2.0, r.seriesIndex)
    }

    @Test fun `epub3 series`() {
        val r = extract(TestEpubBuilder().collection("The Stormlight Archive", 1.5))
        assertEquals("The Stormlight Archive", r.seriesName)
        assertEquals(1.5, r.seriesIndex)
    }

    @Test fun `ISBN identifiers ignore UUIDs`() {
        val r = extract(TestEpubBuilder().isbn("urn:isbn:9780765326355").isbn("0765326353"))
        assertEquals("9780765326355", r.isbn13)
        assertEquals("0765326353", r.isbn10)
        val uuid = extract(TestEpubBuilder().isbn("urn:uuid:1234567890123"))
        assertNull(uuid.isbn13)
        assertNull(uuid.isbn10)
    }

    @Test fun `both cover conventions and reverse order`() {
        for (epub2 in listOf(false, true)) {
            for (reverse in listOf(false, true)) {
                val b = TestEpubBuilder().cover(byteArrayOf(1, 2, 3))
                b.epub2Cover = epub2
                b.reverseEntries = reverse
                assertArrayEquals(byteArrayOf(1, 2, 3), extract(b).coverImage)
            }
        }
    }

    @Test fun `nested relative percent encoded cover`() {
        val b = TestEpubBuilder().entry("OPS/images/cover art.jpg", byteArrayOf(7))
        b.opfPath =
            "OPS/nested/package.opf"
        b.coverHref = "../images/cover%20art.jpg"
        assertArrayEquals(byteArrayOf(7), extract(b).coverImage)
    }

    @Test fun `font obfuscation is not DRM`() {
        for (algorithm in listOf(
            "http://www.idpf.org/2008/embedding",
            "http://ns.adobe.com/pdf/enc#RC",
        )) {
            assertFalse(extract(TestEpubBuilder().encryption(algorithm)).drmProtected)
        }
        assertTrue(extract(TestEpubBuilder().encryption("http://www.w3.org/2001/04/xmlenc#aes128-cbc")).drmProtected)
        for (path in listOf(
            "META-INF/rights.xml",
            "META-INF/license.lcpl",
        )) {
            assertTrue(extract(TestEpubBuilder().entry(path, byteArrayOf())).drmProtected)
        }
    }

    @Test fun `malformed ZIP throws metadata exception`() {
        for (bytes in listOf(
            byteArrayOf(),
            byteArrayOf(1, 2, 3),
            TestEpubBuilder().build().take(40).toByteArray(),
        )) {
            assertThrows(MetadataException::class.java) {
                EpubMetadataExtractor().extract(bytes.inputStream(), "bad.epub")
            }
        }
    }

    @Test fun `missing title stays null and description is plain text`() {
        val r = extract(TestEpubBuilder().language("en").description("<p>Hello <b>world</b></p>"))
        assertNull(r.title)
        assertEquals("en", r.language)
        assertEquals("Hello world", r.description)
    }

    @Test fun `unsafe and malformed XML rejected`() {
        for (xml in listOf(
            "<package>",
            "<package/><package/>",
            "<!DOCTYPE package [<!ENTITY x SYSTEM 'file:///secret'>]><package>&x;</package>",
        )) {
            val b = TestEpubBuilder()
            b.rawOpf = xml
            assertThrows(MetadataException::class.java) { extract(b) }
        }
    }
}
