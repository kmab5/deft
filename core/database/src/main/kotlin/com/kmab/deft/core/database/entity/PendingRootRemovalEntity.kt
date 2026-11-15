package com.kmab.deft.core.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/** Transactional outbox; no foreign key because it must outlive the selected folder. */
@Entity(tableName = "pending_root_removals")
data class PendingRootRemovalEntity(
    @PrimaryKey val treeUri: String,
)
