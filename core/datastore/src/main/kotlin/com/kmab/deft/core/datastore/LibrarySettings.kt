package com.kmab.deft.core.datastore

import com.kmab.deft.core.model.BookDisplaySize
import com.kmab.deft.core.model.BookFormat
import com.kmab.deft.core.model.BookSort
import com.kmab.deft.core.model.BookState
import java.time.Instant

data class LibrarySettings(
    val enabledFormats: Set<BookFormat> = BookFormat.entries.toSet(),
    val showMissing: Boolean = false,
    val sort: BookSort = BookSort.TITLE,
    val gridView: Boolean = true,
    val lastScanAt: Instant? = null,
    val reverseSort: Boolean = false,
    val gridSize: BookDisplaySize = BookDisplaySize.MEDIUM,
    val listSize: BookDisplaySize = BookDisplaySize.MEDIUM,
    val formatFilter: Set<BookFormat> = BookFormat.entries.toSet(),
    val advancedSearch: Boolean = false,
    val stateFilter: Set<BookState> = BookState.entries.toSet(),
    val favoritesOnly: Boolean = false,
    val minimumRating: Double? = null,
    val seriesAutoFill: Boolean = true,
    val categoryFilter: Long? = null,
    val categoryTypeFilter: Long? = null,
    val groupCategoriesByType: Boolean = false,
    val categoryGridView: Boolean = false,
)
