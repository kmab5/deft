package com.kmab.deft.library.organization

import com.kmab.deft.core.database.entity.CategoryAutoFillExclusionEntity
import com.kmab.deft.core.model.CategoryOrder
import com.kmab.deft.core.model.ReadThroughOutcome
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class CategoryBulkOperationsTest : OrganizationFixture() {
    @Test fun emptySelectionsAndMergeWithoutAnotherSourceAreRejected() =
        runBlocking {
            val category = repo.saveCategory(name = "Kept")
            invalid { repo.deleteCategories(emptySet()) }
            invalid { repo.updateCategoryTypes(emptySet(), setOf(1), add = true) }
            invalid { repo.updateCategoryTypes(setOf(category), emptySet(), add = true) }
            invalid { repo.mergeCategories(emptySet(), category) }
            invalid { repo.mergeCategories(setOf(category), category) }
            assertEquals(listOf(category), db.organizationDao().categories().map { it.id })
        }

    @Test fun staleSelectionsLeaveAllCategoriesAndRelationsUntouched() =
        runBlocking {
            val book = book("Kept")
            val first = repo.createCategoryWithBooks(name = "First", bookIds = setOf(book))
            val second = repo.saveCategory(name = "Second", typeIds = setOf(1))
            val before = repo.get()
            invalid { repo.deleteCategories(linkedSetOf(first, second, 999)) }
            assertEquals(before, repo.get())
            invalid { repo.updateCategoryTypes(linkedSetOf(first, second, 999), setOf(1), add = true) }
            assertEquals(before, repo.get())
            invalid { repo.updateCategoryTypes(setOf(first, second), setOf(1, 999), add = true) }
            invalid { repo.updateCategoryTypes(setOf(first, second), setOf(1, 999), add = false) }
            assertEquals(before, repo.get())
            invalid { repo.mergeCategories(linkedSetOf(first, 999), second) }
            invalid { repo.mergeCategories(setOf(first, second), 999) }
            assertEquals(before, repo.get())
        }

    @Test fun bulkDeleteKeepsBooksHistoryTypesAndUnselectedCategories() =
        runBlocking {
            val book = book("History", "Original", 1.0)
            val read = read(book, ReadThroughOutcome.FINISHED)
            session(book, read, 90)
            repo.autoFill()
            val automatic =
                db
                    .organizationDao()
                    .categories()
                    .single()
                    .id
            val type = repo.saveType(name = "Club")
            val manual = repo.createCategoryWithBooks(name = "Temporary", typeIds = setOf(type), bookIds = setOf(book))
            val kept = repo.createCategoryWithBooks(name = "Kept", bookIds = setOf(book))
            repo.removeMember(automatic, book)

            repo.deleteCategories(setOf(automatic, manual))

            assertEquals(listOf(kept), db.organizationDao().categories().map { it.id })
            assertEquals(listOf(kept), db.organizationDao().members().map { it.categoryId })
            assertTrue(db.organizationDao().seriesAliases().isEmpty())
            assertTrue(db.organizationDao().exclusions().isEmpty())
            assertTrue(db.organizationDao().typeLinks().isEmpty())
            assertNotNull(db.organizationDao().type(type))
            assertNotNull(db.bookDao().getById(book))
            assertEquals(
                read,
                db
                    .trackingDao()
                    .getReadThroughs(book)
                    .single()
                    .id,
            )
            assertEquals(
                90L,
                db
                    .trackingDao()
                    .getSessions(book)
                    .single()
                    .activeSeconds,
            )
        }

    @Test fun typeAdditionAndRemovalPreserveUnrelatedTypesAndCategoryDetails() =
        runBlocking {
            val club = repo.saveType(name = "Club")
            val tbr = repo.saveType(name = "TBR")
            val added = repo.saveType(name = "New")
            val first =
                repo.saveCategory(
                    name = "First",
                    description = "Keep this description",
                    typeIds = setOf(club),
                    order = CategoryOrder.TITLE,
                    coverPath = "private-cover.jpg",
                )
            repo.setRating(first, 4.5)
            val second = repo.saveCategory(name = "Second", typeIds = setOf(tbr))
            val book = book("Member")
            repo.addBooks(first, setOf(book))
            val categories = db.organizationDao().categories()
            val members = db.organizationDao().members()

            repo.updateCategoryTypes(setOf(first, second), setOf(club, added), add = true)
            assertEquals(setOf(club, added), types(first))
            assertEquals(setOf(tbr, club, added), types(second))
            repo.updateCategoryTypes(setOf(first, second), setOf(club), add = false)
            assertEquals(setOf(added), types(first))
            assertEquals(setOf(tbr, added), types(second))
            assertEquals(categories, db.organizationDao().categories())
            assertEquals(members, db.organizationDao().members())
        }

    @Test fun removingSeriesClearsAliasesAndAutomaticStatusButAddingItDoesNotClaimOwnership() =
        runBlocking {
            book("Series book", "Original", 1.0)
            repo.autoFill()
            val automatic =
                db
                    .organizationDao()
                    .categories()
                    .single()
                    .id
            repo.saveCategory(automatic, "Renamed", typeIds = setOf(1))
            val custom = repo.saveCategory(name = "Custom")
            val type = repo.saveType(name = "Club")
            val aliases = db.organizationDao().seriesAliases()
            repo.updateCategoryTypes(setOf(automatic, custom), setOf(type), add = true)
            assertTrue(db.organizationDao().category(automatic)!!.autoCreated)
            assertTrue(db.organizationDao().seriesAliases().containsAll(aliases))

            repo.updateCategoryTypes(setOf(automatic, custom), setOf(1), add = false)
            assertTrue(db.organizationDao().seriesAliases().isEmpty())
            assertFalse(db.organizationDao().category(automatic)!!.autoCreated)
            assertEquals(setOf(type), types(automatic))
            repo.updateCategoryTypes(setOf(automatic, custom), setOf(1), add = true)
            assertFalse(db.organizationDao().category(automatic)!!.autoCreated)
            assertFalse(db.organizationDao().category(custom)!!.autoCreated)
            assertTrue(db.organizationDao().seriesAliases().isEmpty())
            assertEquals(setOf(1L, type), types(custom))
        }

    @Test fun multiSourceMergeKeepsSelectedTargetAndUsesAscendingSourcesForConflicts() =
        runBlocking {
            val overlap = book("Overlap")
            val sharedSource = book("Shared by sources")
            val extra = book("Extra")
            val first = repo.saveCategory(name = "First", typeIds = setOf(1))
            val second = repo.saveCategory(name = "Second", typeIds = setOf(1))
            val type = repo.saveType(name = "Club")
            val target =
                repo.saveCategory(
                    name = "Destination",
                    description = "Keep",
                    typeIds = setOf(type),
                    order = CategoryOrder.TITLE,
                    coverPath = "target-cover.jpg",
                )
            repo.setRating(target, 4.5)
            repo.addBooks(first, setOf(overlap, sharedSource))
            repo.addBooks(second, setOf(overlap, sharedSource, extra))
            repo.addBooks(target, setOf(overlap))
            repo.setPosition(first, overlap, 11.0)
            repo.setPosition(first, sharedSource, 12.0)
            repo.setPosition(second, sharedSource, 22.0)
            val targetDetails = db.organizationDao().category(target)
            val targetMember = db.organizationDao().members(target).single()
            val firstMember = db.organizationDao().members(first).single { it.bookId == sharedSource }

            repo.mergeCategories(linkedSetOf(second, target, first), target)

            assertEquals(listOf(targetDetails), db.organizationDao().categories())
            assertEquals(setOf(1L, type), types(target))
            val members = db.organizationDao().members(target)
            assertEquals(setOf(overlap, sharedSource, extra), members.map { it.bookId }.toSet())
            assertEquals(targetMember, members.single { it.bookId == overlap })
            assertEquals(firstMember.copy(categoryId = target), members.single { it.bookId == sharedSource })
        }

    @Test fun multiSourceMergeRedirectsAliasesAndUnionsExclusionsWithMembersWinning() =
        runBlocking {
            val a = book("A", "First", 1.0)
            val b = book("B", "Second", 2.0)
            val excluded = book("Excluded")
            repo.autoFill()
            val sources =
                db
                    .organizationDao()
                    .categories()
                    .map { it.id }
                    .toSet()
            val target = repo.createCategoryWithBooks(name = "Target", bookIds = setOf(a))
            val dao = db.organizationDao()
            dao.insertExclusions(
                listOf(
                    CategoryAutoFillExclusionEntity(sources.first(), excluded),
                    CategoryAutoFillExclusionEntity(sources.last(), a),
                    CategoryAutoFillExclusionEntity(target, b),
                ),
            )

            repo.mergeCategories(sources, target)

            assertEquals(setOf("first", "second"), dao.seriesAliases().map { it.normalizedSeriesName }.toSet())
            assertTrue(dao.seriesAliases().all { it.categoryId == target })
            assertEquals(listOf(CategoryAutoFillExclusionEntity(target, excluded)), dao.exclusions())
            repo.autoFill()
            assertEquals(listOf(target), dao.categories().map { it.id })
            assertEquals(setOf(a, b), dao.members(target).map { it.bookId }.toSet())
        }

    @Test fun databaseFailureDuringBulkDeleteRollsBackEarlierDeletes() =
        runBlocking {
            val first = repo.saveCategory(name = "First")
            val second = repo.saveCategory(name = "Second")
            val before = repo.get()
            failMutation("DELETE", second)
            val failure = runCatching { repo.deleteCategories(linkedSetOf(first, second)) }.exceptionOrNull()
            assertNotNull(failure)
            assertEquals(before, repo.get())
        }

    @Test fun databaseFailureDuringBulkTypeUpdateRollsBackEarlierChanges() =
        runBlocking {
            val first = repo.saveCategory(name = "First")
            val second = repo.saveCategory(name = "Second")
            val before = repo.get()
            failMutation("UPDATE", second)
            val failure =
                runCatching {
                    repo.updateCategoryTypes(linkedSetOf(first, second), setOf(1), add = true)
                }.exceptionOrNull()
            assertNotNull(failure)
            assertEquals(before, repo.get())
        }

    @Test fun databaseFailureDuringBulkMergeRollsBackEarlierSourcesAndTargetChanges() =
        runBlocking {
            val book = book("Merged")
            val first = repo.createCategoryWithBooks(name = "First", typeIds = setOf(1), bookIds = setOf(book))
            val second = repo.saveCategory(name = "Second")
            val target = repo.saveCategory(name = "Target")
            val before = repo.get()
            failMutation("DELETE", second)
            val failure = runCatching { repo.mergeCategories(linkedSetOf(first, second), target) }.exceptionOrNull()
            assertNotNull(failure)
            assertEquals(before, repo.get())
        }

    private suspend fun types(categoryId: Long): Set<Long> =
        db
            .organizationDao()
            .typeLinks()
            .filter { it.categoryId == categoryId }
            .map { it.typeId }
            .toSet()

    private fun failMutation(
        operation: String,
        categoryId: Long,
    ) {
        db.openHelper.writableDatabase.execSQL(
            "CREATE TRIGGER reject_category BEFORE $operation ON categories " +
                "WHEN OLD.id = $categoryId BEGIN SELECT RAISE(ABORT, 'Simulated storage failure'); END",
        )
    }
}
