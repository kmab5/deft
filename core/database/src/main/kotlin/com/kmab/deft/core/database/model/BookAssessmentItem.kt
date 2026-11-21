package com.kmab.deft.core.database.model

/** Library projection avoids loading descriptions, locators and session history. */
data class BookAssessmentItem(
    val bookId: Long,
    val rating: Double?,
    val favorite: Boolean,
    val review: String?,
)
