package com.kmab.deft.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.kmab.deft.core.database.entity.AuthorAliasEntity
import com.kmab.deft.core.database.entity.AuthorEntity
import com.kmab.deft.core.database.entity.BookAuthorEntity
import com.kmab.deft.core.model.AuthorNames

@Dao
abstract class AuthorDao {
    @Query(
        """
        SELECT authorId FROM author_aliases WHERE aliasNormalizedName = :key
        UNION ALL SELECT id FROM authors WHERE normalizedName = :key LIMIT 1
        """,
    )
    protected abstract suspend fun resolve(key: String): Long?

    @Insert protected abstract suspend fun insert(author: AuthorEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun addAlias(alias: AuthorAliasEntity)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    protected abstract suspend fun link(author: BookAuthorEntity)

    @Query("DELETE FROM book_authors WHERE bookId = :bookId AND overridden = 0")
    protected abstract suspend fun clearExtracted(bookId: Long)

    @Transaction open suspend fun getOrCreate(rawName: String): Long {
        val key = AuthorNames.normalizedKey(rawName)
        return resolve(key)
            ?: insert(
                AuthorEntity(
                    name = AuthorNames.display(rawName),
                    sortName = AuthorNames.sortName(AuthorNames.display(rawName)),
                    normalizedName = key,
                ),
            )
    }

    @Transaction open suspend fun setBookAuthors(
        bookId: Long,
        rawNames: List<String>,
    ) {
        clearExtracted(bookId)
        rawNames.map { getOrCreate(it) }.distinct().forEachIndexed {
            position,
            id,
            ->
            link(BookAuthorEntity(bookId, id, position, false))
        }
    }
}
