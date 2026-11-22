package com.kmab.deft.library.organization

import com.kmab.deft.core.database.AppDatabase
import com.kmab.deft.core.database.entity.AuthorEntity
import com.kmab.deft.core.database.entity.BookAuthorEntity
import com.kmab.deft.core.database.entity.CategoryEntity
import com.kmab.deft.core.database.entity.CategoryTypeEntity
import com.kmab.deft.core.model.BookState
import com.kmab.deft.core.model.CategoryOrder
import com.kmab.deft.core.model.ReadThroughOutcome
import java.util.Locale

/** Bounded per-table reads and indexed grouping avoid per-book database queries. */
internal class OrganizationSnapshots(
    private val db: AppDatabase,
) {
    suspend fun load(): OrganizationSnapshot {
        val dao = db.organizationDao()
        val authors = dao.authors().associateBy { it.id }
        val links = dao.effectiveAuthorLinks()
        val byBook = links.groupBy { it.bookId }
        val books = loadBooks(authors, byBook)
        val indexedBooks = books.associateBy { it.book.id }
        val authorSort = byBook.mapValues { (_, values) -> authors[values.first().authorId]?.sortName.orEmpty() }
        val types =
            dao.types().sortedWith(
                compareByDescending<CategoryTypeEntity> { it.builtIn }
                    .thenBy { it.name.lowercase(Locale.ROOT) }
                    .thenBy { it.id },
            )
        return OrganizationSnapshot(
            loadCategories(indexedBooks, authorSort, types),
            authorSummaries(authors, links, indexedBooks),
            types,
            books,
        )
    }

    private suspend fun loadBooks(
        authors: Map<Long, AuthorEntity>,
        byBook: Map<Long, List<BookAuthorEntity>>,
    ): List<OrganizedBook> {
        val dao = db.organizationDao()
        val states = db.trackingDao().latestReadThroughs().associate { it.bookId to it.outcome.state() }
        val seconds = dao.timeTotals().associate { it.bookId to it.activeSeconds }
        return dao
            .books()
            .map { row ->
                val authorIds = byBook[row.book.id].orEmpty().map { it.authorId }
                val names = authorIds.mapNotNull { authors[it]?.name }.joinToString(", ")
                OrganizedBook(
                    row.book.copy(authorsDisplay = names),
                    states[row.book.id] ?: BookState.NOT_STARTED,
                    row.rating,
                    seconds[row.book.id] ?: 0,
                    authorIds,
                )
            }.sortedWith(compareBy<OrganizedBook> { it.book.sortTitle.lowercase(Locale.ROOT) }.thenBy { it.book.id })
    }

    private suspend fun loadCategories(
        indexedBooks: Map<Long, OrganizedBook>,
        authorSort: Map<Long, String>,
        types: List<CategoryTypeEntity>,
    ): List<CategorySummary> {
        val dao = db.organizationDao()
        val indexedTypes = types.associateBy { it.id }
        val categoryTypes = dao.typeLinks().groupBy { it.categoryId }
        val members = dao.members().groupBy { it.categoryId }
        return dao
            .categories()
            .sortedWith(
                compareBy<CategoryEntity> { it.name.lowercase(Locale.ROOT) }.thenBy { it.id },
            ).map { category ->
                val labels =
                    categoryTypes[category.id]
                        .orEmpty()
                        .mapNotNull { indexedTypes[it.typeId] }
                        .sortedWith(compareByDescending<CategoryTypeEntity> { it.builtIn }.thenBy { it.normalizedName })
                val categoryBooks =
                    members[category.id].orEmpty().mapNotNull { member ->
                        indexedBooks[member.bookId]?.let { CategoryBook(it, member) }
                    }
                CategorySummary(
                    category,
                    labels,
                    categoryBooks.sortedWith(
                        memberOrder(
                            category,
                            labels.any {
                                it.id ==
                                    OrganizationNames.SERIES_ID
                            },
                            authorSort,
                        ),
                    ),
                    categoryBooks.map { it.book }.stats(),
                )
            }
    }

    private fun authorSummaries(
        authors: Map<Long, AuthorEntity>,
        links: List<BookAuthorEntity>,
        indexedBooks: Map<Long, OrganizedBook>,
    ): List<AuthorSummary> {
        val authorBooks = links.groupBy { it.authorId }
        return authors.values
            .sortedWith(
                compareBy<AuthorEntity> {
                    it.sortName.lowercase(Locale.ROOT)
                }.thenBy { it.id },
            ).mapNotNull { author ->
                val items =
                    authorBooks[author.id]
                        .orEmpty()
                        .mapNotNull { indexedBooks[it.bookId] }
                        .distinctBy { it.book.id }
                        .sortedWith(
                            compareBy<OrganizedBook> { it.book.sortTitle.lowercase(Locale.ROOT) }.thenBy { it.book.id },
                        )
                items.takeIf { it.isNotEmpty() }?.let { AuthorSummary(author, it, it.stats()) }
            }
    }

    private fun memberOrder(
        category: CategoryEntity,
        series: Boolean,
        authorSort: Map<Long, String>,
    ): Comparator<CategoryBook> {
        val order = if (series) CategoryOrder.MANUAL else category.order
        val primary: Comparator<CategoryBook> =
            when (order) {
                CategoryOrder.MANUAL -> compareBy<CategoryBook> { it.membership.position }
                CategoryOrder.TITLE ->
                    compareBy {
                        it.book.book.sortTitle
                            .lowercase(Locale.ROOT)
                    }
                CategoryOrder.AUTHOR -> compareBy { authorSort[it.book.book.id].orEmpty().lowercase(Locale.ROOT) }
                CategoryOrder.DATE_ADDED -> compareByDescending { it.membership.addedAt }
                CategoryOrder.FILE_LOCATION ->
                    compareBy {
                        it.book.book.fileLocation
                            .lowercase(Locale.ROOT)
                    }
                CategoryOrder.RATING -> compareByDescending { it.book.rating ?: -1.0 }
            }
        return primary
            .thenBy {
                it.book.book.sortTitle
                    .lowercase(Locale.ROOT)
            }.thenBy { it.book.book.id }
    }
}

private fun ReadThroughOutcome.state(): BookState =
    when (this) {
        ReadThroughOutcome.IN_PROGRESS -> BookState.READING
        ReadThroughOutcome.FINISHED -> BookState.FINISHED
        ReadThroughOutcome.PAUSED -> BookState.PAUSED
        ReadThroughOutcome.ABANDONED -> BookState.ABANDONED
    }

private fun List<OrganizedBook>.stats(): OrganizationStats {
    val ratings = mapNotNull { it.rating }
    return OrganizationStats(
        size,
        count { it.state == BookState.FINISHED },
        count { it.state == BookState.READING },
        count { it.state == BookState.PAUSED },
        count { it.state == BookState.ABANDONED },
        sumOf { it.activeSeconds },
        ratings.takeIf { it.isNotEmpty() }?.average(),
    )
}
