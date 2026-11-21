package com.kmab.deft.core.database.model

import androidx.room.Embedded

/** Bounded organization projection: excludes book descriptions, reviews and reading locators. */
data class OrganizationBookRow(
    @Embedded val book: BookListItem,
    val rating: Double?,
    val extractedSeriesName: String?,
    val extractedSeriesIndex: Double?,
)

data class OrganizationTimeTotal(
    val bookId: Long,
    val activeSeconds: Long,
)
