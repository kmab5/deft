package com.kmab.deft.feature.organization

import com.kmab.deft.core.datastore.LibrarySettings
import com.kmab.deft.library.organization.AuthorSummary
import com.kmab.deft.library.organization.CategorySummary
import com.kmab.deft.library.organization.OrganizedBook
import java.util.Locale

internal fun browseCategories(
    categories: List<CategorySummary>,
    preferences: LibrarySettings,
    query: String,
): List<CategorySummary> {
    val filtered =
        categories.filter { category ->
            category.category.name.contains(query.trim(), true) && category.matches(preferences)
        }
    val primary: Comparator<CategorySummary> =
        when (preferences.categorySort) {
            "created" -> compareByDescending { it.category.createdAt }
            "book count" -> compareByDescending { it.stats.total }
            "rating" -> compareByDescending { it.rating ?: -1.0 }
            else -> compareBy { it.category.name.normalizedSort() }
        }
    return filtered.sortedWith(
        (if (preferences.categoryReverse) primary.reversed() else primary)
            .thenBy {
                it.category.name.normalizedSort()
            }.thenBy { it.category.id },
    )
}

internal fun browseAuthors(
    authors: List<AuthorSummary>,
    preferences: LibrarySettings,
    query: String,
): List<AuthorSummary> {
    val filtered =
        authors.filter { author ->
            author.author.name.contains(query.trim(), true) &&
                (
                    preferences.authorRating == "all" ||
                        (author.stats.averageRating != null) == (preferences.authorRating == "rated")
                ) &&
                (preferences.authorState == null || author.books.any { it.state == preferences.authorState })
        }
    val primary: Comparator<AuthorSummary> =
        when (preferences.authorSort) {
            "book count" -> compareByDescending { it.stats.total }
            "time" -> compareByDescending { it.stats.activeSeconds }
            "rating" -> compareByDescending { it.stats.averageRating ?: -1.0 }
            else -> compareBy { it.author.sortName.normalizedSort() }
        }
    return filtered.sortedWith(
        (if (preferences.authorReverse) primary.reversed() else primary)
            .thenBy {
                it.author.sortName.normalizedSort()
            }.thenBy { it.author.id },
    )
}

internal fun sortOrganizationBooks(
    books: List<OrganizedBook>,
    sort: String,
    reverse: Boolean,
): List<OrganizedBook> {
    val primary: Comparator<OrganizedBook> =
        when (sort) {
            "author" -> compareBy { it.book.authorSortName.normalizedSort() }
            "date added" -> compareByDescending { it.book.addedAt }
            "rating" -> compareByDescending { it.rating ?: -1.0 }
            "file location" -> compareBy { it.book.fileLocation.normalizedSort() }
            else -> compareBy { it.book.sortTitle.normalizedSort() }
        }
    return books.sortedWith(
        (if (reverse) primary.reversed() else primary)
            .thenBy {
                it.book.sortTitle.normalizedSort()
            }.thenBy { it.book.id },
    )
}

private fun String.normalizedSort(): String = lowercase(Locale.ROOT)

private fun CategorySummary.matches(preferences: LibrarySettings): Boolean {
    val typeMatches =
        when (preferences.categoryTypeFilter) {
            null -> true
            0L -> types.isEmpty()
            else -> types.any { it.id == preferences.categoryTypeFilter }
        }
    val reviewMatches = preferences.categoryReview == "all" || needsReview == (preferences.categoryReview == "review")
    val stateMatches = preferences.categoryState == null || books.any { it.book.state == preferences.categoryState }
    return typeMatches && reviewMatches && stateMatches
}
