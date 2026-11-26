package com.kmab.deft.library.notes

import com.kmab.deft.core.model.BookFormat
import com.kmab.deft.core.model.HighlightColor
import java.time.Instant

data class HighlightDraft(
    val bookId: Long,
    val locator: String,
    val text: String,
    val color: HighlightColor = HighlightColor.NONE,
    val note: String? = null,
    val totalProgression: Double = 0.0,
    val id: Long? = null,
    val underline: Boolean = false,
)

enum class NoteType { HIGHLIGHT, NOTE, BOOKMARK }

data class NoteFilter(
    val bookId: Long? = null,
    val color: HighlightColor? = null,
    val type: NoteType? = null,
    val query: String = "",
)

data class NoteEntry(
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
    val type: NoteType,
    val underline: Boolean = false,
)
