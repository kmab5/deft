package com.kmab.deft.library.scanner

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.kmab.deft.core.common.TimeSource
import com.kmab.deft.core.database.AppDatabase
import com.kmab.deft.core.database.entity.BookEntity
import com.kmab.deft.core.database.entity.BookFileEntity
import com.kmab.deft.core.model.BookFormat
import com.kmab.deft.core.model.BookSort
import com.kmab.deft.core.model.ReadingPosition
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.time.Instant

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ReadingRepositoryTest {
    @get:Rule val temp = TemporaryFolder()
    private val context = ApplicationProvider.getApplicationContext<Context>()
    private val db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).build()
    private val coordination = ScanCoordination()
    private var now = Instant.parse("2026-11-18T12:00:00Z")
    private val repository = BookRepository(db, coordination, TimeSource { now })
    private val position = ReadingPosition("""{"href":"chapter.xhtml","locations":{"totalProgression":0.25}}""", 0.25)

    @After fun close() = db.close()

    @Test fun positionBookmarksAndIgnoreSurviveMissingAndReattachment() =
        runBlocking {
            val id = addBook("Kept")
            repository.recordOpened(id)
            repository.savePosition(id, position)
            val bookmark = repository.addBookmark(id, position, "Chapter one")
            assertEquals(bookmark, repository.addBookmark(id, position, "Duplicate location"))
            repository.renameBookmark(bookmark, "My name")
            repository.setIgnored(id, true)
            assertTrue(repository.observeBooks(true, BookFormat.entries.toSet(), BookSort.TITLE).first().isEmpty())
            assertEquals(listOf(id), repository.observeIgnoredBooks().first().map { it.id })
            db.bookFileDao().markMissingForRoot("root")
            repository.setIgnored(id, false)
            assertNull(repository.observeContinueReading().first())
            val file = db.bookFileDao().getForBook(id).single()
            db.bookFileDao().update(file.copy(missing = false))
            assertEquals(id, repository.observeContinueReading().first()?.id)
            assertEquals(position.locatorJson, repository.getBook(id)?.book?.currentLocator)
            assertEquals(0.25, repository.getBook(id)!!.book.totalProgression, 0.0)
            assertEquals(now, repository.getBook(id)?.book?.lastReadAt)
            assertEquals(
                "My name",
                repository
                    .observeBookmarks(id)
                    .first()
                    .single()
                    .label,
            )
        }

    @Test fun continueSkipsIgnoredProtectedUnsupportedAndMissingBooks() =
        runBlocking {
            val readable = addBook("Readable")
            val protected = addBook("Protected", protected = true)
            val pdf = addBook("PDF", format = BookFormat.PDF)
            listOf(readable, protected, pdf).forEach { id ->
                now = now.plusSeconds(1)
                repository.recordOpened(id)
                repository.savePosition(id, position)
            }
            assertEquals(readable, repository.observeContinueReading().first()?.id)
            repository.setIgnored(readable, true)
            assertNull(repository.observeContinueReading().first())
        }

    @Test fun forgettingWaitsForScanLockDeletesHistoryAndNeverTouchesTheSource() =
        runBlocking {
            val id = addBook("Original")
            val source = temp.newFile("original.epub").apply { writeText("Original bytes") }
            val file = db.bookFileDao().getForBook(id).single()
            db.bookFileDao().update(file.copy(uri = source.toURI().toString()))
            repository.savePosition(id, position)
            repository.addBookmark(id, position, "Remember")
            coordination.mutex.lock()
            val forgetting = async(start = CoroutineStart.UNDISPATCHED) { repository.forget(id) }
            try {
                assertFalse(forgetting.isCompleted)
                assertEquals(id, repository.getBook(id)?.book?.id)
            } finally {
                coordination.mutex.unlock()
            }
            forgetting.await()
            assertNull(repository.getBook(id))
            assertTrue(repository.observeBookmarks(id).first().isEmpty())
            assertTrue(db.bookFileDao().getForBook(id).isEmpty())
            repository.savePosition(id, position)
            assertNull(repository.getBook(id))
            assertEquals("Original bytes", source.readText())
        }

    @Test fun invalidPositionsCannotReplaceTheSavedLocation() =
        runBlocking {
            val id = addBook("Valid")
            repository.savePosition(id, position)
            listOf(
                position.copy(totalProgression = Double.NaN),
                position.copy(totalProgression = 2.0),
                position.copy(locatorJson = "{}"),
                position.copy(locatorJson = "not json"),
            ).forEach { invalid ->
                var rejected = false
                try {
                    repository.savePosition(id, invalid)
                } catch (_: IllegalArgumentException) {
                    rejected = true
                }
                assertTrue(rejected)
            }
            assertEquals(position.locatorJson, repository.getBook(id)?.book?.currentLocator)
        }

    @Test fun orderedPositionWritesSurviveClockRollbackAndPreserveMetadata() =
        runBlocking {
            val id = addBook("Original title")
            val metadata = requireNotNull(db.bookDao().getById(id)).copy(titleOverride = "My title", ignored = true)
            db.bookDao().update(metadata)
            repository.recordOpened(id)
            repository.savePosition(id, position)
            now = now.minusSeconds(1)
            val latest = position.copy(locatorJson = """{"href":"latest.xhtml"}""", totalProgression = 0.5)
            repository.savePosition(id, latest)
            val saved = requireNotNull(repository.getBook(id)).book
            assertEquals("My title", saved.titleOverride)
            assertTrue(saved.ignored)
            assertEquals(latest.locatorJson, saved.currentLocator)
            assertEquals(latest.totalProgression, saved.totalProgression, 0.0)
            assertEquals(now.plusSeconds(1), saved.lastReadAt)
            assertEquals(now.plusSeconds(1), saved.lastOpenedAt)
        }

    @Test fun continueFollowsLatestOpenWhenClockMovesBackward() =
        runBlocking {
            val first = addBook("First")
            val second = addBook("Second")
            repository.savePosition(first, position)
            repository.savePosition(second, position)
            repository.recordOpened(first)
            now = now.plusSeconds(1)
            repository.recordOpened(second)
            assertEquals(second, repository.observeContinueReading().first()?.id)
            val previous = requireNotNull(repository.getBook(second)?.book?.lastOpenedAt)
            now = now.minusSeconds(100)
            repository.recordOpened(first)
            assertEquals(first, repository.observeContinueReading().first()?.id)
            assertTrue(requireNotNull(repository.getBook(first)?.book?.lastOpenedAt).isAfter(previous))
        }

    private suspend fun addBook(
        title: String,
        protected: Boolean = false,
        format: BookFormat = BookFormat.EPUB,
    ): Long {
        val id =
            db.bookDao().insert(
                BookEntity(
                    contentHash = title,
                    format = format,
                    title = title,
                    sortTitle = title,
                    addedAt = now,
                    drmProtected = protected,
                ),
            )
        db.bookFileDao().insert(
            BookFileEntity(
                bookId = id,
                rootUri = "root",
                uri = "content://books/$id",
                displayPath = "$title.epub",
                size = 10,
                lastModified = 1,
                missing = false,
                lastSeenAt = now,
            ),
        )
        db.authorDao().setBookAuthors(id, listOf("Reader Author"))
        return id
    }
}
