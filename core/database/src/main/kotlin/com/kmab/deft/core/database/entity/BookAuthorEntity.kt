package com.kmab.deft.core.database.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index

@Entity(
    tableName = "book_authors",
    primaryKeys = ["bookId", "authorId"],
    foreignKeys = [
        ForeignKey(
            entity = BookEntity::class,
            parentColumns = ["id"],
            childColumns = ["bookId"],
            onDelete = ForeignKey.CASCADE,
        ), ForeignKey(
            entity = AuthorEntity::class,
            parentColumns = ["id"],
            childColumns = ["authorId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("authorId")],
)
data class BookAuthorEntity(
    val bookId: Long,
    val authorId: Long,
    val position: Int,
    val overridden: Boolean,
)
