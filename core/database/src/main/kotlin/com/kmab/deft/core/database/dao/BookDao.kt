package com.kmab.deft.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.kmab.deft.core.database.entity.BookEntity
import com.kmab.deft.core.database.model.BookListItem
import com.kmab.deft.core.model.BookFormat
import com.kmab.deft.core.model.BookSort
import kotlinx.coroutines.flow.Flow

@Dao
abstract class BookDao {
    @Query("SELECT * FROM books WHERE contentHash = :hash")
    abstract suspend fun findByHash(hash: String): BookEntity?

    @Insert abstract suspend fun insert(book: BookEntity): Long

    @Update abstract suspend fun update(book: BookEntity)

    @Query("SELECT NOT EXISTS (SELECT 1 FROM book_files WHERE bookId = :bookId AND missing = 0)")
    abstract suspend fun isMissing(bookId: Long): Boolean

    fun observeList(
        includeMissing: Boolean,
        formats: Set<BookFormat>,
        sort: BookSort,
        reverseSort: Boolean = false,
    ): Flow<List<BookListItem>> =
        observeFiltered(includeMissing, formats.toList(), formats.isEmpty(), sort.name, reverseSort)

    @Query(
        """
 SELECT b.id, COALESCE(b.titleOverride, b.title) AS title, b.sortTitle,
 COALESCE((SELECT group_concat(name, ', ') FROM (SELECT a.name FROM book_authors ba JOIN authors a ON a.id = ba.authorId WHERE ba.bookId = b.id AND ba.overridden = (SELECT COALESCE(MAX(overridden), 0) FROM book_authors WHERE bookId = b.id) ORDER BY ba.position, a.id)), '') AS authorsDisplay,
 b.format, COALESCE(b.coverOverridePath, b.coverPath) AS coverPath, b.addedAt, b.lastReadAt,
 NOT EXISTS (SELECT 1 FROM book_files f WHERE f.bookId = b.id AND f.missing = 0) AS missing, b.drmProtected
 FROM books b WHERE b.ignored = 0 AND (:allFormats OR b.format IN (:formats))
 AND (:includeMissing OR EXISTS (SELECT 1 FROM book_files f WHERE f.bookId = b.id AND f.missing = 0))
 ORDER BY CASE WHEN :sort = 'LAST_READ' AND b.lastReadAt IS NULL THEN 1 ELSE 0 END,
 CASE WHEN :sort = 'LAST_READ' AND NOT :reverseSort THEN b.lastReadAt END DESC,
 CASE WHEN :sort = 'LAST_READ' AND :reverseSort THEN b.lastReadAt END ASC,
 CASE WHEN :sort = 'DATE_ADDED' AND NOT :reverseSort THEN b.addedAt END DESC,
 CASE WHEN :sort = 'DATE_ADDED' AND :reverseSort THEN b.addedAt END ASC,
 CASE WHEN :sort = 'AUTHOR' AND NOT :reverseSort THEN (SELECT a.sortName FROM book_authors ba JOIN authors a ON a.id = ba.authorId WHERE ba.bookId = b.id AND ba.overridden = (SELECT COALESCE(MAX(overridden), 0) FROM book_authors WHERE bookId = b.id) ORDER BY ba.position, a.id LIMIT 1) END COLLATE NOCASE ASC,
 CASE WHEN :sort = 'AUTHOR' AND :reverseSort THEN (SELECT a.sortName FROM book_authors ba JOIN authors a ON a.id = ba.authorId WHERE ba.bookId = b.id AND ba.overridden = (SELECT COALESCE(MAX(overridden), 0) FROM book_authors WHERE bookId = b.id) ORDER BY ba.position, a.id LIMIT 1) END COLLATE NOCASE DESC,
 CASE WHEN :sort = 'TITLE' AND :reverseSort THEN b.sortTitle END COLLATE NOCASE DESC,
 b.sortTitle COLLATE NOCASE ASC, b.id ASC
 """,
    )
    protected abstract fun observeFiltered(
        includeMissing: Boolean,
        formats: List<BookFormat>,
        allFormats: Boolean,
        sort: String,
        reverseSort: Boolean,
    ): Flow<List<BookListItem>>
}
