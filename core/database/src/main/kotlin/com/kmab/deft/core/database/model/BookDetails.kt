package com.kmab.deft.core.database.model

import com.kmab.deft.core.database.entity.BookEntity
import com.kmab.deft.core.database.entity.BookFileEntity

data class BookDetails(
    val book: BookEntity,
    val authorsDisplay: String,
    val files: List<BookFileEntity>,
)
