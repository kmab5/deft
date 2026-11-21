package com.kmab.deft.core.database.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.kmab.deft.core.model.ReadThroughOutcome
import java.time.Instant

@Entity(
    tableName = "read_throughs",
    foreignKeys = [
        ForeignKey(
            entity = BookEntity::class,
            parentColumns = ["id"],
            childColumns = ["bookId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("bookId")],
)
data class ReadThroughEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val bookId: Long,
    val startedAt: Instant,
    val endedAt: Instant? = null,
    val outcome: ReadThroughOutcome = ReadThroughOutcome.IN_PROGRESS,
    val finishPromptShown: Boolean = false,
)
