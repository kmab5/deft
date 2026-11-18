package com.kmab.deft.library.scanner

import android.content.Context
import androidx.work.WorkManager
import com.kmab.deft.core.common.ContentHasher
import com.kmab.deft.core.common.SystemTimeSource
import com.kmab.deft.core.common.TimeSource
import com.kmab.deft.core.database.AppDatabase
import com.kmab.deft.format.epub.EpubMetadataExtractor
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object ScannerModule {
    @Provides @Singleton
    fun tree(tree: SafDocumentTree): DocumentTree = tree

    @Provides @Singleton
    fun access(access: SafFolderAccess): FolderAccess = access

    @Provides @Singleton
    fun time(): TimeSource = SystemTimeSource

    @Provides @Singleton
    fun workManager(
        @ApplicationContext context: Context,
    ): WorkManager = WorkManager.getInstance(context)

    @Provides @Singleton
    fun covers(
        @ApplicationContext context: Context,
    ): CoverStore = FileCoverStore(context.filesDir)

    @Provides @Singleton
    fun scanner(
        tree: DocumentTree,
        covers: CoverStore,
        db: AppDatabase,
        time: TimeSource,
    ): LibraryScanner =
        LibraryScanner(
            tree,
            ContentHasher(),
            MetadataExtractorRegistry(setOf(EpubMetadataExtractor())),
            covers,
            db,
            time,
        )
}
