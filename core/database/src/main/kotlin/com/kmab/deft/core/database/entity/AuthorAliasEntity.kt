package com.kmab.deft.core.database.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "author_aliases",
    foreignKeys = [
        ForeignKey(
            entity = AuthorEntity::class,
            parentColumns = ["id"],
            childColumns = ["authorId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("authorId")],
)
data class AuthorAliasEntity(
    @PrimaryKey val aliasNormalizedName: String,
    val authorId: Long,
)
