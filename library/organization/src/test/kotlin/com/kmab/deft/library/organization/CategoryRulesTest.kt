package com.kmab.deft.library.organization

import com.kmab.deft.core.model.CategoryOrder
import com.kmab.deft.core.model.ReadThroughOutcome
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class CategoryRulesTest : OrganizationFixture() {
    @Test fun creatingCategoryWithStaleSelectionRollsBackTheWholeCreation() =
        runBlocking {
            val book = book("Selected")
            invalid { repo.createCategoryWithBooks(name = "Must not remain", bookIds = setOf(book, 999)) }
            assertTrue(repo.get().categories.isEmpty())
            val category =
                repo.createCategoryWithBooks(
                    name = "Added together",
                    typeIds = setOf(1),
                    bookIds = setOf(book),
                )
            val created = repo.get().categories.single()
            assertEquals(category, created.category.id)
            assertEquals(
                book,
                created.books
                    .single()
                    .book.book.id,
            )
            assertTrue(created.isSeries)
        }

    @Test fun categoriesMayShareNamesButTypesAreNormalizedAndProtected() =
        runBlocking {
            val a = repo.saveCategory(name = "Shared")
            val b = repo.saveCategory(name = "Shared")
            assertNotEquals(a, b)
            val type = repo.saveType(name = "  Book   club ")
            assertEquals(
                "Book club",
                repo
                    .get()
                    .types
                    .single { it.id == type }
                    .name,
            )
            invalid { repo.saveType(name = "book club") }
            invalid { repo.saveType(1, "Something else") }
            invalid { repo.deleteType(1) }
            invalid { repo.saveCategory(name = " ") }
            invalid { repo.saveCategory(name = "x".repeat(241)) }
            invalid { repo.saveCategory(name = "Valid", description = "x".repeat(10001)) }
            invalid { repo.saveCategory(name = "Valid", typeIds = setOf(999)) }
            assertEquals(2, repo.get().categories.size)
        }

    @Test fun reorderRejectsPartialDuplicateAndSeriesListsWhileRetainingIgnoredMemberships() =
        runBlocking {
            val a = book("A")
            val b = book("B")
            val hidden = book("Hidden", ignored = true)
            val category = repo.saveCategory(name = "Manual")
            repo.addBooks(category, setOf(a, b, hidden))
            invalid { repo.reorder(category, listOf(a)) }
            invalid { repo.reorder(category, listOf(a, a)) }
            invalid { repo.reorder(category, listOf(a, b, hidden)) }
            repo.reorder(category, listOf(b, a))
            assertEquals(
                listOf(b, a),
                repo
                    .get()
                    .categories
                    .single()
                    .books
                    .map { it.book.book.id },
            )
            db.bookDao().setIgnored(hidden, false)
            assertEquals(
                3,
                repo
                    .get()
                    .categories
                    .single()
                    .books.size,
            )
            repo.saveCategory(category, "Manual", typeIds = setOf(1))
            invalid { repo.reorder(category, listOf(a, b, hidden)) }
        }

    @Test fun seriesDecimalsAreFiniteAndConfirmationSurvivesRescan() =
        runBlocking {
            val a = book("One", "Series", 1.0)
            val b = book("Two", "Series", 2.0)
            val c = book("Two and half", "Series", 2.5)
            repo.autoFill()
            val category =
                repo
                    .get()
                    .categories
                    .single()
                    .category.id
            repo.setPosition(category, c, 0.0)
            repo.confirmMembers(category, setOf(a, b))
            repo.autoFill()
            val summary = repo.get().categories.single()
            assertFalse(summary.needsReview)
            assertEquals(listOf(c, a, b), summary.books.map { it.book.book.id })
            invalid { repo.setPosition(category, a, Double.NaN) }
            invalid { repo.setPosition(category, a, Double.POSITIVE_INFINITY) }
            invalid { repo.setPosition(category, a, 1e10) }
            repo.setPosition(category, a, -0.5)
            assertEquals(
                a,
                repo
                    .get()
                    .categories
                    .single()
                    .books
                    .first()
                    .book.book.id,
            )
        }

    @Test fun strictMergeUnionsSuggestionsAndTargetPositionsWinOverExclusions() =
        runBlocking {
            val a = book("One", "Series", 1.0)
            val b = book("Two", "Series", 2.0)
            repo.autoFill()
            val source =
                repo
                    .get()
                    .categories
                    .single()
                    .category.id
            val type = repo.saveType(name = "TBR")
            val target = repo.saveCategory(name = "Mine", description = "Keep", typeIds = setOf(type))
            repo.addBooks(target, setOf(a, b))
            repo.removeMember(target, b)
            repo.setRating(target, 4.5)
            val oldPosition =
                repo
                    .get()
                    .categories
                    .single { it.category.id == target }
                    .books
                    .single()
                    .membership.position
            repo.mergeCategories(source, target)
            val result = repo.get().categories.single()
            assertEquals("Mine", result.category.name)
            assertEquals("Keep", result.category.description)
            assertEquals(4.5, result.rating!!, 0.0)
            assertEquals(setOf(a, b), result.books.map { it.book.book.id }.toSet())
            assertEquals(
                oldPosition,
                result.books
                    .single { it.book.book.id == a }
                    .membership.position,
                0.0,
            )
            assertFalse(
                result.books
                    .single { it.book.book.id == a }
                    .membership.suggested,
            )
            assertTrue(
                result.books
                    .single { it.book.book.id == b }
                    .membership.suggested,
            )
            assertEquals(setOf(1L, type), result.types.map { it.id }.toSet())
            repo.autoFill()
            assertEquals(1, repo.get().categories.size)
            invalid { repo.mergeCategories(target, target) }
            invalid { repo.mergeCategories(source, target) }
        }

    @Test fun ratingMeanIgnoresUnratedAndOverrideResetRestoresIt() =
        runBlocking {
            val rated = book("Rated", rating = 4.5)
            val other = book("Other", rating = 3.0)
            val unrated = book("Unrated")
            val category = repo.saveCategory(name = "Ratings")
            repo.addBooks(category, setOf(rated, other, unrated))
            assertEquals(
                3.75,
                repo
                    .get()
                    .categories
                    .single()
                    .rating!!,
                0.0,
            )
            invalid { repo.setRating(category, 3.7) }
            invalid { repo.setRating(category, Double.NaN) }
            invalid { repo.setRating(category, 0.0) }
            repo.setRating(category, 0.5)
            assertEquals(
                0.5,
                repo
                    .get()
                    .categories
                    .single()
                    .rating!!,
                0.0,
            )
            repo.setRating(category, null)
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

    @Test fun categoryAndTypeDeletionNeverDeletesBooksOrReadingHistory() =
        runBlocking {
            val book = book("Kept")
            val read = read(book, ReadThroughOutcome.FINISHED)
            session(book, read, 90)
            val type = repo.saveType(name = "Club")
            val category = repo.saveCategory(name = "Temporary", typeIds = setOf(type))
            repo.addBooks(category, setOf(book))
            repo.deleteType(type)
            assertTrue(
                repo
                    .get()
                    .categories
                    .single()
                    .types
                    .isEmpty(),
            )
            repo.deleteCategory(category)
            assertTrue(repo.get().categories.isEmpty())
            assertNotNull(db.bookDao().getById(book))
            assertEquals(
                90L,
                db
                    .trackingDao()
                    .getSessions(book)
                    .single()
                    .activeSeconds,
            )
            assertEquals(
                read,
                db
                    .trackingDao()
                    .getReadThroughs(book)
                    .single()
                    .id,
            )
        }

    @Test fun invalidBulkAdditionIsAtomicAndEmptyCategoryHasNoRating() =
        runBlocking {
            val a = book("A")
            val category = repo.saveCategory(name = "Empty")
            invalid { repo.addBooks(category, setOf(a, 999)) }
            val summary = repo.get().categories.single()
            assertTrue(summary.books.isEmpty())
            assertEquals(0, summary.stats.total)
            assertNull(summary.rating)
            invalid { repo.addBooks(999, setOf(a)) }
            invalid { repo.setRating(999, 4.0) }
        }

    @Test fun categorySortUsesMembershipAddedTimeAndPrimaryEffectiveAuthor() =
        runBlocking {
            val a = book("A", rating = 2.0)
            val z = book("Z", rating = 5.0)
            db.authorDao().setBookAuthors(a, listOf("Zoe Zebra"))
            db.authorDao().setBookAuthors(z, listOf("Ben Baker"))
            val category = repo.saveCategory(name = "Order", order = CategoryOrder.AUTHOR)
            repo.addBooks(category, setOf(a, z))
            assertEquals(
                listOf(z, a),
                repo
                    .get()
                    .categories
                    .single()
                    .books
                    .map { it.book.book.id },
            )
            repo.saveCategory(category, "Order", order = CategoryOrder.TITLE)
            assertEquals(
                listOf(a, z),
                repo
                    .get()
                    .categories
                    .single()
                    .books
                    .map { it.book.book.id },
            )
            repo.saveCategory(category, "Order", order = CategoryOrder.RATING)
            assertEquals(
                listOf(z, a),
                repo
                    .get()
                    .categories
                    .single()
                    .books
                    .map { it.book.book.id },
            )
            db.openHelper.writableDatabase.execSQL(
                "UPDATE category_members SET addedAt = 2000 WHERE bookId = ?",
                arrayOf(a),
            )
            db.openHelper.writableDatabase.execSQL(
                "UPDATE category_members SET addedAt = 1000 WHERE bookId = ?",
                arrayOf(z),
            )
            repo.saveCategory(category, "Order", order = CategoryOrder.DATE_ADDED)
            assertEquals(
                listOf(a, z),
                repo
                    .get()
                    .categories
                    .single()
                    .books
                    .map { it.book.book.id },
            )
        }
}
