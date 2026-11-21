package com.kmab.deft.library.organization

import com.kmab.deft.core.database.entity.AuthorEntity
import com.kmab.deft.core.database.entity.CategoryEntity
import com.kmab.deft.core.database.entity.CategoryMemberEntity
import com.kmab.deft.core.database.entity.CategoryTypeEntity
import com.kmab.deft.core.database.model.BookListItem
import com.kmab.deft.core.model.BookState
import java.util.Locale

data class OrganizedBook(
    val book: BookListItem,
    val state: BookState = BookState.NOT_STARTED,
    val rating: Double? = null,
    val activeSeconds: Long = 0,
    val authorIds: List<Long> = emptyList(),
)

data class OrganizationStats(
    val total: Int = 0,
    val finished: Int = 0,
    val reading: Int = 0,
    val paused: Int = 0,
    val abandoned: Int = 0,
    val activeSeconds: Long = 0,
    val averageRating: Double? = null,
)

data class CategoryBook(
    val book: OrganizedBook,
    val membership: CategoryMemberEntity,
)

data class CategorySummary(
    val category: CategoryEntity,
    val types: List<CategoryTypeEntity>,
    val books: List<CategoryBook>,
    val stats: OrganizationStats,
) {
    val isSeries: Boolean get() = types.any { it.id == 1L }
    val needsReview: Boolean get() = books.any { it.membership.suggested }
    val rating: Double? get() = category.ratingOverride ?: stats.averageRating
}

data class AuthorSummary(
    val author: AuthorEntity,
    val books: List<OrganizedBook>,
    val stats: OrganizationStats,
)

data class SeriesNext(
    val categoryId: Long,
    val categoryName: String,
    val book: OrganizedBook,
    val position: Double,
)

data class OrganizationSnapshot(
    val categories: List<CategorySummary> = emptyList(),
    val authors: List<AuthorSummary> = emptyList(),
    val types: List<CategoryTypeEntity> = emptyList(),
    val books: List<OrganizedBook> = emptyList(),
) {
    fun nextInSeries(bookId: Long): List<SeriesNext> =
        categories.filter { it.isSeries }.mapNotNull { category ->
            val current = category.books.find { it.book.book.id == bookId } ?: return@mapNotNull null
            category.books
                .filter {
                    it.membership.position > current.membership.position &&
                        it.book.state != BookState.FINISHED
                }.minWithOrNull(
                    compareBy<CategoryBook> { it.membership.position }
                        .thenBy {
                            it.book.book.sortTitle
                                .lowercase(Locale.ROOT)
                        }.thenBy { it.book.book.id },
                )?.let { SeriesNext(category.category.id, category.category.name, it.book, it.membership.position) }
        }
}
