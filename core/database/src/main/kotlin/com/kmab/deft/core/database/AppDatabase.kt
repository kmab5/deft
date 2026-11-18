package com.kmab.deft.core.database

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.kmab.deft.core.database.dao.AuthorDao
import com.kmab.deft.core.database.dao.BookDao
import com.kmab.deft.core.database.dao.BookFileDao
import com.kmab.deft.core.database.dao.LibraryFolderDao
import com.kmab.deft.core.database.entity.AuthorAliasEntity
import com.kmab.deft.core.database.entity.AuthorEntity
import com.kmab.deft.core.database.entity.BookAuthorEntity
import com.kmab.deft.core.database.entity.BookEntity
import com.kmab.deft.core.database.entity.BookFileEntity
import com.kmab.deft.core.database.entity.ExcludedFolderEntity
import com.kmab.deft.core.database.entity.LibraryFolderEntity
import com.kmab.deft.core.database.entity.PendingRootRemovalEntity

@Database(
    entities = [
        BookEntity::class,
        BookFileEntity::class,
        AuthorEntity::class,
        AuthorAliasEntity::class,
        BookAuthorEntity::class,
        LibraryFolderEntity::class,
        ExcludedFolderEntity::class,
        PendingRootRemovalEntity::class,
    ],
    version = 3,
    exportSchema = true,
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun bookDao(): BookDao

    abstract fun bookFileDao(): BookFileDao

    abstract fun authorDao(): AuthorDao

    abstract fun libraryFolderDao(): LibraryFolderDao
}
