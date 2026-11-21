package com.kmab.deft.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import androidx.room.Upsert
import com.kmab.deft.core.database.entity.ReadThroughEntity
import com.kmab.deft.core.database.entity.ReadingSessionEntity
import com.kmab.deft.core.database.model.BookAssessmentItem

/** Storage primitives; callers use transactional tracking repositories for state changes. */
@Dao
@Suppress("TooManyFunctions")
interface TrackingDao {
    @Query("SELECT * FROM read_throughs WHERE bookId = :bookId ORDER BY id DESC")
    suspend fun getReadThroughs(bookId: Long): List<ReadThroughEntity>

    @Query("SELECT * FROM reading_sessions WHERE bookId = :bookId ORDER BY startedAt DESC, id")
    suspend fun getSessions(bookId: Long): List<ReadingSessionEntity>

    @Query("SELECT id AS bookId, rating, favorite, review FROM books")
    suspend fun bookAssessments(): List<BookAssessmentItem>

    @Query("SELECT * FROM read_throughs WHERE id IN (SELECT MAX(id) FROM read_throughs GROUP BY bookId)")
    suspend fun latestReadThroughs(): List<ReadThroughEntity>

    @Query("SELECT * FROM reading_sessions ORDER BY startedAt DESC, id")
    suspend fun allSessions(): List<ReadingSessionEntity>

    @Query("SELECT * FROM read_throughs WHERE id = :id")
    suspend fun readThrough(id: Long): ReadThroughEntity?

    @Query("SELECT * FROM reading_sessions WHERE id = :id")
    suspend fun session(id: String): ReadingSessionEntity?

    @Insert suspend fun insertReadThrough(value: ReadThroughEntity): Long

    @Update suspend fun updateReadThrough(value: ReadThroughEntity)

    @Upsert suspend fun upsertSession(value: ReadingSessionEntity)

    @Query("DELETE FROM read_throughs WHERE id = :id")
    suspend fun deleteReadThrough(id: Long)

    @Query("DELETE FROM read_throughs WHERE bookId = :bookId")
    suspend fun clearReadThroughs(bookId: Long)

    @Query("DELETE FROM reading_sessions WHERE bookId = :bookId")
    suspend fun clearSessions(bookId: Long)

    @Query("DELETE FROM reading_sessions WHERE id = :id")
    suspend fun deleteSession(id: String)

    @Query(
        "UPDATE read_throughs SET finishPromptShown = 1 WHERE id = :id AND " +
            "finishPromptShown = 0 AND outcome = 'IN_PROGRESS'",
    )
    suspend fun claimFinishPrompt(id: Long): Int

    @Query("DELETE FROM reading_sessions WHERE finalized = 0 AND activeSeconds < minimumSessionSeconds")
    suspend fun discardInterruptedShortSessions()

    @Query("UPDATE reading_sessions SET finalized = 1 WHERE finalized = 0 AND activeSeconds >= minimumSessionSeconds")
    suspend fun finalizeInterruptedSessions()

    @Query(
        "SELECT COALESCE(SUM(s.activeSeconds), 0) FROM reading_sessions s JOIN books b ON " +
            "b.id = s.bookId WHERE s.browsing = 0 AND s.activeSeconds >= s.minimumSessionSeconds AND b.ignored = 0",
    )
    suspend fun aggregateSeconds(): Long

    @Query("UPDATE books SET rating = :rating, favorite = :favorite, review = :review WHERE id = :bookId")
    suspend fun updateAssessment(
        bookId: Long,
        rating: Double?,
        favorite: Boolean,
        review: String?,
    )
}
