package com.kmab.deft.library.scanner

import com.kmab.deft.core.model.BookFormat
import com.kmab.deft.core.model.ExtractedMetadata
import com.kmab.deft.core.model.MetadataException
import com.kmab.deft.core.model.MetadataExtractor
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.ByteArrayInputStream
import java.io.IOException
import java.util.concurrent.CancellationException

class FilenameMetadataTest {
    @Test fun `ordered filename conventions`() {
        val series = FilenameMetadata.parse("Mistborn - 02 - The Well of Ascension.epub")
        assertEquals("Mistborn", series.seriesName)
        assertEquals(2.0, series.seriesIndex)
        assertEquals("The Well of Ascension", series.title)
        val parenthesized = FilenameMetadata.parse("The Final Empire (Mistborn #1).epub")
        assertEquals("The Final Empire", parenthesized.title)
        assertEquals("Mistborn", parenthesized.seriesName)
        assertEquals(1.0, parenthesized.seriesIndex)
        val author = FilenameMetadata.parse("Brandon Sanderson - Elantris.epub")
        assertEquals(listOf("Brandon Sanderson"), author.authors)
        assertEquals("Elantris", author.title)
        assertEquals("elantris", FilenameMetadata.parse("elantris_[retail].epub").title)
        assertEquals("Dune", FilenameMetadata.parse("Dune.fb2.zip").title)
        assertEquals("Dune", FilenameMetadata.parse("Dune_[OCR]_[v12]_[2020].PDF").title)
        assertEquals("Book [illustrated]", FilenameMetadata.parse("Book [illustrated].epub").title)
        assertEquals("A - B - C", FilenameMetadata.parse("A - B - C.epub").title)
    }
}

class MetadataExtractorRegistryTest {
    private fun extractor(block: () -> ExtractedMetadata) =
        object : MetadataExtractor {
            override val formats = setOf(BookFormat.EPUB)

            override fun extract(
                input: java.io.InputStream,
                fileName: String,
            ) = block()
        }

    private fun run(
        vararg extractors: MetadataExtractor,
        format: BookFormat = BookFormat.EPUB,
    ) = MetadataExtractorRegistry(
        extractors.toSet(),
    ).extract(format, ByteArrayInputStream(byteArrayOf()), "Title (Filename #2).epub")

    @Test fun `missing extractor uses filename without error`() {
        val (metadata, error) = run(format = BookFormat.PDF)
        assertEquals("Title", metadata.title)
        assertNull(error)
    }

    @Test fun `metadata and read failures report error with filename fallback`() {
        for (failure in listOf(MetadataException("bad metadata"), IOException("unreadable"))) {
            val (metadata, error) = run(extractor { throw failure })
            assertEquals("Title", metadata.title)
            assertTrue(error!!.contains(failure.message!!))
        }
    }

    @Test fun `extractor fields win with coherent series fallback`() {
        val (metadata, error) =
            run(
                extractor {
                    ExtractedMetadata.EMPTY.copy(seriesName = "Embedded", publisher = "Publisher", drmProtected = true)
                },
            )
        assertEquals("Title", metadata.title)
        assertEquals("Embedded", metadata.seriesName)
        assertNull(metadata.seriesIndex)
        assertEquals("Publisher", metadata.publisher)
        assertTrue(metadata.drmProtected)
        assertNull(error)
        assertEquals(2.0, run(extractor { ExtractedMetadata.EMPTY.copy(seriesName = "Filename") }).first.seriesIndex)
    }

    @Test fun `series fallback keeps a coherent name and index pair`() {
        val orphan = run(extractor { ExtractedMetadata.EMPTY.copy(seriesIndex = 9.0) }).first
        assertEquals("Filename", orphan.seriesName)
        assertEquals(2.0, orphan.seriesIndex)
        val matching = run(extractor { ExtractedMetadata.EMPTY.copy(seriesName = " filename ") }).first
        assertEquals(" filename ", matching.seriesName)
        assertEquals(2.0, matching.seriesIndex)
    }

    @Test fun `cancellation fatal errors and programming failures propagate`() {
        for (failure in listOf(
            CancellationException("cancel"),
            AssertionError("fatal"),
            IllegalStateException("bug"),
        )) {
            val caught = assertThrows(failure.javaClass) { run(extractor { throw failure }) }
            assertSame(failure, caught)
        }
    }

    @Test fun `duplicate formats are rejected`() {
        assertThrows(IllegalArgumentException::class.java) {
            MetadataExtractorRegistry(
                setOf(extractor { ExtractedMetadata.EMPTY }, extractor { ExtractedMetadata.EMPTY }),
            )
        }
    }

    @Test fun `caller retains input even when extractor closes it`() {
        var closed = false
        val input =
            object : ByteArrayInputStream(byteArrayOf(1)) {
                override fun close() {
                    closed = true
                }
            }
        val extractor =
            object : MetadataExtractor {
                override val formats = setOf(BookFormat.EPUB)

                override fun extract(
                    input: java.io.InputStream,
                    fileName: String,
                ): ExtractedMetadata {
                    input.close()
                    return ExtractedMetadata.EMPTY
                }
            }
        MetadataExtractorRegistry(setOf(extractor)).extract(BookFormat.EPUB, input, "Title.epub")
        assertFalse(closed)
        assertEquals(1, input.read())
    }
}
