package com.kmab.deft.core.database

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.kmab.deft.core.database.dao.AuthorDao
import com.kmab.deft.core.database.dao.BookDao
import com.kmab.deft.core.database.dao.BookFileDao
import com.kmab.deft.core.database.dao.BookmarkDao
import com.kmab.deft.core.database.dao.LibraryFolderDao
import com.kmab.deft.core.database.dao.OrganizationDao
import com.kmab.deft.core.database.dao.TrackingDao
import com.kmab.deft.core.database.entity.AuthorAliasEntity
import com.kmab.deft.core.database.entity.AuthorEntity
import com.kmab.deft.core.database.entity.BookAuthorEntity
import com.kmab.deft.core.database.entity.BookEntity
import com.kmab.deft.core.database.entity.BookFileEntity
import com.kmab.deft.core.database.entity.BookmarkEntity
import com.kmab.deft.core.database.entity.CategoryAutoFillExclusionEntity
import com.kmab.deft.core.database.entity.CategoryEntity
import com.kmab.deft.core.database.entity.CategoryMemberEntity
import com.kmab.deft.core.database.entity.CategorySeriesAliasEntity
import com.kmab.deft.core.database.entity.CategoryTypeEntity
import com.kmab.deft.core.database.entity.CategoryTypeLinkEntity
import com.kmab.deft.core.database.entity.ExcludedFolderEntity
import com.kmab.deft.core.database.entity.LibraryFolderEntity
import com.kmab.deft.core.database.entity.PendingRootRemovalEntity
import com.kmab.deft.core.database.entity.ReadThroughEntity
import com.kmab.deft.core.database.entity.ReadingSessionEntity

@Database(
    entities = [
        BookEntity::class,
        ReadThroughEntity::class,
        ReadingSessionEntity::class,
        BookmarkEntity::class,
        BookFileEntity::class,
        AuthorEntity::class,
        AuthorAliasEntity::class,
        BookAuthorEntity::class,
        LibraryFolderEntity::class,
        ExcludedFolderEntity::class,
        PendingRootRemovalEntity::class,
        CategoryEntity::class,
        CategoryTypeEntity::class,
        CategoryTypeLinkEntity::class,
        CategoryMemberEntity::class,
        CategorySeriesAliasEntity::class,
        CategoryAutoFillExclusionEntity::class,
    ],
    version = 7,
    exportSchema = true,
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun organizationDao(): OrganizationDao

    abstract fun trackingDao(): TrackingDao

    abstract fun bookDao(): BookDao

    abstract fun bookmarkDao(): BookmarkDao

    abstract fun bookFileDao(): BookFileDao

    abstract fun authorDao(): AuthorDao

    abstract fun libraryFolderDao(): LibraryFolderDao
}
