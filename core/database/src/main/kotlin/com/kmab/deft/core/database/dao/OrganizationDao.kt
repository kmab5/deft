package com.kmab.deft.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import androidx.room.Upsert
import com.kmab.deft.core.database.entity.AuthorAliasEntity
import com.kmab.deft.core.database.entity.AuthorEntity
import com.kmab.deft.core.database.entity.BookAuthorEntity
import com.kmab.deft.core.database.entity.CategoryAutoFillExclusionEntity
import com.kmab.deft.core.database.entity.CategoryEntity
import com.kmab.deft.core.database.entity.CategoryMemberEntity
import com.kmab.deft.core.database.entity.CategorySeriesAliasEntity
import com.kmab.deft.core.database.entity.CategoryTypeEntity
import com.kmab.deft.core.database.entity.CategoryTypeLinkEntity
import com.kmab.deft.core.database.model.OrganizationBookRow
import com.kmab.deft.core.database.model.OrganizationTimeTotal

/** Storage primitives; organization repositories provide validation and transaction boundaries. */
@Dao
@Suppress("TooManyFunctions")
interface OrganizationDao {
    @Query("INSERT OR IGNORE INTO category_types (id, name, normalizedName, builtIn) VALUES (1, 'Series', 'series', 1)")
    suspend fun ensureSeries()

    @Query("SELECT * FROM categories")
    suspend fun categories(): List<CategoryEntity>

    @Query("SELECT * FROM categories WHERE id = :id")
    suspend fun category(id: Long): CategoryEntity?

    @Insert suspend fun insertCategory(category: CategoryEntity): Long

    @Update suspend fun updateCategory(category: CategoryEntity)

    @Query("DELETE FROM categories WHERE id = :id")
    suspend fun deleteCategory(id: Long)

    @Query("SELECT * FROM category_types")
    suspend fun types(): List<CategoryTypeEntity>

    @Query("SELECT * FROM category_types WHERE id = :id")
    suspend fun type(id: Long): CategoryTypeEntity?

    @Insert suspend fun insertType(type: CategoryTypeEntity): Long

    @Update suspend fun updateType(type: CategoryTypeEntity)

    @Query("DELETE FROM category_types WHERE id = :id")
    suspend fun deleteType(id: Long)

    @Query("SELECT * FROM category_type_links")
    suspend fun typeLinks(): List<CategoryTypeLinkEntity>

    @Query("DELETE FROM category_type_links WHERE categoryId = :id")
    suspend fun clearTypes(id: Long)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertTypeLinks(links: List<CategoryTypeLinkEntity>)

    @Query("SELECT * FROM category_members")
    suspend fun members(): List<CategoryMemberEntity>

    @Query("SELECT * FROM category_members WHERE categoryId = :id")
    suspend fun members(id: Long): List<CategoryMemberEntity>

    @Upsert suspend fun putMembers(members: List<CategoryMemberEntity>)

    @Query("DELETE FROM category_members WHERE categoryId = :categoryId AND bookId = :bookId")
    suspend fun deleteMember(
        categoryId: Long,
        bookId: Long,
    )

    @Query("SELECT * FROM category_series_aliases")
    suspend fun seriesAliases(): List<CategorySeriesAliasEntity>

    @Upsert suspend fun putSeriesAliases(aliases: List<CategorySeriesAliasEntity>)

    @Query("DELETE FROM category_series_aliases WHERE categoryId = :categoryId")
    suspend fun clearSeriesAliases(categoryId: Long)

    @Query("SELECT * FROM category_auto_fill_exclusions")
    suspend fun exclusions(): List<CategoryAutoFillExclusionEntity>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertExclusions(exclusions: List<CategoryAutoFillExclusionEntity>)

    @Query("DELETE FROM category_auto_fill_exclusions WHERE categoryId = :categoryId AND bookId = :bookId")
    suspend fun clearExclusion(
        categoryId: Long,
        bookId: Long,
    )

    @Query("DELETE FROM category_auto_fill_exclusions WHERE categoryId = :categoryId")
    suspend fun clearExclusions(categoryId: Long)

    @Query("SELECT id FROM books")
    suspend fun bookIds(): List<Long>

    @Query("SELECT id FROM books WHERE ignored = 0")
    suspend fun visibleBookIds(): List<Long>

    @Query(
        """
        SELECT b.id, COALESCE(b.titleOverride, b.title) AS title, b.sortTitle, '' AS authorsDisplay,
        b.format, COALESCE(b.coverOverridePath, b.coverPath) AS coverPath, b.addedAt, b.lastReadAt, b.totalProgression,
        NOT EXISTS (SELECT 1 FROM book_files f WHERE f.bookId = b.id AND f.missing = 0) AS missing,
        b.drmProtected, b.rating, b.extractedSeriesName, b.extractedSeriesIndex
        FROM books b WHERE b.ignored = 0 ORDER BY b.id
        """,
    )
    suspend fun books(): List<OrganizationBookRow>

    @Query(
        "SELECT bookId, SUM(activeSeconds) AS activeSeconds FROM reading_sessions " +
            "WHERE browsing = 0 AND activeSeconds >= minimumSessionSeconds GROUP BY bookId",
    )
    suspend fun timeTotals(): List<OrganizationTimeTotal>

    @Query("SELECT * FROM authors")
    suspend fun authors(): List<AuthorEntity>

    @Query("SELECT * FROM author_aliases")
    suspend fun authorAliases(): List<AuthorAliasEntity>

    @Query("SELECT * FROM book_authors")
    suspend fun authorLinks(): List<BookAuthorEntity>

    @Query(
        "SELECT ba.* FROM book_authors ba WHERE ba.overridden = " +
            "(SELECT MAX(overridden) FROM book_authors WHERE bookId = ba.bookId) ORDER BY ba.position, ba.authorId",
    )
    suspend fun effectiveAuthorLinks(): List<BookAuthorEntity>

    @Update suspend fun updateAuthor(author: AuthorEntity)

    @Upsert suspend fun putAuthorLinks(links: List<BookAuthorEntity>)

    @Upsert suspend fun putAuthorAliases(aliases: List<AuthorAliasEntity>)

    @Query("DELETE FROM authors WHERE id = :id")
    suspend fun deleteAuthor(id: Long)
}
