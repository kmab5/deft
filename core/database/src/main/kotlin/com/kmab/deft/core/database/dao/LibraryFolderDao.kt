package com.kmab.deft.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.kmab.deft.core.database.entity.ExcludedFolderEntity
import com.kmab.deft.core.database.entity.LibraryFolderEntity
import com.kmab.deft.core.database.entity.PendingRootRemovalEntity
import kotlinx.coroutines.flow.Flow

@Dao
// Selection and its transactional removal outbox form one persistence aggregate.
@Suppress("TooManyFunctions")
interface LibraryFolderDao {
    @Query("SELECT * FROM library_folders ORDER BY addedAt, treeUri")
    suspend fun getAll(): List<LibraryFolderEntity>

    @Query("SELECT treeUri FROM pending_root_removals ORDER BY treeUri")
    suspend fun pendingRemovals(): List<String>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun recordRemoval(removal: PendingRootRemovalEntity)

    @Query("DELETE FROM pending_root_removals WHERE treeUri IN (:roots)")
    suspend fun acknowledgeRemovals(roots: List<String>)

    @Transaction
    suspend fun removeForReconciliation(treeUri: String) {
        recordRemoval(PendingRootRemovalEntity(treeUri))
        remove(treeUri)
    }

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
