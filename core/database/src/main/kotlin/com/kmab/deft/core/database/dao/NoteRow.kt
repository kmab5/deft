package com.kmab.deft.core.database.dao

import com.kmab.deft.core.model.BookFormat
import com.kmab.deft.core.model.HighlightColor
import java.time.Instant

data class NoteRow(
    val id: Long,
    val bookId: Long,
    val bookTitle: String,
    val authors: String,
    val bookFormat: BookFormat,
    val missing: Boolean,
    val drmProtected: Boolean,
    val locator: String,
    val totalProgression: Double,
    val text: String,
    val note: String?,
    val color: HighlightColor?,
    val label: String?,
    val createdAt: Instant,
    val updatedAt: Instant,
)
