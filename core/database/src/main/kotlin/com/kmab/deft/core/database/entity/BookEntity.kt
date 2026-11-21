package com.kmab.deft.core.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.kmab.deft.core.model.BookFormat
import java.time.Instant

@Entity(tableName = "books", indices = [Index(value = ["contentHash"], unique = true)])
data class BookEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val contentHash: String,
    val format: BookFormat,
    val title: String,
    val sortTitle: String,
    val titleOverride: String? = null,
    val language: String? = null,
    val publisher: String? = null,
    val publishedDate: String? = null,
    val description: String? = null,
    val isbn10: String? = null,
    val isbn13: String? = null,
    val pageCount: Int? = null,
    val coverPath: String? = null,
    val coverOverridePath: String? = null,
    val extractedSeriesName: String? = null,
    val extractedSeriesIndex: Double? = null,
    val drmProtected: Boolean = false,
    val ignored: Boolean = false,
    val metadataError: String? = null,
    val addedAt: Instant,
    val lastOpenedAt: Instant? = null,
    val rating: Double? = null,
    @ColumnInfo(defaultValue = "0") val favorite: Boolean = false,
    val review: String? = null,
    val lastReadAt: Instant? = null,
    val currentLocator: String? = null,
    @ColumnInfo(defaultValue = "0.0") val totalProgression: Double = 0.0,
)
