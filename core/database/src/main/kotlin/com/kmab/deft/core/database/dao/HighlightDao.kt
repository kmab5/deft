package com.kmab.deft.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.kmab.deft.core.database.entity.HighlightEntity
import kotlinx.coroutines.flow.Flow

private const val NOTES_QUERY = """
    SELECT n.id, n.bookId, n.locator, n.totalProgression, n.text, n.note, n.color, n.label,
        n.createdAt, n.updatedAt, COALESCE(b.titleOverride, b.title) AS bookTitle, b.format AS bookFormat,
        b.drmProtected,
        NOT EXISTS (SELECT 1 FROM book_files f WHERE f.bookId = b.id AND f.missing = 0) AS missing,
        COALESCE((SELECT group_concat(name, ', ') FROM (
            SELECT a.name FROM book_authors ba JOIN authors a ON a.id = ba.authorId
            WHERE ba.bookId = b.id AND ba.overridden =
                (SELECT COALESCE(MAX(overridden), 0) FROM book_authors WHERE bookId = b.id)
            ORDER BY ba.position, a.id
        )), '') AS authors
    FROM (
        SELECT id, bookId, locator, totalProgression, text, note, color,
            CAST(NULL AS TEXT) AS label, createdAt, updatedAt
        FROM highlights
        UNION ALL
        SELECT id, bookId, locator, totalProgression, '', NULL, NULL, label, createdAt, createdAt
        FROM bookmarks
    ) n
    JOIN books b ON b.id = n.bookId
    WHERE b.ignored = 0
"""

@Dao
interface HighlightDao {
    @Query(
        """
        SELECT h.* FROM highlights h JOIN books b ON b.id = h.bookId
        WHERE h.bookId = :bookId AND b.ignored = 0
        ORDER BY h.totalProgression, h.createdAt, h.id
        """,
    )
    fun observeForBook(bookId: Long): Flow<List<HighlightEntity>>

    @Query("SELECT * FROM highlights WHERE id = :id")
    suspend fun findById(id: Long): HighlightEntity?

    @Query("SELECT * FROM highlights WHERE bookId = :bookId AND locator = :locator")
    suspend fun findAt(
        bookId: Long,
        locator: String,
    ): HighlightEntity?

    @Insert suspend fun insert(highlight: HighlightEntity): Long

    @Update suspend fun update(highlight: HighlightEntity)

    @Query("DELETE FROM highlights WHERE id = :id")
    suspend fun remove(id: Long)

    @Query(
        NOTES_QUERY + """
        AND n.bookId BETWEEN COALESCE(:bookId, 1) AND COALESCE(:bookId, 9223372036854775807)
        AND (:color IS NULL OR n.color = :color)
        AND (
            :type IS NULL OR
            (:type = 'BOOKMARK' AND n.color IS NULL) OR
            (:type = 'NOTE' AND n.note IS NOT NULL) OR
            (:type = 'HIGHLIGHT' AND n.color <> 'NONE')
        )
        ORDER BY n.updatedAt DESC, n.color IS NULL, n.id DESC
        """,
    )
    fun observeNotes(
        bookId: Long?,
        color: String?,
        type: String?,
    ): Flow<List<NoteRow>>

    @Query("$NOTES_QUERY AND n.bookId = :bookId")
    suspend fun getNotesForBook(bookId: Long): List<NoteRow>
}
