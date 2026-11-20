package com.kmab.deft.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.kmab.deft.core.database.entity.BookmarkEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface BookmarkDao {
    @Query("SELECT * FROM bookmarks WHERE bookId = :bookId ORDER BY totalProgression, createdAt, id")
    fun observeForBook(bookId: Long): Flow<List<BookmarkEntity>>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(bookmark: BookmarkEntity): Long

    @Query("SELECT id FROM bookmarks WHERE bookId = :bookId AND locator = :locator")
    suspend fun findId(
        bookId: Long,
        locator: String,
    ): Long?

    @Query("UPDATE bookmarks SET label = :label WHERE id = :id")
    suspend fun rename(
        id: Long,
        label: String,
    )

    @Query("DELETE FROM bookmarks WHERE id = :id")
    suspend fun remove(id: Long)
}
