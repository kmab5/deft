package com.kmab.deft.library.organization

import com.kmab.deft.core.model.ReadThroughOutcome
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class SeriesRulesTest : OrganizationFixture() {
    @Test fun absentIndexAppendsAfterNegativePrequelPosition() =
        runBlocking {
            book("Prequel", "Series", -2.0)
            book("Next", "Series")
            repo.autoFill()
            assertEquals(
                listOf(-2.0, -1.0),
                repo
                    .get()
                    .categories
                    .single()
                    .books
                    .map { it.membership.position },
            )
        }

    @Test fun absentIndicesAppendAndDuplicatePositionsRemainDistinct() =
        runBlocking {
            book("First", " Space   Saga ", 1.0)
            book("Second", "space saga", 2.0)
            book("Interlude A", "Space Saga", 2.5)
            book("Interlude B", "Space Saga", 2.5)
            book("Last", "Space Saga")
            book("Hidden", "Hidden Series", 1.0, ignored = true)
            repo.autoFill()
            val categories = repo.get().categories
            assertEquals(1, categories.size)
            val summary = categories.single()
            assertEquals("Space Saga", summary.category.name)
            assertEquals(listOf(1.0, 2.0, 2.5, 2.5, 3.5), summary.books.map { it.membership.position })
            assertTrue(summary.books.all { it.book.book.missing })
            repo.autoFill()
            assertEquals(
                5,
                repo
                    .get()
                    .categories
                    .single()
                    .books.size,
            )
        }

    @Test fun seriesNormalizationPreservesPunctuationAndDiacritics() =
        runBlocking {
            book("One", "Série", 1.0)
            book("Two", "Serie", 1.0)
            book("Three", "A-B", 1.0)
            book("Four", "A B", 1.0)
            repo.autoFill()
            assertEquals(4, repo.get().categories.size)
        }

    @Test fun renamedAutomaticSeriesKeepsItsOldAliasAndRemovingTypeRelinquishesOwnership() =
        runBlocking {
            val first = book("First", "Original", 1.0)
            repo.autoFill()
            val category =
                repo
                    .get()
                    .categories
                    .single()
                    .category.id
            repo.saveCategory(category, "Renamed", typeIds = setOf(1))
            val second = book("Second", "Original", 2.0)
            repo.autoFill()
            assertEquals(1, repo.get().categories.size)
            assertEquals(
                setOf(first, second),
                repo
                    .get()
                    .categories
                    .single()
                    .books
                    .map { it.book.book.id }
                    .toSet(),
            )
            repo.saveCategory(category, "Renamed")
            repo.autoFill()
            val categories = repo.get().categories
            assertEquals(2, categories.size)
            val previous = categories.single { it.category.id == category }
            assertFalse(previous.category.autoCreated)
            assertFalse(previous.isSeries)
            assertEquals(2, previous.books.size)
            assertTrue(categories.single { it.category.id != category }.category.autoCreated)
        }

    @Test fun nextInSeriesUsesStrictlyHigherPositionAndSkipsFinishedButNotAbandoned() =
        runBlocking {
            val first = book("First", "Series", 1.0)
            book("Same position", "Series", 1.0)
            val finished = book("Second", "Series", 2.0)
            val abandoned = book("Third", "Series", 2.5)
            book("Fourth", "Series", 3.0)
            read(finished, ReadThroughOutcome.FINISHED)
            read(abandoned, ReadThroughOutcome.ABANDONED)
            repo.autoFill()
            val snapshot = repo.get()
            assertEquals(
                abandoned,
                snapshot
                    .nextInSeries(first)
                    .single()
                    .book.book.id,
            )
            assertEquals(
                5,
                snapshot.categories
                    .single()
                    .stats.total,
            )
            assertEquals(
                1,
                snapshot.categories
                    .single()
                    .stats.finished,
            )
            assertEquals(
                1,
                snapshot.categories
                    .single()
                    .stats.abandoned,
            )
            assertTrue(snapshot.nextInSeries(999).isEmpty())
        }

    @Test fun latestReadThroughIdentityOverridesEditedDates() =
        runBlocking {
            val book = book("Book", "Series", 1.0)
            val first = read(book, ReadThroughOutcome.FINISHED)
            val second = read(book, ReadThroughOutcome.PAUSED)
            db.trackingDao().updateReadThrough(
                db.trackingDao().readThrough(first)!!.copy(startedAt = now.plusSeconds(999)),
            )
            session(book, first, 45, minimum = 30)
            session(book, second, 40, minimum = 60)
            repo.autoFill()
            val stats =
                repo
                    .get()
                    .categories
                    .single()
                    .stats
            assertEquals(1, stats.paused)
            assertEquals(0, stats.finished)
            assertEquals(45L, stats.activeSeconds)
        }
}
