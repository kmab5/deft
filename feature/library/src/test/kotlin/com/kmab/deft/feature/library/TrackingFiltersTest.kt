package com.kmab.deft.feature.library

import com.kmab.deft.core.database.model.BookListItem
import com.kmab.deft.core.datastore.LibrarySettings
import com.kmab.deft.core.model.BookAssessment
import com.kmab.deft.core.model.BookFormat
import com.kmab.deft.core.model.BookSort
import com.kmab.deft.core.model.BookState
import com.kmab.deft.core.model.BookTrackingSummary
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.Instant

class TrackingFiltersTest {
    private fun book(id: Long) =
        BookListItem(id, "book $id", "book $id", "author", BookFormat.EPUB, null, Instant.EPOCH, false, false)

    private val books = (1L..4).map(::book)
    private val history =
        mapOf(
            1L to BookTrackingSummary(1, BookState.FINISHED, BookAssessment(4.5, true)),
            2L to BookTrackingSummary(2, BookState.PAUSED, BookAssessment(2.5)),
            3L to BookTrackingSummary(3, BookState.READING, BookAssessment(5.0, true)),
        )

    @Test fun stateFavoriteAndRatingFiltersCompose() {
        val preferences =
            LibrarySettings(
                stateFilter = setOf(BookState.FINISHED, BookState.READING),
                favoritesOnly = true,
                minimumRating = 5.0,
            )
        assertEquals(listOf(3L), filterTrackedBooks(books, history, preferences).map { it.id })
        assertEquals(
            listOf(
                4L,
            ),
            filterTrackedBooks(books, history, LibrarySettings(stateFilter = setOf(BookState.NOT_STARTED))).map {
                it.id
            },
        )
        assertEquals(
            emptyList<Long>(),
            filterTrackedBooks(
                books,
                history,
                LibrarySettings(stateFilter = emptySet()),
            ).map {
                it.id
            },
        )
    }

    @Test fun ratingSortAlwaysKeepsUnratedLastInBothDirections() {
        assertEquals(
            listOf(
                3L,
                1L,
                2L,
                4L,
            ),
            filterTrackedBooks(books, history, LibrarySettings(sort = BookSort.RATING)).map {
                it.id
            },
        )
        assertEquals(
            listOf(
                2L,
                1L,
                3L,
                4L,
            ),
            filterTrackedBooks(books, history, LibrarySettings(sort = BookSort.RATING, reverseSort = true)).map {
                it.id
            },
        )
    }
}
