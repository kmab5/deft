package com.kmab.deft.core.database.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.time.Instant

@Entity(
    tableName = "reading_sessions",
    foreignKeys = [
        ForeignKey(
            entity = BookEntity::class,
            parentColumns = ["id"],
            childColumns = ["bookId"],
            onDelete = ForeignKey.CASCADE,
        ),
        ForeignKey(
            entity = ReadThroughEntity::class,
            parentColumns = ["id"],
            childColumns = ["readThroughId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("bookId"), Index("readThroughId")],
)
data class ReadingSessionEntity(
    @PrimaryKey val id: String,
    val bookId: Long,
    val readThroughId: Long?,
    val startedAt: Instant,
    val endedAt: Instant,
    val activeSeconds: Long,
    val startLocator: String?,
    val endLocator: String?,
    val startProgression: Double,
    val endProgression: Double,
    val pagesTurned: Int,
    val startPosition: Int?,
    val endPosition: Int?,
    val browsing: Boolean,
    val zoneOffsetSeconds: Int,
    val finalized: Boolean,
)
