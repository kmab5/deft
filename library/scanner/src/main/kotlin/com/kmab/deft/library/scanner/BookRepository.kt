package com.kmab.deft.library.scanner

import com.kmab.deft.core.database.AppDatabase
import com.kmab.deft.core.model.BookFormat
import com.kmab.deft.core.model.BookSort

class BookRepository
    @javax.inject.Inject
    constructor(
        private val db: AppDatabase,
    ) {
        fun observeBooks(
            includeMissing: Boolean,
            formats: Set<BookFormat>,
            sort: BookSort,
            reverseSort: Boolean = false,
        ) = db.bookDao().observeList(includeMissing, formats, sort, reverseSort)
    }
