package com.kmab.deft.feature.organization

import com.kmab.deft.core.database.entity.AuthorEntity
import com.kmab.deft.core.database.entity.CategoryEntity
import com.kmab.deft.core.database.entity.CategoryMemberEntity
import com.kmab.deft.core.database.model.BookListItem
import com.kmab.deft.core.datastore.LibrarySettings
import com.kmab.deft.core.model.BookFormat
import com.kmab.deft.core.model.BookState
import com.kmab.deft.library.organization.AuthorSummary
import com.kmab.deft.library.organization.CategoryBook
import com.kmab.deft.library.organization.CategorySummary
import com.kmab.deft.library.organization.OrganizationStats
import com.kmab.deft.library.organization.OrganizedBook
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.Instant

class OrganizationBrowseTest {
    @Test fun categoryReviewStateAndCreatedSortComposeWithoutDroppingStableIdentity() {
        val older = category(1, "Alpha", 10, true, BookState.READING)
        val newer = category(2, "Beta", 20, true, BookState.READING)
        val reviewed = category(3, "Gamma", 30, false, BookState.FINISHED)
        val prefs =
            LibrarySettings(categorySort = "created", categoryReview = "review", categoryState = BookState.READING)
        assertEquals(listOf(2L, 1L), browseCategories(listOf(older, reviewed, newer), prefs, "").map { it.category.id })
        assertEquals(
            listOf(
                1L,
                2L,
            ),
            browseCategories(listOf(older, newer), prefs.copy(categoryReverse = true), "").map {
                it.category.id
            },
        )
        assertEquals(
            listOf(
                3L,
            ),
            browseCategories(
                listOf(older, reviewed),
                prefs.copy(categoryReview = "reviewed", categoryState = null),
                "gam",
            ).map {
                it.category.id
            },
        )
    }

    @Test fun authorFiltersAndTimeSortingUseWholeAuthorSummary() {
        val first = author(1, "Zulu", 200, null, BookState.READING)
        val second = author(2, "Alpha", 500, 4.0, BookState.FINISHED)
        val prefs = LibrarySettings(authorSort = "time", authorRating = "rated", authorState = BookState.FINISHED)
        assertEquals(listOf(2L), browseAuthors(listOf(first, second), prefs, "").map { it.author.id })
        assertEquals(
            listOf(
                2L,
                1L,
            ),
            browseAuthors(listOf(first, second), prefs.copy(authorRating = "all", authorState = null), "").map {
                it.author.id
            },
        )
    }

    @Test fun bookSortingSupportsAuthorAndLocationWithStableTies() {
        val first =
            book(1, BookState.READING).let { entry ->
                entry.copy(
                    book =
                        entry.book.copy(
                            authorsDisplay = "Amy Zulu",
                            authorSortName = "Zulu Amy",
                            fileLocation = "A/one.epub",
                        ),
                )
            }
        val second =
            book(2, BookState.FINISHED).let { entry ->
                entry.copy(
                    book =
                        entry.book.copy(
                            authorsDisplay = "Zoe Alpha",
                            authorSortName = "Alpha Zoe",
                            fileLocation = "Z/two.epub",
                        ),
                )
            }
        assertEquals(listOf(2L, 1L), sortOrganizationBooks(listOf(first, second), "author", false).map { it.book.id })
        assertEquals(
            listOf(1L, 2L),
            sortOrganizationBooks(listOf(second, first), "file location", false).map { it.book.id },
        )
    }

    @Test fun coauthorDisplayDoesNotChangePrimaryAuthorOrderOrTitleTies() {
        val first =
            book(1, BookState.READING).let {
                it.copy(
                    book =
                        it.book.copy(
                            title = "Alpha",
                            sortTitle = "Alpha",
                            authorsDisplay = "Zoe Alpha, Zelda Last",
                            authorSortName = "Alpha Zoe",
                        ),
                )
            }
        val second =
            book(2, BookState.READING).let {
                it.copy(
                    book =
                        it.book.copy(
                            title = "Beta",
                            sortTitle = "Beta",
                            authorsDisplay = "Zoe Alpha, Amy First",
                            authorSortName = "Alpha Zoe",
                        ),
                )
            }
        val unknown =
            book(3, BookState.READING).let {
                it.copy(book = it.book.copy(authorsDisplay = "Zzz display only", authorSortName = ""))
            }
        assertEquals(listOf(1L, 2L), sortOrganizationBooks(listOf(second, first), "author", false).map { it.book.id })
        assertEquals(listOf(1L, 2L), sortOrganizationBooks(listOf(second, first), "author", true).map { it.book.id })
        assertEquals(listOf(3L, 1L), sortOrganizationBooks(listOf(first, unknown), "author", false).map { it.book.id })
    }

    private fun category(
        id: Long,
        name: String,
        created: Long,
        suggested: Boolean,
        state: BookState,
    ): CategorySummary {
        val book = book(id, state)
        return CategorySummary(
            CategoryEntity(id, name, createdAt = Instant.ofEpochSecond(created)),
            emptyList(),
            listOf(CategoryBook(book, CategoryMemberEntity(id, id, 1.0, suggested, Instant.EPOCH))),
            OrganizationStats(total = 1),
        )
    }

    private fun author(
        id: Long,
        name: String,
        seconds: Long,
        rating: Double?,
        state: BookState,
    ) = AuthorSummary(
        AuthorEntity(id, name, name, name.lowercase()),
        listOf(book(id, state)),
        OrganizationStats(total = 1, activeSeconds = seconds, averageRating = rating),
    )

    private fun book(
        id: Long,
        state: BookState,
    ) = OrganizedBook(
        BookListItem(id, "Book $id", "Book $id", "", BookFormat.EPUB, null, Instant.EPOCH, false, false),
        state,
    )
}
