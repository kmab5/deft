package com.kmab.deft.core.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.time.Instant

@Entity(tableName = "library_folders")
data class LibraryFolderEntity(
    @PrimaryKey val treeUri: String,
    val displayName: String,
    val addedAt: Instant,
)
