package com.kmab.deft.core.database.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.kmab.deft.core.model.HighlightColor
import java.time.Instant

@Entity(
    tableName = "highlights",
    foreignKeys = [
        ForeignKey(
            entity = BookEntity::class,
            parentColumns = ["id"],
            childColumns = ["bookId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("bookId"), Index(value = ["bookId", "locator"], unique = true)],
)
data class HighlightEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val bookId: Long,
    val locator: String,
    val text: String,
    val color: HighlightColor,
    val note: String?,
    val totalProgression: Double,
    val createdAt: Instant,
    val updatedAt: Instant,
    @androidx.room.ColumnInfo(defaultValue = "0") val underline: Boolean = false,
)
