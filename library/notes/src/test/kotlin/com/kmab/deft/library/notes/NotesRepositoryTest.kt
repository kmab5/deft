package com.kmab.deft.library.notes

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.kmab.deft.core.database.AppDatabase
import com.kmab.deft.core.database.entity.BookEntity
import com.kmab.deft.core.database.entity.BookFileEntity
import com.kmab.deft.core.database.entity.BookmarkEntity
import com.kmab.deft.core.model.BookFormat
import com.kmab.deft.core.model.HighlightColor
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.time.Instant

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class NotesRepositoryTest {
    private val db =
        Room
            .inMemoryDatabaseBuilder(
                ApplicationProvider.getApplicationContext<Context>(),
                AppDatabase::class.java,
            ).build()
    private val repository = NotesRepository(db)

    @After fun close() = db.close()

    @Test fun updatesByLocatorKeepIdentityAndCreationTime() =
        runBlocking {
            val book = insertBook("First")
            val draft = draft(book)
            val id = requireNotNull(repository.saveHighlight(draft))
            val initial = repository.observeHighlights(book).first().single()
            assertEquals(id, repository.saveHighlight(draft.copy(note = "Remember")))
            val updated = repository.observeHighlights(book).first().single()
            assertEquals(initial.createdAt, updated.createdAt)
            assertEquals("Remember", updated.note)
            assertTrue(updated.updatedAt >= initial.updatedAt)
            assertEquals(
                id,
                repository.saveHighlight(draft.copy(id = id, color = HighlightColor.NONE, note = "Remember")),
            )
            assertEquals(
                NoteType.NOTE,
                repository
                    .observe()
                    .first()
                    .single()
                    .type,
            )
            assertNull(repository.saveHighlight(draft.copy(id = id, color = HighlightColor.NONE, note = "  ")))
            assertTrue(repository.observe().first().isEmpty())
            assertNull(repository.saveHighlight(draft.copy(color = HighlightColor.NONE)))
        }

    @Test fun invalidAndStaleEditsLeaveExistingHighlightsAlone() =
        runBlocking {
            val book = insertBook("First")
            val other = insertBook("Other")
            val draft = draft(book)
            val id = requireNotNull(repository.saveHighlight(draft))
            assertInvalid { repository.saveHighlight(draft.copy(id = id, bookId = other)) }
            assertInvalid { repository.saveHighlight(draft.copy(locator = "")) }
            assertInvalid { repository.saveHighlight(draft.copy(totalProgression = Double.NaN)) }
            assertInvalid { repository.saveHighlight(draft.copy(text = " ")) }
            assertEquals(
                id,
                repository
                    .observe()
                    .first()
                    .single()
                    .id,
            )
            repository.deleteHighlight(id)
            assertInvalid { repository.saveHighlight(draft.copy(id = id)) }
            assertTrue(repository.observe().first().isEmpty())
        }

    @Test fun searchIsLiteralAndFiltersIntersectAcrossBookmarksAndNotes() =
        runBlocking {
            val book = insertBook("First")
            val other = insertBook("Other")
            repository.saveHighlight(draft(book).copy(text = "Écho 100%_done", note = "Remember"))
            repository.saveHighlight(draft(other).copy(text = "écho 100XXdone", note = "Remember"))
            val bookmarkId = insertBookmark(book)
            assertEquals(
                "Écho 100%_done",
                repository
                    .observe(NoteFilter(query = "éCHO 100%_"))
                    .first()
                    .single()
                    .text,
            )
            val filter =
                NoteFilter(bookId = book, color = HighlightColor.PURPLE, type = NoteType.NOTE, query = "remember")
            assertEquals(
                book,
                repository
                    .observe(filter)
                    .first()
                    .single()
                    .bookId,
            )
            assertTrue(repository.observe(filter.copy(type = NoteType.BOOKMARK)).first().isEmpty())
            repository.renameBookmark(bookmarkId, "Revisit 50%_")
            assertEquals(
                "Revisit 50%_",
                db
                    .bookmarkDao()
                    .observeForBook(book)
                    .first()
                    .single()
                    .label,
            )
            val bookmark = repository.observe(NoteFilter(type = NoteType.BOOKMARK, query = "%_")).first().single()
            assertEquals(bookmarkId, bookmark.id)
            repository.deleteBookmark(bookmarkId)
            assertTrue(
                db
                    .bookmarkDao()
                    .observeForBook(book)
                    .first()
                    .isEmpty(),
            )
        }

    @Test fun missingAndIgnoredBooksRetainHistoryUntilForget() =
        runBlocking {
            val book = insertBook("First")
            val file =
                BookFileEntity(
                    bookId = book,
                    rootUri = "root",
                    uri = "content://private/source",
                    displayPath = "First.epub",
                    size = 10,
                    lastModified = 0,
                    missing = false,
                    lastSeenAt = Instant.EPOCH,
                )
            val fileId = db.bookFileDao().insert(file)
            repository.saveHighlight(draft(book))
            insertBookmark(book)
            assertEquals(2, repository.observe().first().count { !it.missing })
            db.bookFileDao().markMissingForRoot("root")
            assertEquals(2, repository.observe().first().count { it.missing })
            db.bookFileDao().deleteById(fileId)
            assertEquals(2, repository.observe().first().count { it.missing })
            db.bookDao().setIgnored(book, true)
            assertTrue(repository.observe().first().isEmpty())
            assertInvalid { repository.exportMarkdown(book) }
            db.bookDao().setIgnored(book, false)
            assertEquals(2, repository.observe().first().size)
            db.bookDao().forget(book)
            assertTrue(repository.observeHighlights(book).first().isEmpty())
            assertTrue(
                db
                    .bookmarkDao()
                    .observeForBook(book)
                    .first()
                    .isEmpty(),
            )
        }

    @Test fun markdownUsesEffectiveMetadataAndEscapedContentInLocationOrder() =
        runBlocking {
            val book = insertBook("Original", titleOverride = "Edited *title*")
            db.authorDao().setBookAuthors(book, listOf("Extracted", "Edited [author]"))
            val editedAuthor = db.authorDao().getOrCreate("Edited [author]")
            db.openHelper.writableDatabase.execSQL(
                "UPDATE book_authors SET overridden = 1 WHERE bookId = ? AND authorId = ?",
                arrayOf(book, editedAuthor),
            )
            repository.saveHighlight(
                draft(book).copy(
                    text = "One\n> Two",
                    note = "# Note\n- Item",
                    color = HighlightColor.NONE,
                    totalProgression = 0.75,
                ),
            )
            insertBookmark(book)
            assertEquals(
                """
                # Edited \*title\*

                Edited \[author\]

                ## Bookmark · 50%

                Page \[one\]

                ## Note · 75%

                > One
                > \> Two

                \# Note
                \- Item
                """.trimIndent() + "\n",
                repository.exportMarkdown(book),
            )
            assertEquals("# Anonymous\n", repository.exportMarkdown(insertBook("Anonymous")))
        }

    private suspend fun insertBook(
        title: String,
        titleOverride: String? = null,
    ): Long =
        db.bookDao().insert(
            BookEntity(
                contentHash = title,
                format = BookFormat.EPUB,
                title = title,
                sortTitle = title,
                titleOverride = titleOverride,
                addedAt = Instant.EPOCH,
            ),
        )

    private fun draft(book: Long) =
        HighlightDraft(
            bookId = book,
            locator = """{"href":"chapter.xhtml","type":"application/xhtml+xml"}""",
            text = "Passage",
            color = HighlightColor.PURPLE,
        )

    private suspend fun insertBookmark(book: Long): Long =
        db.bookmarkDao().insert(
            BookmarkEntity(
                bookId = book,
                locator = """{"href":"content://private/book"}""",
                totalProgression = 0.5,
                label = "Page [one]",
                createdAt = Instant.EPOCH,
            ),
        )

    private suspend fun assertInvalid(block: suspend () -> Unit) {
        assertTrue(runCatching { block() }.exceptionOrNull() is IllegalArgumentException)
    }
}
