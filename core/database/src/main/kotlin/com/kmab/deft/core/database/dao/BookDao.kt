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
    ): Flow<List<BookListItem>> = observeFiltered(includeMissing, formats.toList(), formats.isEmpty(), sort.name)

    @Query(
        """
 SELECT b.id, COALESCE(b.titleOverride, b.title) AS title, b.sortTitle,
 COALESCE((SELECT group_concat(name, ', ') FROM (SELECT a.name FROM book_authors ba JOIN authors a ON a.id = ba.authorId WHERE ba.bookId = b.id AND ba.overridden = (SELECT COALESCE(MAX(overridden), 0) FROM book_authors WHERE bookId = b.id) ORDER BY ba.position, a.id)), '') AS authorsDisplay,
 b.format, COALESCE(b.coverOverridePath, b.coverPath) AS coverPath, b.addedAt,
 NOT EXISTS (SELECT 1 FROM book_files f WHERE f.bookId = b.id AND f.missing = 0) AS missing, b.drmProtected
 FROM books b WHERE b.ignored = 0 AND (:allFormats OR b.format IN (:formats))
 AND (:includeMissing OR EXISTS (SELECT 1 FROM book_files f WHERE f.bookId = b.id AND f.missing = 0))
 ORDER BY CASE WHEN :sort = 'DATE_ADDED' THEN b.addedAt END DESC,
 CASE WHEN :sort = 'AUTHOR' THEN (SELECT a.sortName FROM book_authors ba JOIN authors a ON a.id = ba.authorId WHERE ba.bookId = b.id AND ba.overridden = (SELECT COALESCE(MAX(overridden), 0) FROM book_authors WHERE bookId = b.id) ORDER BY ba.position, a.id LIMIT 1) END COLLATE NOCASE,
 b.sortTitle COLLATE NOCASE, b.id
 """,
    )
    protected abstract fun observeFiltered(
        includeMissing: Boolean,
        formats: List<BookFormat>,
        allFormats: Boolean,
        sort: String,
    ): Flow<List<BookListItem>>
}
