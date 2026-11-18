package com.kmab.deft.library.scanner

import android.content.Context
import android.net.Uri
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.work.Configuration
import androidx.work.CoroutineWorker
import androidx.work.ListenableWorker
import androidx.work.WorkManager
import androidx.work.WorkerFactory
import androidx.work.WorkerParameters
import androidx.work.testing.SynchronousExecutor
import androidx.work.testing.TestListenableWorkerBuilder
import androidx.work.testing.WorkManagerTestInitHelper
import com.kmab.deft.core.common.ContentHasher
import com.kmab.deft.core.common.TimeSource
import com.kmab.deft.core.database.AppDatabase
import com.kmab.deft.core.database.entity.LibraryFolderEntity
import com.kmab.deft.core.datastore.LibrarySettingsRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeoutOrNull
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.ByteArrayInputStream
import java.time.Instant

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ScanIntegrationTest {
    @get:Rule val temp = TemporaryFolder()
    private val context = ApplicationProvider.getApplicationContext<Context>()
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).allowMainThreadQueries().build()
    private val settingsFile by lazy { temp.newFolder().resolve("settings.preferences_pb") }
    private val settings by lazy {
        LibrarySettingsRepository(PreferenceDataStoreFactory.create(scope = scope) { settingsFile })
    }
    private val now = Instant.parse("2026-11-15T10:00:00Z")
    private val time = TimeSource { now }
    private val tree = BackgroundTree()
    private val access = MemoryFolderAccess()
    private val coordination = ScanCoordination()
    private val scanner =
        LibraryScanner(
            tree,
            ContentHasher(),
            MetadataExtractorRegistry(emptySet()),
            object : CoverStore {
                override suspend fun save(
                    hash: com.kmab.deft.core.model.ContentHash,
                    image: ByteArray,
                ): String? = null
            },
            db,
            time,
        )
    private val runner by lazy { ScanRunner(db, scanner, settings, time, coordination, access) }
    private lateinit var manager: WorkManager
    private val requestedForegroundModes = mutableListOf<Boolean>()
    private val scheduler by lazy { ScanScheduler(manager, settings, time, db) }
    private val folders by lazy { LibraryFolderRepository(db, tree, access, scheduler, time, coordination) }

    @Before fun workManager() {
        val factory =
            object : WorkerFactory() {
                override fun createWorker(
                    appContext: Context,
                    workerClassName: String,
                    workerParameters: WorkerParameters,
                ): ListenableWorker {
                    requestedForegroundModes +=
                        workerParameters.inputData.getBoolean(ScanScheduler.USER_INITIATED, false)
                    return WaitingWorker(appContext, workerParameters)
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

    @After fun close() {
        manager.cancelAllWork().result.get()
        WorkManagerTestInitHelper.closeWorkDatabase()
        db.close()
        scope.cancel()
    }

    private fun requests() = manager.getWorkInfosForUniqueWork(ScanScheduler.WORK_NAME).get()

    @Test fun exclusionsRequireStrictProviderDescendantsAndCanBeUndone() =
        runBlocking {
            val root = android.provider.DocumentsContract.buildTreeDocumentUri("deft.fake", "opaque-root")
            val child = android.provider.DocumentsContract.buildTreeDocumentUri("deft.fake", "different-child")
            folders.add(root)
            try {
                folders.exclude(root.toString(), root)
                error("Expected root exclusion rejection")
            } catch (_: IllegalArgumentException) {
            }
            try {
                folders.exclude(root.toString(), child)
                error("Expected unrelated folder rejection")
            } catch (_: IllegalArgumentException) {
            }
            assertTrue(folders.exclusions(root.toString()).first().isEmpty())
            tree.parent = root.toString() to child.toString()
            folders.exclude(root.toString(), child)
            assertEquals(listOf("different-child"), folders.exclusions(root.toString()).first().map { it.documentId })
            folders.unexclude(root.toString(), "different-child")
            assertTrue(folders.exclusions(root.toString()).first().isEmpty())
        }

    @Test fun automaticStalenessAndKeepVersusManualReplace() =
        runBlocking {
            settings.setLastScanAt(now.minusSeconds(5 * 3600))
            scheduler.scanOnStartIfStale()
            assertTrue(requests().isEmpty())
            settings.setLastScanAt(now.minusSeconds(7 * 3600))
            scheduler.scanOnStartIfStale()
            val first = requests().single().id
            assertFalse(requestedForegroundModes.last())
            scheduler.scanOnStartIfStale()
            assertEquals(first, requests().single().id)
            scheduler.scanNow()
            assertTrue(requestedForegroundModes.last())
            assertTrue(requests().none { it.id == first && !it.state.isFinished })
            assertTrue(requests().any { it.id != first && !it.state.isFinished })
            assertEquals(1, requests().count { !it.state.isFinished })
        }

    @Test fun nullLastScanSchedulesAndPendingRemovalOverridesFreshTimestamp() =
        runBlocking {
            scheduler.scanOnStartIfStale()
            assertEquals(1, requests().size)
            manager.cancelAllWork().result.get()
            settings.setLastScanAt(now)
            db.libraryFolderDao().removeForReconciliation("old")
            scheduler.scanOnStartIfStale()
            assertEquals(1, requests().count { !it.state.isFinished })
        }

    @Test fun workerIndexesFilesStoresTimestampAndOutputsReport() =
        runBlocking {
            db.libraryFolderDao().insert(LibraryFolderEntity("root", "Books", now))
            val factory =
                object : WorkerFactory() {
                    override fun createWorker(
                        appContext: Context,
                        workerClassName: String,
                        workerParameters: WorkerParameters,
                    ): ListenableWorker = ScanWorker(appContext, workerParameters, runner)
                }
            val worker =
                TestListenableWorkerBuilder
                    .from(
                        context,
                        ScanWorker::class.java,
                    ).setWorkerFactory(factory)
                    .build()
            val result = worker.doWork()
            assertTrue(result is ListenableWorker.Result.Success)
            assertEquals(1, (result as ListenableWorker.Result.Success).outputData.getInt("added", -1))
            assertEquals(now, settings.settings.first().lastScanAt)
        }

    @Test fun canceledScanRetainsRemovalAcrossReplacementAndRunnerRecreation() =
        runBlocking {
            db.libraryFolderDao().insert(LibraryFolderEntity("old", "Old", now))
            runner.run()
            folders.remove("old")
            scheduler.scanNow()
            db.libraryFolderDao().insert(LibraryFolderEntity("other", "Other", now))
            tree.cancelWalk = true
            try {
                runner.run()
                error("Expected cancellation")
            } catch (_: CancellationException) {
            }
            assertEquals(listOf("old"), db.libraryFolderDao().pendingRemovals())
            tree.cancelWalk = false
            val restarted = ScanRunner(db, scanner, settings, time, ScanCoordination(), access)
            restarted.run()
            assertTrue(db.libraryFolderDao().pendingRemovals().isEmpty())
            assertTrue(
                db
                    .bookFileDao()
                    .getAll()
                    .filter { it.rootUri == "old" }
                    .all { it.missing },
            )
        }

    @Test fun readdKeepsBooksPresentAndDoesNotReleaseReacquiredGrant() =
        runBlocking {
            folders.add(Uri.parse("root"))
            runner.run()
            folders.remove("root")
            folders.add(Uri.parse("root"))
            runner.run()
            assertTrue(access.hasRead(Uri.parse("root")))
            assertFalse(
                db.bookDao().isMissing(
                    db
                        .bookFileDao()
                        .getAll()
                        .single()
                        .bookId,
                ),
            )
            assertTrue(db.libraryFolderDao().pendingRemovals().isEmpty())
        }

    @Test fun scanAndFolderRemovalAreSerializedUntilAcknowledgement() =
        runBlocking {
            folders.add(Uri.parse("root"))
            runner.run()
            folders.remove("root")
            folders.add(Uri.parse("root"))
            val entered = CompletableDeferred<Unit>()
            val release = CompletableDeferred<Unit>()
            tree.gate = {
                entered.complete(Unit)
                release.await()
            }
            val scan = async(Dispatchers.Default) { runner.run() }
            entered.await()
            val remove = async(Dispatchers.Default) { folders.remove("root") }
            assertNull(
                withTimeoutOrNull(100) {
                    remove.await()
                    true
                },
            )
            assertEquals(listOf("root"), db.libraryFolderDao().getAll().map { it.treeUri })
            release.complete(Unit)
            scan.await()
            remove.await()
            assertEquals(listOf("root"), db.libraryFolderDao().pendingRemovals())
            tree.gate = null
            runner.run()
            assertTrue(
                db
                    .bookFileDao()
                    .getAll()
                    .single()
                    .missing,
            )
        }

    @Test fun overlappingRootsRejectedBothOrdersAndFailedAddRollsBackOnlyNewGrant() =
        runBlocking {
            tree.parent = "parent" to "child"
            for ((first, second) in listOf("parent" to "child", "child" to "parent")) {
                folders.add(Uri.parse(first))
                try {
                    folders.add(Uri.parse(second))
                    error("Expected overlap rejection")
                } catch (failure: IllegalArgumentException) {
                    if (second == "parent") {
                        assertTrue(failure.message.orEmpty().contains("Remove Books"))
                        assertTrue(failure.message.orEmpty().contains("then add the parent"))
                        assertTrue(failure.message.orEmpty().contains("exclusions again"))
                    } else {
                        assertTrue(failure.message.orEmpty().contains("already included inside Books"))
                    }
                }
                assertFalse(access.hasRead(Uri.parse(second)))
                folders.remove(first)
            }
            access.failName = true
            try {
                folders.add(Uri.parse("new"))
                error("Expected name failure")
            } catch (_: java.io.IOException) {
            }
            assertFalse(access.hasRead(Uri.parse("new")))
            access.takeRead(Uri.parse("existing"))
            try {
                folders.add(Uri.parse("existing"))
                error("Expected name failure")
            } catch (_: java.io.IOException) {
            }
            assertTrue(access.hasRead(Uri.parse("existing")))
        }
}

private class WaitingWorker(
    context: Context,
    params: WorkerParameters,
) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        CompletableDeferred<Unit>().await()
        return Result.success()
    }
}

private class BackgroundTree : DocumentTree {
    var cancelWalk = false
    var parent: Pair<String, String>? = null
    var gate: (suspend () -> Unit)? = null

    override suspend fun isAccessible(rootUri: String) = true

    override suspend fun isChild(
        parentRootUri: String,
        childRootUri: String,
    ) = parentRootUri == childRootUri || parent == parentRootUri to childRootUri

    override fun walk(
        rootUri: String,
        excludedDocumentIds: Set<String>,
    ) = flow {
        gate?.invoke()
        if (cancelWalk) throw CancellationException("interrupted")
        emit(DocumentEntry("$rootUri/book.txt", rootUri, "Books/Book.txt", "Book.txt", 1, 1))
    }

    override fun open(uri: String) = ByteArrayInputStream(uri.substringBefore('/').toByteArray())
}

private class MemoryFolderAccess : FolderAccess {
    private val reads = mutableSetOf<Uri>()
    var failName = false

    override suspend fun displayName(uri: Uri): String {
        if (failName) throw java.io.IOException("No metadata")
        return "Books"
    }

    override fun hasRead(uri: Uri) = uri in reads

    override fun takeRead(uri: Uri) {
        reads += uri
    }

    override fun releaseRead(uri: Uri) {
        reads -= uri
    }
}
