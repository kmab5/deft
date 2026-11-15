package com.kmab.deft.core.datastore

import com.kmab.deft.core.model.BookFormat
import com.kmab.deft.core.model.BookSort
import java.time.Instant

data class LibrarySettings(
    val enabledFormats: Set<BookFormat> = BookFormat.entries.toSet(),
    val showMissing: Boolean = false,
    val sort: BookSort = BookSort.TITLE,
    val gridView: Boolean = true,
    val lastScanAt: Instant? = null,
)
