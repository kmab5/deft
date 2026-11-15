package com.kmab.deft.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.kmab.deft.core.database.entity.ExcludedFolderEntity
import com.kmab.deft.core.database.entity.LibraryFolderEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface LibraryFolderDao {
    @Query("SELECT * FROM library_folders ORDER BY addedAt, treeUri")
    fun observeAll(): Flow<List<LibraryFolderEntity>>

    @Insert suspend fun insert(folder: LibraryFolderEntity)

    @Query("DELETE FROM library_folders WHERE treeUri = :treeUri")
    suspend fun remove(treeUri: String)

    @Query("SELECT * FROM excluded_folders WHERE treeUri = :treeUri ORDER BY documentId")
    fun observeExclusions(treeUri: String): Flow<List<ExcludedFolderEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun addExclusion(exclusion: ExcludedFolderEntity)

    @Query("DELETE FROM excluded_folders WHERE treeUri = :treeUri AND documentId = :documentId")
    suspend fun removeExclusion(
        treeUri: String,
        documentId: String,
    )
}
