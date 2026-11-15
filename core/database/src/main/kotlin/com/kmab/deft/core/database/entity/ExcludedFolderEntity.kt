package com.kmab.deft.core.database.entity

import androidx.room.Entity
import androidx.room.ForeignKey

@Entity(
    tableName = "excluded_folders",
    primaryKeys = ["treeUri", "documentId"],
    foreignKeys = [
        ForeignKey(
            entity = LibraryFolderEntity::class,
            parentColumns = ["treeUri"],
            childColumns = ["treeUri"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
)
data class ExcludedFolderEntity(
    val treeUri: String,
    val documentId: String,
    val displayPath: String,
)
