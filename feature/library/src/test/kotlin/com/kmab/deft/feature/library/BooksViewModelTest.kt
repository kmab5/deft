package com.kmab.deft.feature.library

import android.content.Context
import android.net.Uri
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.lifecycle.ViewModelStore
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.work.Configuration
import androidx.work.CoroutineWorker
import androidx.work.Data
import androidx.work.ListenableWorker
import androidx.work.WorkManager
import androidx.work.WorkerFactory
import androidx.work.WorkerParameters
import androidx.work.testing.SynchronousExecutor
import androidx.work.testing.WorkManagerTestInitHelper
import androidx.work.workDataOf
import com.kmab.deft.core.common.TimeSource
import com.kmab.deft.core.database.AppDatabase
import com.kmab.deft.core.database.entity.BookEntity
import com.kmab.deft.core.database.entity.BookFileEntity
import com.kmab.deft.core.database.entity.LibraryFolderEntity
import com.kmab.deft.core.datastore.LibrarySettingsRepository
import com.kmab.deft.core.model.BookDisplaySize
import com.kmab.deft.core.model.BookFormat
import com.kmab.deft.core.model.BookSort
import com.kmab.deft.library.scanner.BookRepository
import com.kmab.deft.library.scanner.DocumentEntry
import com.kmab.deft.library.scanner.DocumentTree
import com.kmab.deft.library.scanner.FolderAccess
import com.kmab.deft.library.scanner.LibraryFolderRepository
import com.kmab.deft.library.scanner.ScanCoordination
import com.kmab.deft.library.scanner.ScanScheduler
import com.kmab.deft.library.scanner.ScanStatusObserver
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.ExternalResource
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.ByteArrayInputStream
import java.io.IOException
import java.time.Instant

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class BooksViewModelTest {
    @get:Rule(order = 0)
    val mainDispatcher =
        object : ExternalResource() {
            override fun before() {
                Dispatchers.setMain(UnconfinedTestDispatcher())
            }

            override fun after() {
                Dispatchers.resetMain()
            }
        }

    @get:Rule(order = 1)
    val compose = createComposeRule()

    @get:Rule val temp = TemporaryFolder()
    private val context = ApplicationProvider.getApplicationContext<Context>()
    private val job = SupervisorJob()
    private val scope = CoroutineScope(job + Dispatchers.IO)
    private val db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).allowMainThreadQueries().build()
    private val settingsFile by lazy { temp.newFolder().resolve("library.preferences_pb") }
    private val store by lazy { PreferenceDataStoreFactory.create(scope = scope) { settingsFile } }
    private val settings by lazy { LibrarySettingsRepository(store) }
    private lateinit var manager: WorkManager
    private val time = TimeSource { Instant.EPOCH }
    private val scheduler by lazy { ScanScheduler(manager, settings, time, db) }
    private val folders by lazy {
        LibraryFolderRepository(db, EmptyTree(), NoFolderAccess(), scheduler, time, ScanCoordination())
    }
    private val models = ViewModelStore()
    private val scanResult = CompletableDeferred<Data>()
    private var latest = BooksUiState()

    @Before fun initializeWork() {
        val factory =
            object : WorkerFactory() {
                override fun createWorker(
                    appContext: Context,
                    workerClassName: String,
                    workerParameters: WorkerParameters,
                ): ListenableWorker =
                    object : CoroutineWorker(appContext, workerParameters) {
                        override suspend fun doWork(): Result = Result.success(scanResult.await())
                    }
            }
        WorkManagerTestInitHelper.initializeTestWorkManager(
            context,
            Configuration
                .Builder()
                .setExecutor(SynchronousExecutor())
                .setWorkerFactory(factory)
                .build(),
        )
        manager = WorkManager.getInstance(context)
    }

    private fun observe(preferences: LibrarySettingsRepository = settings): BooksViewModel {
        val vm =
            BooksViewModel(
                BookRepository(db),
                preferences,
                folders,
                scheduler,
                ScanStatusObserver(manager),
            )
        models.put("books", vm)
        compose.setContent {
            val state by vm.uiState.collectAsState()
            val current = state
            SideEffect { latest = current }
        }
        compose.waitUntil(5_000) { !latest.isLoading }
        return vm
    }

    @After fun close() {
        compose.runOnIdle { models.clear() }
        manager.cancelAllWork().result.get()
        WorkManagerTestInitHelper.closeWorkDatabase()
        runBlocking { job.cancelAndJoin() }
        db.close()
    }

    private suspend fun add(
        title: String,
        author: String,
        format: BookFormat = BookFormat.EPUB,
        missing: Boolean = false,
    ): Long {
        val id =
            db.bookDao().insert(
                BookEntity(
                    contentHash = title,
                    format = format,
                    title = title,
                    sortTitle = title,
                    addedAt =
                        Instant.ofEpochSecond(
                            if (title ==
                                "Zulu"
                            ) {
                                1
                            } else {
                                2
                            },
                        ),
                ),
            )
        db.authorDao().setBookAuthors(id, listOf(author))
        db.bookFileDao().insert(
            BookFileEntity(
                bookId = id,
                rootUri = "root",
                uri = "content://books/$id",
                displayPath = title,
                size = 1,
                lastModified = 1,
                missing = missing,
                lastSeenAt = Instant.EPOCH,
            ),
        )
        return id
    }

    @Test fun realSettingsRequerySortingMissingAndGrid() =
        runBlocking {
            add("Zulu", "Alpha Writer")
            add("Alpha", "Zulu Writer")
            add("Ghost", "Missing Writer", missing = true)
            val vm = observe()
            compose.waitUntil { latest.books.map { it.title } == listOf("Alpha", "Zulu") }
            assertFalse(latest.hasFolders)
            db.libraryFolderDao().insert(LibraryFolderEntity("root", "Books", Instant.EPOCH))
            compose.waitUntil { latest.hasFolders }
            vm.setSort(BookSort.AUTHOR)
            compose.waitUntil { latest.books.firstOrNull()?.title == "Zulu" }
            vm.setReverseSort(true)
            compose.waitUntil { latest.books.firstOrNull()?.title == "Alpha" }
            vm.setGridSize(BookDisplaySize.SMALL)
            vm.setListSize(BookDisplaySize.LARGE)
            compose.waitUntil { latest.gridSize == BookDisplaySize.SMALL && latest.listSize == BookDisplaySize.LARGE }
            vm.setShowMissing(true)
            compose.waitUntil { latest.books.size == 3 }
            vm.setGridView(false)
            compose.waitUntil { !latest.gridView }
            val saved = settings.settings.first()
            assertEquals(BookSort.AUTHOR, saved.sort)
            assertTrue(saved.showMissing)
            assertTrue(saved.reverseSort)
            assertEquals(BookDisplaySize.SMALL, saved.gridSize)
            assertEquals(BookDisplaySize.LARGE, saved.listSize)
            assertFalse(saved.gridView)
        }

    @Test fun formatViewFiltersDoNotChangeScannerPreferences() =
        runBlocking {
            add("Epub", "Writer")
            add("Text", "Writer", BookFormat.TXT)
            val vm = observe()
            compose.waitUntil { latest.books.size == 2 }
            vm.setFormat(BookFormat.TXT, false)
            compose.waitUntil { latest.books.size == 1 }
            assertEquals("Epub", latest.books.single().title)
            BookFormat.entries.forEach { vm.setFormat(it, false) }
            compose.waitUntil { latest.books.isEmpty() && latest.formatFilter.isEmpty() }
            assertEquals(BookFormat.entries.toSet(), settings.settings.first().enabledFormats)
            assertTrue(manager.getWorkInfosForUniqueWork(ScanScheduler.WORK_NAME).get().isEmpty())
        }

    @Test fun freshViewModelRestoresLastFilterAndSort() =
        runBlocking {
            add("Alpha", "Writer", BookFormat.EPUB)
            add("Zulu", "Writer", BookFormat.EPUB)
            add("Text", "Writer", BookFormat.TXT)
            val original = observe()
            original.setFormat(BookFormat.TXT, false)
            original.setSort(BookSort.TITLE)
            original.setReverseSort(true)
            compose.waitUntil { latest.books.map { it.title } == listOf("Zulu", "Alpha") }
            compose.runOnIdle { models.clear() }
            val recreated =
                BooksViewModel(BookRepository(db), settings, folders, scheduler, ScanStatusObserver(manager))
            models.put("recreated", recreated)
            val restored = recreated.uiState.first { !it.isLoading }
            assertEquals(BookSort.TITLE, restored.sort)
            assertTrue(restored.reverseSort)
            assertFalse(BookFormat.TXT in restored.formatFilter)
            assertEquals(listOf("Zulu", "Alpha"), restored.books.map { it.title })
        }

    @Test fun failedPreferenceWriteRetainsPersistedValueAndReportsError() {
        val failing =
            object : DataStore<Preferences> by store {
                override suspend fun updateData(transform: suspend (Preferences) -> Preferences): Preferences =
                    throw IOException("disk full")
            }
        val vm = observe(LibrarySettingsRepository(failing))
        vm.setGridView(false)
        compose.waitUntil { latest.error != null }
        assertTrue(latest.gridView)
        assertTrue(runBlocking { settings.settings.first().gridView })
    }

    @Test fun manualScanUsesActualUniqueWorkAndAllReportKeys() {
        val vm = observe()
        vm.rescan()
        compose.waitUntil { latest.isScanning }
        assertEquals(1, manager.getWorkInfosForUniqueWork(ScanScheduler.WORK_NAME).get().size)
        scanResult.complete(
            workDataOf(
                "added" to 2,
                "updated" to 3,
                "unchanged" to 4,
                "reattached" to 5,
                "markedMissing" to 6,
                "errors" to 7,
                "unavailableRoots" to arrayOf("root"),
            ),
        )
        compose.waitUntil { latest.scanReport != null && !latest.isScanning }
        val report = requireNotNull(latest.scanReport)
        assertEquals(
            listOf(2, 3, 4, 5, 6, 7),
            listOf(
                report.added,
                report.updated,
                report.unchanged,
                report.reattached,
                report.markedMissing,
                report.errors,
            ),
        )
        assertEquals(listOf("root"), report.unavailableRoots)
        assertEquals(report.summary(), latest.scanMessage)
    }

    @Test fun cancelledScanDoesNotSpinForever() {
        val vm = observe()
        vm.rescan()
        compose.waitUntil { latest.isScanning }
        manager.cancelUniqueWork(ScanScheduler.WORK_NAME).result.get()
        compose.waitUntil { !latest.isScanning && latest.scanMessage?.contains("cancelled") == true }
    }
}

private class EmptyTree : DocumentTree {
    override suspend fun isAccessible(rootUri: String) = true

    override suspend fun isChild(
        parentRootUri: String,
        childRootUri: String,
    ) = parentRootUri == childRootUri

    override fun walk(
        rootUri: String,
        excludedDocumentIds: Set<String>,
    ) = emptyFlow<DocumentEntry>()

    override fun open(uri: String) = ByteArrayInputStream(byteArrayOf())
}

private class NoFolderAccess : FolderAccess {
    override suspend fun displayName(uri: Uri) = "Books"

    override fun hasRead(uri: Uri) = true

    override fun takeRead(uri: Uri) = Unit

    override fun releaseRead(uri: Uri) = Unit
}
