package com.kmab.deft.core.database.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "authors", indices = [Index(value = ["normalizedName"], unique = true)])
data class AuthorEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val sortName: String,
    val normalizedName: String,
)
