package com.kmab.deft.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.kmab.deft.core.database.entity.BookFileEntity
import kotlinx.coroutines.flow.Flow

@Dao
abstract class BookFileDao {
    @Query("SELECT * FROM book_files")
    abstract suspend fun getAll(): List<BookFileEntity>

    @Query("SELECT * FROM book_files WHERE uri = :uri")
    abstract suspend fun findByUri(uri: String): BookFileEntity?

    @Insert abstract suspend fun insert(file: BookFileEntity): Long

    @Update abstract suspend fun update(file: BookFileEntity)

    @Query("DELETE FROM book_files WHERE id = :id")
    abstract suspend fun deleteById(id: Long)

    @Query("SELECT * FROM book_files WHERE bookId = :bookId ORDER BY id")
    abstract fun observeForBook(bookId: Long): Flow<List<BookFileEntity>>

    @Query("UPDATE book_files SET missing = 1 WHERE rootUri = :rootUri")
    abstract suspend fun markMissingForRoot(rootUri: String)

    @Query("UPDATE book_files SET missing = 0 WHERE rootUri = :rootUri AND uri IN (:uris)")
    protected abstract suspend fun restoreSeen(
        rootUri: String,
        uris: List<String>,
    )

    /** Call only after every requested root was completely enumerated. Both phases are atomic. */
    @Transaction
    open suspend fun markMissingUnderRootsExcept(
        rootUris: List<String>,
        seenUris: Set<String>,
    ) {
        rootUris.forEach { markMissingForRoot(it) }
        val chunks = seenUris.toList().chunked(SEEN_URI_CHUNK_SIZE)
        rootUris.forEach { root -> chunks.forEach { restoreSeen(root, it) } }
    }

    @Query(
        """
        DELETE FROM book_files WHERE missing = 1 AND EXISTS (
            SELECT 1 FROM book_files present
            WHERE present.bookId = book_files.bookId AND present.missing = 0
        )
        """,
    )
    abstract suspend fun pruneMissingWherePresentCopyExists()

    private companion object {
        // Leave room for rootUri below SQLite's historical 999 bound-variable limit.
        const val SEEN_URI_CHUNK_SIZE = 900
    }
}
