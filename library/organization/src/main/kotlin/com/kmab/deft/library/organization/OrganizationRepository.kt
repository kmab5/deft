package com.kmab.deft.library.organization

import androidx.room.withTransaction
import com.kmab.deft.core.database.AppDatabase
import com.kmab.deft.core.model.CategoryOrder
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
@Suppress("TooManyFunctions") // One transaction boundary owns the organization command interface.
class OrganizationRepository
    @Inject
    constructor(
        private val db: AppDatabase,
    ) {
        private val dao = db.organizationDao()
        private val categories = CategoryOperations(dao)
        private val members = MembershipOperations(dao)
        private val authors = AuthorOperations(dao)

        fun observe(): Flow<OrganizationSnapshot> =
            db.invalidationTracker
                .createFlow(
                    "books",
                    "book_files",
                    "authors",
                    "author_aliases",
                    "book_authors",
                    "categories",
                    "category_types",
                    "category_type_links",
                    "category_members",
                    "category_series_aliases",
                    "category_auto_fill_exclusions",
                    "read_throughs",
                    "reading_sessions",
                    emitInitialState = true,
                ).map { get() }
                .distinctUntilChanged()

        suspend fun get(): OrganizationSnapshot = transaction { OrganizationSnapshots(db).load() }

        @Suppress("LongParameterList") // Complete category edit is atomic, including its chosen types.
        suspend fun saveCategory(
            id: Long? = null,
            name: String,
            description: String? = null,
            typeIds: Set<Long> = emptySet(),
            order: CategoryOrder = CategoryOrder.MANUAL,
            coverPath: String? = null,
        ): Long = transaction { categories.save(id, name, description, typeIds, order, coverPath) }

        suspend fun deleteCategory(id: Long) = transaction { categories.delete(id) }

        @Suppress("LongParameterList") // Category attributes and selected books commit as one user action.
        suspend fun createCategoryWithBooks(
            name: String,
            description: String? = null,
            typeIds: Set<Long> = emptySet(),
            order: CategoryOrder = CategoryOrder.MANUAL,
            coverPath: String? = null,
            bookIds: Set<Long>,
        ): Long =
            transaction {
                val id = categories.save(null, name, description, typeIds, order, coverPath)
                members.add(id, bookIds)
                id
            }

        suspend fun saveType(
            id: Long? = null,
            name: String,
        ): Long = transaction { categories.saveType(id, name) }

        suspend fun deleteType(id: Long) = transaction { categories.deleteType(id) }

        suspend fun addBooks(
            categoryId: Long,
            bookIds: Set<Long>,
        ) = transaction { members.add(categoryId, bookIds) }

        suspend fun removeMember(
            categoryId: Long,
            bookId: Long,
        ) = transaction { members.remove(categoryId, bookId) }

        suspend fun setPosition(
            categoryId: Long,
            bookId: Long,
            position: Double,
        ) = transaction {
            members.position(categoryId, bookId, position)
        }

        suspend fun reorder(
            categoryId: Long,
            visibleBookIds: List<Long>,
        ) = transaction {
            members.reorder(categoryId, visibleBookIds)
        }

        suspend fun confirmMembers(
            categoryId: Long,
            bookIds: Set<Long>,
        ) = transaction { members.confirm(categoryId, bookIds) }

        suspend fun setRating(
            categoryId: Long,
            rating: Double?,
        ) = transaction { categories.rate(categoryId, rating) }

        suspend fun mergeCategories(
            sourceId: Long,
            targetId: Long,
        ) = transaction {
            CategoryMerger(dao).merge(sourceId, targetId)
        }

        suspend fun renameAuthor(
            id: Long,
            name: String,
        ) = transaction { authors.rename(id, name) }

        suspend fun mergeAuthors(
            sourceId: Long,
            targetId: Long,
        ) = transaction { authors.merge(sourceId, targetId) }

        suspend fun autoFill() = transaction { SeriesAutoFill(dao).fill() }

        private suspend fun <T> transaction(block: suspend () -> T): T =
            db.withTransaction {
                dao.ensureSeries()
                block()
            }
    }
