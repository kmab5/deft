package com.kmab.deft.library.scanner

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.kmab.deft.core.common.ContentHasher
import com.kmab.deft.core.common.TimeSource
import com.kmab.deft.core.database.AppDatabase
import com.kmab.deft.core.database.entity.BookEntity
import com.kmab.deft.core.database.entity.BookFileEntity
import com.kmab.deft.core.database.entity.ExcludedFolderEntity
import com.kmab.deft.core.database.entity.LibraryFolderEntity
import com.kmab.deft.core.model.BookFormat
import com.kmab.deft.core.model.BookSort
import com.kmab.deft.core.model.ContentHash
import com.kmab.deft.format.epub.EpubMetadataExtractor
import com.kmab.deft.format.epub.TestEpubBuilder
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.time.Instant

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class LibraryScannerTest {
    private val db =
        Room
            .inMemoryDatabaseBuilder(
                ApplicationProvider.getApplicationContext(),
                AppDatabase::class.java,
            ).allowMainThreadQueries()
            .build()
    private val tree = FakeDocumentTree()
    private var covers = 0
    private val scanner =
        LibraryScanner(
            tree,
            ContentHasher(),
            MetadataExtractorRegistry(setOf(EpubMetadataExtractor())),
            object : CoverStore {
                override suspend fun save(
                    hash: ContentHash,
                    image: ByteArray,
                ): String {
                    covers++
                    return "private/${hash.hex}.jpg"
                }
            },
            db,
            TimeSource { Instant.EPOCH },
        )

    private suspend fun scan(
        roots: List<String> = listOf("R"),
        removed: List<String> = emptyList(),
    ) = scanner.scan(roots, setOf(BookFormat.EPUB), removed)

    private suspend fun books(missing: Boolean = true) =
        BookRepository(db).observeBooks(missing, emptySet(), BookSort.TITLE).first()

    @After fun close() = db.close()

    @Test fun newEpubIsAdded() =
        runBlocking {
            tree.put(
                "one",
                bytes =
                    TestEpubBuilder()
                        .title("Embedded")
                        .creator("Writer")
                        .cover(byteArrayOf(1))
                        .build(),
            )
            assertEquals(1, scan().added)
            assertEquals("Embedded", books().single().title)
            assertEquals("Writer", books().single().authorsDisplay)
            assertEquals(1, covers)
        }

    @Test fun disabledFormatIgnored() =
        runBlocking {
            tree.put("one")
            assertEquals(0, scanner.scan(listOf("R"), setOf(BookFormat.PDF)).added)
            assertTrue(books().isEmpty())
        }

    @Test fun unrecognizedExtensionsIgnored() =
        runBlocking {
            listOf("rtf", "lit", "jpg").forEach { extension ->
                tree.put(extension)
                val (e, b) = tree.files.getValue(extension)
                tree.files[extension] =
                    e.copy(name = "x.$extension") to b
            }
            scan()
            assertTrue(books().isEmpty())
        }

    @Test fun unchangedFileIsNotRehashed() =
        runBlocking {
            tree.put("one")
            scan()
            tree.opens = 0
            assertEquals(1, scan().unchanged)
            assertEquals(0, tree.opens)
        }

    @Test fun duplicateContentIsOneBook() =
        runBlocking {
            tree.put("one", bytes = byteArrayOf(1))
            tree.put("two", "S", byteArrayOf(1))
            scan(listOf("R", "S"))
            val book = books().single()
            assertEquals(
                2,
                db
                    .bookFileDao()
                    .observeForBook(book.id)
                    .first()
                    .size,
            )
            tree.files.remove("one")
            scan(listOf("R", "S"))
            assertFalse(books().single().missing)
            assertEquals(
                1,
                db
                    .bookFileDao()
                    .observeForBook(book.id)
                    .first()
                    .size,
            )
        }

    @Test fun deletedFileMarksBookMissingAndRestores() =
        runBlocking {
            tree.put("one")
            scan()
            val id = books().single().id
            val saved = tree.files.remove("one")!!
            assertEquals(1, scan().markedMissing)
            assertTrue(books().single().missing)
            tree.files["one"] = saved
            assertEquals(1, scan().reattached)
            assertEquals(id, books().single().id)
        }

    @Test fun movedFileReattachesInEitherRootOrder() =
        runBlocking {
            for (roots in listOf(listOf("R", "S"), listOf("S", "R"))) {
                tree.files.clear()
                tree.put("original${roots.first()}", bytes = roots.first().toByteArray())
                scan(roots)
                val id = books(false).single().id
                val bytes =
                    tree.files.values
                        .single()
                        .second
                tree.files.clear()
                tree.put("moved${roots.first()}", "S", bytes)
                assertEquals(1, scan(roots).reattached)
                assertEquals(id, books(false).single().id)
                tree.files.clear()
                scan(roots)
            }
        }

    @Test fun changedContentIsNewBook() =
        runBlocking {
            tree.put("one")
            scan()
            val id = books().single().id
            tree.put("one", bytes = byteArrayOf(1, 2, 3, 4, 5))
            assertEquals(1, scan().updated)
            assertEquals(2, books().size)
            assertTrue(books().first { it.id == id }.missing)
        }

    @Test fun unavailableRootCannotBlockHealthyRootAncestryValidation() =
        runBlocking {
            tree.put("old", root = "R")
            scan()
            tree.unavailable += "R"
            tree.relationshipFailure = java.io.IOException("Revoked root relationship")
            tree.put("healthy", root = "S", bytes = byteArrayOf(42))
            val report = scan(listOf("R", "S"))
            assertEquals(listOf("R"), report.unavailableRoots)
            assertEquals(1, report.added)
            assertEquals(2, books().size)
        }

    @Test fun unavailableAndIncompleteRootsNeverMarkUnseenMissing() =
        runBlocking {
            tree.put("one")
            tree.put("two")
            scan()
            tree.files.remove("two")
            tree.unavailable += "R"
            assertEquals(listOf("R"), scan().unavailableRoots)
            assertEquals(2, books(false).size)
            tree.unavailable.clear()
            tree.failWalk += "R"
            assertEquals(1, scan().errors)
            assertEquals(2, books(false).size)
        }

    @Test fun cancellationPropagatesWithoutMissing() =
        runBlocking {
            tree.put("one")
            tree.put("two")
            scan()
            tree.files.remove("two")
            tree.interruption =
                CancellationException("stop")
            try {
                scan()
                fail("Expected cancellation")
            } catch (_: CancellationException) {
            }
            assertEquals(2, books(false).size)
        }

    @Test fun removedRootMarksMissingButActiveRootWins() =
        runBlocking {
            tree.put("one")
            scan()
            scan(removed = listOf("R"))
            assertEquals(1, books(false).size)
            assertEquals(1, scan(emptyList(), listOf("R")).markedMissing)
            assertTrue(books().single().missing)
        }

    @Test fun excludedFileReattachesAndIgnoredBookStaysIgnored() =
        runBlocking {
            tree.put("one")
            tree.put("two")
            scan()
            val id = db.bookFileDao().findByUri("one")!!.bookId
            db.libraryFolderDao().insert(LibraryFolderEntity("R", "Root", Instant.EPOCH))
            tree.ancestors["one"] = setOf("manuals")
            db.libraryFolderDao().addExclusion(ExcludedFolderEntity("R", "manuals", "Root/manuals"))
            assertEquals(1, scan().markedMissing)
            db.libraryFolderDao().removeExclusion("R", "manuals")
            assertEquals(1, scan().reattached)
            val book = db.bookDao().findByHash(ContentHasher().hash("one".byteInputStream()).hex)!!
            db.bookDao().update(book.copy(ignored = true))
            tree.files.remove("one")
            tree.put("moved", bytes = "one".toByteArray())
            scan()
            assertEquals(id, db.bookFileDao().findByUri("moved")!!.bookId)
            assertEquals(1, books().size)
        }

    @Test fun corruptFileFallsBackAndDrmIsFlagged() =
        runBlocking {
            tree.put("bad")
            tree.put(
                "drm",
                bytes =
                    TestEpubBuilder()
                        .title(
                            "Protected",
                        ).encryption("http://www.w3.org/2001/04/xmlenc#aes256-cbc")
                        .build(),
            )
            assertEquals(1, scan().errors)
            assertEquals("bad", books().first { !it.drmProtected }.title)
            assertTrue(books().any { it.drmProtected })
        }

    @Test fun overlappingRootsRejectedInBothOrders() =
        runBlocking {
            tree.parents["S"] = "R"
            for (roots in listOf(listOf("R", "S"), listOf("S", "R"))) {
                try {
                    scan(roots)
                    fail("Expected overlapping roots rejection")
                } catch (_: IllegalArgumentException) {
                }
            }
        }

    @Test fun replacedBookWithoutFileRowsStillReattaches() =
        runBlocking {
            tree.put("one", bytes = byteArrayOf(1))
            scan()
            val original = books().single().id
            tree.put("one", bytes = byteArrayOf(1, 2))
            scan()
            tree.put("restored", bytes = byteArrayOf(1))
            assertEquals(1, scan().reattached)
            assertFalse(books().first { it.id == original }.missing)
        }

    @Test fun readFailureKeepsKnownFilePresent() =
        runBlocking {
            tree.put("one")
            scan()
            tree.put("one", bytes = byteArrayOf(1, 2, 3, 4))
            tree.failOpen += "one"
            assertEquals(1, scan().errors)
            assertEquals(1, books(false).size)
        }

    @Test fun repositoryFiltersAndDateAddedSort() =
        runBlocking {
            for ((title, format, at) in listOf(
                Triple("Old", BookFormat.EPUB, 1L),
                Triple("New", BookFormat.EPUB, 2L),
                Triple("Pdf", BookFormat.PDF, 3L),
            )) {
                db.bookDao().insert(
                    BookEntity(
                        contentHash = title,
                        format = format,
                        title = title,
                        sortTitle = title,
                        addedAt = Instant.ofEpochSecond(at),
                    ),
                )
            }
            val repository = BookRepository(db)
            assertEquals(
                listOf(
                    "New",
                    "Old",
                ),
                repository.observeBooks(true, setOf(BookFormat.EPUB), BookSort.DATE_ADDED).first().map {
                    it.title
                },
            )
            assertTrue(repository.observeBooks(false, setOf(BookFormat.EPUB), BookSort.DATE_ADDED).first().isEmpty())
        }

    @Test fun realEpubUsesPrivateTemporaryDirectoryAndCleansIt() =
        runBlocking {
            val context = ApplicationProvider.getApplicationContext<android.content.Context>()
            val dir = java.io.File(context.cacheDir, "epub-test").apply { mkdirs() }
            val previous = System.getProperty("java.io.tmpdir")
            var observedPrivateSpool = false
            tree.onRead =
                { if (dir.listFiles()!!.any { it.name.startsWith("deft-epub-") }) observedPrivateSpool = true }
            try {
                System.setProperty("java.io.tmpdir", dir.absolutePath)
                tree.put("real", bytes = TestEpubBuilder().title("Private spool").build())
                scan()
                assertEquals("Private spool", books().single().title)
                assertTrue("EPUB spool must be in app-private cache", observedPrivateSpool)
                assertTrue(dir.listFiles()!!.isEmpty())
            } finally {
                System.setProperty("java.io.tmpdir", previous)
                dir.delete()
            }
        }

    @Test fun unknownStampsAreRehashed() =
        runBlocking {
            tree.put("one", stamp = 0)
            scan()
            tree.opens = 0
            scan()
            assertTrue(tree.opens > 0)
        }

    @Test fun scanOf5000UnchangedFilesHashesNothing() =
        runBlocking {
            val id =
                db.bookDao().insert(
                    BookEntity(
                        contentHash = "seed",
                        format = BookFormat.EPUB,
                        title = "Seed",
                        sortTitle = "seed",
                        addedAt = Instant.EPOCH,
                    ),
                )
            repeat(5000) { n ->
                tree.put("f$n")
                val e = tree.files.getValue("f$n").first
                db.bookFileDao().insert(
                    BookFileEntity(
                        bookId = id,
                        rootUri = "R",
                        uri = e.uri,
                        displayPath = e.uri,
                        size = e.size,
                        lastModified = 1,
                        missing = false,
                        lastSeenAt = Instant.EPOCH,
                    ),
                )
            }
            assertEquals(5000, scan().unchanged)
            assertEquals(0, tree.opens)
        }

    @Test fun sameContentWithChangedStampRemainsOneBook() =
        runBlocking {
            tree.put("one")
            scan()
            val id = books().single().id
            tree.put("one", stamp = 2)
            assertEquals(1, scan().updated)
            assertEquals(id, books().single().id)
            assertFalse(books().single().missing)
        }

    @Test fun failedRelationshipCheckDoesNotReconcile() =
        runBlocking {
            tree.put("one")
            scan()
            tree.files.clear()
            tree.relationshipFailure = java.io.IOException("Provider unavailable")
            try {
                scan(listOf("R", "S"))
                fail("Expected provider failure")
            } catch (_: java.io.IOException) {
            }
            assertEquals(1, books(false).size)
        }

    @Test fun perFileWritesRollbackTogether() =
        runBlocking {
            db.openHelper.writableDatabase.execSQL(
                "CREATE TRIGGER reject_file BEFORE INSERT ON book_files " +
                    "BEGIN SELECT RAISE(ABORT, 'test rejection'); END",
            )
            tree.put("one", bytes = TestEpubBuilder().title("Atomic").creator("Writer").build())
            try {
                scan()
                fail("Expected database failure")
            } catch (_: android.database.sqlite.SQLiteException) {
            }
            assertTrue(books().isEmpty())
            db.openHelper.readableDatabase.query("SELECT COUNT(*) FROM authors").use { cursor ->
                assertTrue(cursor.moveToFirst())
                assertEquals(0, cursor.getInt(0))
            }
        }

    @Test fun failedReadDoesNotRestoreMissingBookWithUnverifiedReplacement() =
        runBlocking {
            tree.put("one", bytes = byteArrayOf(1))
            scan()
            val original = books().single().id
            tree.files.clear()
            scan()
            assertTrue(books().single().missing)

            tree.put("one", bytes = byteArrayOf(1, 2), stamp = 2)
            tree.failOpen += "one"
            val report = scan()
            assertEquals(1, report.errors)
            assertEquals(0, report.reattached)
            assertEquals(original, books().single().id)
            assertTrue(books().single().missing)
            assertTrue(db.bookFileDao().findByUri("one")!!.missing)
            assertTrue(books(false).isEmpty())

            tree.failOpen.clear()
            assertEquals(1, scan().updated)
            assertEquals(2, books().size)
            assertTrue(books().first { it.id == original }.missing)
            assertFalse(books(false).single().id == original)
        }
}
