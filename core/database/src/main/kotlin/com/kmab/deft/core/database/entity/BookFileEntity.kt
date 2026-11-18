package com.kmab.deft.core.database.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.time.Instant

@Entity(
    tableName = "book_files",
    foreignKeys = [
        ForeignKey(
            entity = BookEntity::class,
            parentColumns = ["id"],
            childColumns = ["bookId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("bookId"), Index("rootUri"), Index(value = ["uri"], unique = true)],
)
data class BookFileEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val bookId: Long,
    val rootUri: String,
    val uri: String,
    val displayPath: String,
    val size: Long,
    val lastModified: Long,
    val missing: Boolean,
    val lastSeenAt: Instant,
)
