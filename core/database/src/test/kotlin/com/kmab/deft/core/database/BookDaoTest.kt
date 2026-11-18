package com.kmab.deft.core.database

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.kmab.deft.core.database.entity.BookEntity
import com.kmab.deft.core.database.entity.BookFileEntity
import com.kmab.deft.core.model.BookFormat
import com.kmab.deft.core.model.BookSort
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.time.Instant

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class BookDaoTest {
    private val db =
        Room
            .inMemoryDatabaseBuilder(
                ApplicationProvider.getApplicationContext(),
                AppDatabase::class.java,
            ).allowMainThreadQueries()
            .build()

    @After fun close() = db.close()

    private suspend fun book(
        hash: String = "hash",
        ignored: Boolean = false,
        override: String? = null,
    ) = db.bookDao().insert(
        BookEntity(
            contentHash = hash,
            format = BookFormat.EPUB,
            title = hash,
            sortTitle = hash,
            titleOverride = override,
            ignored = ignored,
            addedAt = Instant.EPOCH,
        ),
    )

    private suspend fun file(
        book: Long,
        uri: String,
        missing: Boolean = false,
    ) = db.bookFileDao().insert(
        BookFileEntity(
            bookId = book,
            rootUri = "R",
            uri = uri,
            displayPath = uri,
            size = 1,
            lastModified = 0,
            missing = missing,
            lastSeenAt = Instant.EPOCH,
        ),
    )

    private suspend fun list(
        missing: Boolean = false,
        sort: BookSort = BookSort.TITLE,
    ) = db.bookDao().observeList(missing, emptySet(), sort).first()

    @Test fun lastReadSortKeepsUnreadLastInBothDirectionsWithStableTies() =
        runBlocking {
            val fixtures =
                listOf(
                    "Unread Z" to null,
                    "Recent" to 20L,
                    "Same B" to 10L,
                    "Same A" to 10L,
                    "Unread A" to null,
                )
            fixtures.forEach { (title, readAt) ->
                val id =
                    db.bookDao().insert(
                        BookEntity(
                            contentHash = title,
                            format = BookFormat.EPUB,
                            title = title,
                            sortTitle = title,
                            addedAt = Instant.EPOCH,
                            lastReadAt = readAt?.let(Instant::ofEpochSecond),
                        ),
                    )
                file(id, title)
            }

            fun expected(reverse: Boolean) =
                if (reverse) {
                    listOf("Same A", "Same B", "Recent", "Unread A", "Unread Z")
                } else {
                    listOf("Recent", "Same A", "Same B", "Unread A", "Unread Z")
                }
            listOf(false, true).forEach { reverse ->
                val books = db.bookDao().observeList(false, emptySet(), BookSort.LAST_READ, reverse).first()
                assertEquals(expected(reverse), books.map { it.title })
                assertNull(books.last().lastReadAt)
            }
        }

    @Test fun inverseSortingReversesPrimaryFieldAndPreservesTitleFallback() =
        runBlocking {
            listOf("A", "B", "C").forEachIndexed { index, title ->
                val id =
                    db.bookDao().insert(
                        BookEntity(
                            contentHash = title,
                            format = BookFormat.EPUB,
                            title = title,
                            sortTitle = title,
                            addedAt = Instant.ofEpochSecond(if (index == 0) 1 else 2),
                        ),
                    )
                file(id, title)
                db.authorDao().setBookAuthors(id, listOf(if (index == 0) "Zebra" else "Alpha"))
            }

            suspend fun titles(
                sort: BookSort,
                reverse: Boolean,
            ) = db
                .bookDao()
                .observeList(false, emptySet(), sort, reverse)
                .first()
                .map { it.title }
            assertEquals(listOf("C", "B", "A"), titles(BookSort.TITLE, true))
            assertEquals(listOf("A", "B", "C"), titles(BookSort.AUTHOR, true))
            assertEquals(listOf("B", "C", "A"), titles(BookSort.DATE_ADDED, false))
            assertEquals(listOf("A", "B", "C"), titles(BookSort.DATE_ADDED, true))
        }

    @Test fun deletingBookFileKeepsBook() =
        runBlocking {
            val id = book()
            val file = file(id, "a")
            db.bookFileDao().deleteById(file)
            assertNotNull(db.bookDao().findByHash("hash"))
            assertTrue(db.bookDao().isMissing(id))
        }

    @Test fun bookWithOnePresentCopyIsNotMissing() =
        runBlocking {
            val id = book()
            file(id, "a")
            file(id, "b", true)
            assertFalse(db.bookDao().isMissing(id))
            assertEquals(listOf(id), list().map { it.id })
        }

    @Test fun missingBooksHiddenByDefault() =
        runBlocking {
            val id = book()
            file(id, "a", true)
            assertTrue(list().isEmpty())
            assertTrue(list(true).single().missing)
        }

    @Test fun titleOverrideWins() =
        runBlocking {
            file(book(override = "edited"), "a")
            assertEquals("edited", list().single().title)
        }

    @Test fun ignoredBooksNeverListed() =
        runBlocking {
            file(book(ignored = true), "a")
            assertTrue(list().isEmpty())
            assertTrue(list(true).isEmpty())
        }

    @Test fun pruneRemovesStaleCopiesOnly() =
        runBlocking {
            val a = book("a")
            val b = book("b")
            file(a, "present")
            file(a, "stale", true)
            file(b, "only", true)
            db.bookFileDao().pruneMissingWherePresentCopyExists()
            assertNull(db.bookFileDao().findByUri("stale"))
            assertNotNull(db.bookFileDao().findByUri("only"))
        }

    @Test fun markMissingUnderRootsExcept() =
        runBlocking {
            val id = book()
            repeat(2000) { file(id, "file$it") }
            db.bookFileDao().markMissingUnderRootsExcept(listOf("R"), (0..1998).map { "file$it" }.toSet())
            val files = db.bookFileDao().observeForBook(id).first()
            assertEquals(1999, files.count { !it.missing })
            assertTrue(files.single { it.uri == "file1999" }.missing)
        }

    @Test fun authorAliasesResolve() =
        runBlocking {
            assertEquals(
                db.authorDao().getOrCreate("Sanderson, Brandon"),
                db.authorDao().getOrCreate("Brandon Sanderson"),
            )
        }

    @Test fun sortByAuthorUsesFirstAuthorSortName() =
        runBlocking {
            val a = book("a")
            val z = book("z")
            file(a, "a")
            file(z, "z")
            db.authorDao().setBookAuthors(a, listOf("Zoe Zebra", "Amy Aardvark"))
            db.authorDao().setBookAuthors(z, listOf("Ben Baker"))
            assertEquals(listOf(z, a), list(sort = BookSort.AUTHOR).map { it.id })
            assertEquals("Zoe Zebra, Amy Aardvark", list().first().authorsDisplay)
        }

    @Test
    fun reconciliationRollsBackBothPhasesOnFailure() =
        runBlocking {
            val id = book()
            file(id, "a")
            file(id, "b")
            db.openHelper.writableDatabase.execSQL(
                "CREATE TRIGGER fail_restore BEFORE UPDATE OF missing ON book_files " +
                    "WHEN NEW.missing = 0 BEGIN SELECT RAISE(ABORT, 'restore failed'); END",
            )
            var failed = false
            try {
                db.bookFileDao().markMissingUnderRootsExcept(listOf("R"), setOf("a"))
            } catch (_: android.database.sqlite.SQLiteException) {
                failed = true
            }
            assertTrue(failed)
            assertEquals(
                2,
                db
                    .bookFileDao()
                    .observeForBook(id)
                    .first()
                    .count { !it.missing },
            )
        }

    @Test
    fun explicitAliasUsesCanonicalAuthor() =
        runBlocking {
            val id = db.authorDao().getOrCreate("Samuel Clemens")
            db.authorDao().addAlias(
                com.kmab.deft.core.database.entity
                    .AuthorAliasEntity("mark twain", id),
            )
            assertEquals(id, db.authorDao().getOrCreate("MARK TWAIN"))
        }

    @Test
    fun removingFolderCascadesExclusionsButRetainsBookHistory() =
        runBlocking {
            val id = book()
            file(id, "a")
            val folders = db.libraryFolderDao()
            folders.insert(
                com.kmab.deft.core.database.entity
                    .LibraryFolderEntity("R", "root", Instant.EPOCH),
            )
            folders.addExclusion(
                com.kmab.deft.core.database.entity
                    .ExcludedFolderEntity("R", "child", "child"),
            )
            assertEquals(1, folders.observeExclusions("R").first().size)
            folders.remove("R")
            assertTrue(folders.observeExclusions("R").first().isEmpty())
            assertNotNull(db.bookDao().findByHash("hash"))
            assertNotNull(db.bookFileDao().findByUri("a"))
        }
}
