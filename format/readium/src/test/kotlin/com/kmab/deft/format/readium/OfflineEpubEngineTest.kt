package com.kmab.deft.format.readium

import android.content.ContentProvider
import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.net.Uri
import android.os.ParcelFileDescriptor
import androidx.test.core.app.ApplicationProvider
import com.kmab.deft.core.model.ReaderFont
import com.kmab.deft.core.model.ReaderFontFace
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.readium.r2.shared.util.Url
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowContentResolver
import java.io.File
import java.security.MessageDigest
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class OfflineEpubEngineTest {
    private val context = ApplicationProvider.getApplicationContext<Context>()

    @Test fun bundledFontsUseThePublicationOriginAndServeTheOriginalAssetBytes() =
        runTest {
            val source = epub()
            register(source)
            val assetPath = "readium/fonts/OpenDyslexic-Regular.otf"
            val font = ReaderFont("bundled-test", "Bundled test", listOf(ReaderFontFace(assetPath = assetPath)))
            OfflineEpubEngine(context).open(URI, hash(source.readBytes()), listOf(font)).use { opened ->
                val href = requireNotNull(Url(importedFontPath(font.id, 0)))
                val resource = requireNotNull(opened.publication.get(href))
                try {
                    val expected = context.assets.open(assetPath).use { it.readBytes() }
                    assertTrue(expected.contentEquals(resource.read().getOrNull()))
                    assertEquals(
                        "font/otf",
                        opened.publication
                            .linkWithHref(href)
                            ?.mediaType
                            .toString(),
                    )
                } finally {
                    resource.close()
                }
            }
        }

    @Test fun opensAnActualEpubWithoutChangingTheSourceAndBlocksRemoteResources() =
        runTest {
            val source = epub()
            val original = source.readBytes()
            register(source)
            OfflineEpubEngine(context).open(URI, hash(original), emptyList()).use { opened ->
                assertEquals("Offline fixture", opened.publication.metadata.title)
                val chapter = requireNotNull(opened.publication.get(requireNotNull(Url("EPUB/one.xhtml"))))
                val html =
                    try {
                        requireNotNull(chapter.read().getOrNull()).toString(Charsets.UTF_8)
                    } finally {
                        chapter.close()
                    }
                assertTrue(html.contains("A readable first chapter."))
                assertTrue(html.contains("Publisher Serif"))
                assertFalse(html.contains("<script"))
                assertTrue(html.contains("Content-Security-Policy"))
                javax.xml.parsers.DocumentBuilderFactory
                    .newInstance()
                    .newDocumentBuilder()
                    .parse(java.io.ByteArrayInputStream(html.toByteArray()))
                assertNull(opened.publication.get(requireNotNull(Url("https://example.com/tracker.png"))))
            }
            assertTrue(original.contentEquals(source.readBytes()))
            awaitReaderCacheEmpty(context)
        }

    @Test fun changedContentNeverReceivesASavedLocationAndCleansItsSnapshot() =
        runTest {
            register(epub())
            val result = runCatching { OfflineEpubEngine(context).open(URI, "0".repeat(64), emptyList()) }
            assertTrue(result.exceptionOrNull() is ReaderOpenException)
            assertTrue(
                result
                    .exceptionOrNull()
                    ?.message
                    .orEmpty()
                    .contains("changed"),
            )
            awaitReaderCacheEmpty(context)
        }

    @Test fun rejectsNetworkAndFileUrisBeforeAccess() =
        runTest {
            listOf("https://example.com/book.epub", "file:///private/book.epub").forEach {
                assertTrue(
                    runCatching {
                        OfflineEpubEngine(context).open(Uri.parse(it), "", emptyList())
                    }.exceptionOrNull() is ReaderOpenException,
                )
            }
        }

    private fun register(file: File) {
        val provider =
            ReadOnlyProvider(file).apply {
                attachInfo(
                    this@OfflineEpubEngineTest.context,
                    android.content.pm
                        .ProviderInfo()
                        .apply { authority = "deft-reader-test" },
                )
            }
        ShadowContentResolver.registerProviderInternal("deft-reader-test", provider)
    }

    @Test fun protectedEpubIsDetectedWithoutADecryptionProvider() =
        runTest {
            val file = epub(protectedBook = true)
            register(file)
            val failure =
                runCatching { OfflineEpubEngine(context).open(URI, hash(file.readBytes()), emptyList()) }
                    .exceptionOrNull()
            assertTrue(failure is ReaderOpenException)
            assertTrue(failure?.message.orEmpty().contains("Protected"))
        }

    @Test fun importedFontResourcesOnlyExposeRegisteredFilesWithinTheFontDirectory() =
        runTest {
            val source = epub()
            register(source)
            val directory = File(context.filesDir, "reader-fonts").apply { mkdirs() }
            val font = File(directory, "test.ttf").apply { writeBytes(byteArrayOf(0, 1, 0, 0)) }
            val outside = File(context.filesDir, "not-a-font").apply { writeText("private") }
            val catalog =
                listOf(
                    ReaderFont("safe", "Safe", listOf(ReaderFontFace(privateFilePath = font.path)), imported = true),
                    ReaderFont(
                        "outside",
                        "Outside",
                        listOf(ReaderFontFace(privateFilePath = outside.path)),
                        imported = true,
                    ),
                )
            OfflineEpubEngine(context).open(URI, hash(source.readBytes()), catalog).use { opened ->
                val href = requireNotNull(Url(importedFontPath("safe", 0)))
                assertTrue(
                    font.readBytes().contentEquals(
                        requireNotNull(
                            opened.publication
                                .get(href)
                                ?.read()
                                ?.getOrNull(),
                        ),
                    ),
                )
                assertEquals(
                    "font/ttf",
                    opened.publication
                        .linkWithHref(href)
                        ?.mediaType
                        .toString(),
                )
                assertNull(opened.publication.get(requireNotNull(Url(importedFontPath("outside", 0)))))
            }
            assertTrue(font.isFile)
        }

    @Test fun orphanCleanupDoesNotRemoveActiveSnapshotsOrUnrelatedCacheFiles() {
        val directory =
            java.nio.file.Files
                .createTempDirectory(context.cacheDir.toPath(), "snapshot-test-")
                .toFile()
        val orphan = File(directory, "epub-old.epub").apply { writeText("orphan") }
        val unrelated = File(directory, "keep.txt").apply { writeText("keep") }
        val active = ReaderSnapshots.create(directory)
        assertFalse(orphan.exists())
        ReaderSnapshots.create(directory)
        assertTrue(active.exists())
        assertTrue(unrelated.exists())
    }

    private fun epub(protectedBook: Boolean = false): File {
        val file = File.createTempFile("source-", ".epub", context.cacheDir)
        val files =
            listOf(
                "META-INF/container.xml",
                "EPUB/package.opf",
                "EPUB/one.xhtml",
                "EPUB/two.xhtml",
                "EPUB/nav.xhtml",
            )
        ZipOutputStream(file.outputStream()).use { zip ->
            val mimetype = "application/epub+zip".toByteArray()
            zip.putNextEntry(
                ZipEntry("mimetype").apply {
                    method = ZipEntry.STORED
                    size = mimetype.size.toLong()
                    compressedSize = size
                    crc =
                        java.util.zip
                            .CRC32()
                            .apply { update(mimetype) }
                            .value
                },
            )
            zip.write(mimetype)
            zip.closeEntry()
            files.forEach { name ->
                zip.putNextEntry(ZipEntry(name))
                requireNotNull(javaClass.classLoader?.getResourceAsStream("epub/$name")).use { it.copyTo(zip) }
                zip.closeEntry()
            }
            if (protectedBook) {
                zip.putNextEntry(ZipEntry("META-INF/license.lcpl"))
                zip.write("{}".toByteArray())
                zip.closeEntry()
            }
        }
        return file
    }

    private fun hash(bytes: ByteArray) =
        MessageDigest.getInstance("SHA-256").digest(bytes).joinToString("") {
            "%02x".format(it)
        }

    private class ReadOnlyProvider(
        private val file: File,
    ) : ContentProvider() {
        override fun onCreate() = true

        override fun openFile(
            uri: Uri,
            mode: String,
        ): ParcelFileDescriptor {
            check(mode == "r")
            return ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY)
        }

        override fun getType(uri: Uri) = "application/epub+zip"

        override fun query(
            uri: Uri,
            projection: Array<out String>?,
            selection: String?,
            selectionArgs: Array<out String>?,
            sortOrder: String?,
        ): Cursor? = null

        override fun insert(
            uri: Uri,
            values: ContentValues?,
        ): Uri? = error("Read only")

        override fun delete(
            uri: Uri,
            selection: String?,
            selectionArgs: Array<out String>?,
        ): Int = error("Read only")

        override fun update(
            uri: Uri,
            values: ContentValues?,
            selection: String?,
            selectionArgs: Array<out String>?,
        ): Int = error("Read only")
    }

    private companion object {
        val URI: Uri = Uri.parse("content://deft-reader-test/book")
    }
}

private suspend fun awaitReaderCacheEmpty(context: Context) =
    withContext(Dispatchers.IO) {
        withTimeout(CACHE_CLEANUP_TIMEOUT_MILLIS) {
            while (File(context.cacheDir, "reader").listFiles().orEmpty().isNotEmpty()) {
                delay(CACHE_CLEANUP_POLL_MILLIS)
            }
        }
    }

private const val CACHE_CLEANUP_TIMEOUT_MILLIS = 5_000L
private const val CACHE_CLEANUP_POLL_MILLIS = 10L
