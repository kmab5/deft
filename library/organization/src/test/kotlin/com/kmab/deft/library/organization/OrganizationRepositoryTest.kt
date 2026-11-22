package com.kmab.deft.library.organization

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.kmab.deft.core.database.AppDatabase
import com.kmab.deft.core.database.entity.BookEntity
import com.kmab.deft.core.model.BookFormat
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.time.Instant

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class OrganizationRepositoryTest {
    private lateinit var db: AppDatabase
    private lateinit var repo: OrganizationRepository

    @Before fun setup() {
        db = Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext(), AppDatabase::class.java).build()
        repo = OrganizationRepository(db)
    }

    @After fun close() = db.close()

    @Test fun freshLibraryIncludesProtectedSeriesType() =
        runBlocking {
            val types = repo.get().types
            assertEquals(1, types.size)
            assertEquals(1L, types.single().id)
            assertTrue(types.single().builtIn)
        }

    @Test fun autoFillOwnsItsSeriesThenExplicitMergeRedirectsFutureScans() =
        runBlocking {
            book("one", "Earthsea", 1.0, 3.5)
            book("two", " earthsea ", 2.5, 4.0)
            val manual = repo.saveCategory(name = "Earthsea", typeIds = setOf(1))
            repo.autoFill()
            val categories = repo.get().categories
            assertEquals(2, categories.size)
            val automatic = categories.single { it.category.autoCreated }
            assertNotEquals(manual, automatic.category.id)
            assertTrue(automatic.needsReview)
            assertEquals(listOf(1.0, 2.5), automatic.books.map { it.membership.position })
            repo.mergeCategories(automatic.category.id, manual)
            repo.autoFill()
            assertEquals(1, repo.get().categories.size)
            repo.setRating(manual, 4.5)
            assertEquals(
                4.5,
                repo
                    .get()
                    .categories
                    .single()
                    .rating!!,
                0.0,
            )
            repo.setRating(manual, null)
            assertEquals(
                3.75,
                repo
                    .get()
                    .categories
                    .single()
                    .rating!!,
                0.0,
            )
        }

    @Test fun removedSuggestionsStayRemovedUntilExplicitlyAdded() =
        runBlocking {
            val book = book("one", "Series", 1.0)
            repo.autoFill()
            assertEquals(1, repo.get().categories.size)
            val category =
                repo
                    .get()
                    .categories
                    .single()
                    .category.id
            repo.removeMember(category, book)
            repo.autoFill()
            assertTrue(
                repo
                    .get()
                    .categories
                    .single()
                    .books
                    .isEmpty(),
            )
            repo.addBooks(category, setOf(book))
            repo.autoFill()
            assertFalse(
                repo
                    .get()
                    .categories
                    .single()
                    .needsReview,
            )
            assertEquals(
                book,
                repo
                    .get()
                    .categories
                    .single()
                    .books
                    .single()
                    .book.book.id,
            )
        }

    private suspend fun book(
        title: String,
        series: String? = null,
        index: Double? = null,
        rating: Double? = null,
    ): Long =
        db.bookDao().insert(
            BookEntity(
                contentHash = title,
                format = BookFormat.EPUB,
                title = title,
                sortTitle = title,
                extractedSeriesName = series,
                extractedSeriesIndex = index,
                rating = rating,
                addedAt = Instant.EPOCH,
            ),
        )
}
