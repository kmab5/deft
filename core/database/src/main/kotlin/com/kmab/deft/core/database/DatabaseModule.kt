package com.kmab.deft.core.database

import android.content.Context
import androidx.room.Room
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {
    @Provides @Singleton
    fun database(
        @ApplicationContext context: Context,
    ): AppDatabase = Room.databaseBuilder(context, AppDatabase::class.java, "deft.db").build()

    @Provides fun books(db: AppDatabase) = db.bookDao()

    @Provides fun files(db: AppDatabase) = db.bookFileDao()

    @Provides fun authors(db: AppDatabase) = db.authorDao()

    @Provides fun folders(db: AppDatabase) = db.libraryFolderDao()
}
