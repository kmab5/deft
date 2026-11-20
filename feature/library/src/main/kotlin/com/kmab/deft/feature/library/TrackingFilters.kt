package com.kmab.deft.feature.library

import com.kmab.deft.core.database.model.BookListItem
import com.kmab.deft.core.datastore.LibrarySettings
import com.kmab.deft.core.designsystem.DeftReadingState
import com.kmab.deft.core.model.BookSort
import com.kmab.deft.core.model.BookState
import com.kmab.deft.core.model.BookTrackingSummary

internal fun filterTrackedBooks(
    books: List<BookListItem>,
    history: Map<Long, BookTrackingSummary>,
    preferences: LibrarySettings,
): List<BookListItem> {
    val minimum = preferences.minimumRating
    val filtered =
        books.filter { book ->
            val snapshot = history[book.id]
            (snapshot?.state ?: BookState.NOT_STARTED) in preferences.stateFilter &&
                (!preferences.favoritesOnly || snapshot?.assessment?.favorite == true) &&
                (minimum == null || (snapshot?.assessment?.rating ?: 0.0) >= minimum)
        }
    if (preferences.sort != BookSort.RATING) return filtered
    return filtered.sortedWith(
        compareBy<BookListItem> { history[it.id]?.assessment?.rating == null }
            .thenBy { (history[it.id]?.assessment?.rating ?: 0.0) * if (preferences.reverseSort) 1 else -1 }
            .thenBy { it.sortTitle }
            .thenBy { it.id },
    )
}

internal fun BookState.appearance(): DeftReadingState =
    when (this) {
        BookState.NOT_STARTED -> DeftReadingState.NotRead
        BookState.READING -> DeftReadingState.Reading
        BookState.FINISHED -> DeftReadingState.Finished
        BookState.PAUSED -> DeftReadingState.Paused
        BookState.ABANDONED -> DeftReadingState.Abandoned
    }
