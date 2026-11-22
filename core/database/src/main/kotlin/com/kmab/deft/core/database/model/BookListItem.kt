package com.kmab.deft.core.database.model

import com.kmab.deft.core.model.BookFormat
import java.time.Instant

data class BookListItem(
    val id: Long,
    val title: String,
    val sortTitle: String,
    val authorsDisplay: String,
    val format: BookFormat,
    val coverPath: String?,
    val addedAt: Instant,
    val missing: Boolean,
    val drmProtected: Boolean,
    val lastReadAt: Instant? = null,
    val totalProgression: Double = 0.0,
    val fileLocation: String = "",
    val authorSortName: String = "",
)
