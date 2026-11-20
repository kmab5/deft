package com.kmab.deft.feature.readersettings

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.ByteArrayInputStream
import java.io.File
import java.io.IOException
import java.nio.ByteBuffer
import java.nio.ByteOrder

class ImportedFontStoreTest {
    @get:Rule val temporary = TemporaryFolder()

    @Test fun importedNamesNeverControlPrivatePathsAndCatalogSurvivesRecreation() {
        val directory = temporary.newFolder()
        val store = ImportedFontStore(directory)
        val imported = store.importFont(ByteArrayInputStream(fontBytes()), "../../untrusted.ttf")
        assertEquals("untrusted", imported.name)
        val file = File(imported.faces.single().privateFilePath!!)
        assertEquals(directory.canonicalFile, requireNotNull(file.parentFile).canonicalFile)
        assertTrue(file.name.matches(Regex("[a-f0-9-]{36}\\.ttf")))
        assertEquals(imported, ImportedFontStore(directory).load().single())
        store.remove(imported.id)
        assertFalse(file.exists())
        assertTrue(store.load().isEmpty())
    }

    @Test fun invalidAndOversizedFontsLeaveNoPartialFiles() {
        val directory = temporary.newFolder()
        val store = ImportedFontStore(directory)
        listOf(ByteArray(10), fontBytes().copyOf(30)).forEach { bytes ->
            try {
                store.importFont(ByteArrayInputStream(bytes), "bad.ttf")
                error("Invalid font accepted")
            } catch (_: IOException) {
                assertTrue(directory.listFiles().orEmpty().isEmpty())
            }
        }
        try {
            store.importFont(ByteArrayInputStream(ByteArray(ImportedFontStore.MAX_BYTES + 1)), "large.ttf")
            error("Oversized font accepted")
        } catch (_: IOException) {
            assertTrue(directory.listFiles().orEmpty().isEmpty())
        }
    }

    @Test fun corruptLabelsAndTraversalIdsCannotExposeOtherFiles() {
        val directory = temporary.newFolder()
        File(directory, "outside.label").writeText("Untrusted")
        val store = ImportedFontStore(directory)
        assertTrue(store.load().isEmpty())
        try {
            store.remove("../outside")
            error("Traversal accepted")
        } catch (_: IllegalArgumentException) {
            assertTrue(File(directory, "outside.label").exists())
        }
    }
}

/** A minimal directory-shaped SFNT. Rendering validation is added by the Android repository. */
private fun fontBytes(): ByteArray {
    val tables = listOf("cmap", "head", "hhea", "hmtx", "maxp", "name", "glyf", "loca")
    val headerSize = 12 + tables.size * 16
    val output = ByteBuffer.allocate(headerSize + tables.size * 4).order(ByteOrder.BIG_ENDIAN)
    output
        .putInt(0x00010000)
        .putShort(tables.size.toShort())
        .putShort(0)
        .putShort(0)
        .putShort(0)
    tables.forEachIndexed { index, tag ->
        output
            .put(tag.toByteArray(Charsets.US_ASCII))
            .putInt(0)
            .putInt(headerSize + index * 4)
            .putInt(4)
    }
    return output.array()
}
