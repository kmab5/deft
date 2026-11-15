package com.kmab.deft.feature.settings

import android.content.Context
import android.net.Uri
import androidx.activity.compose.LocalActivityResultRegistryOwner
import androidx.activity.result.ActivityResultRegistry
import androidx.activity.result.ActivityResultRegistryOwner
import androidx.activity.result.contract.ActivityResultContract
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.isToggleable
import androidx.compose.ui.test.junit4.StateRestorationTester
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.core.app.ActivityOptionsCompat
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.lifecycle.viewModelScope
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.work.Configuration
import androidx.work.CoroutineWorker
import androidx.work.ListenableWorker
import androidx.work.WorkManager
import androidx.work.WorkerFactory
import androidx.work.WorkerParameters
import androidx.work.testing.SynchronousExecutor
import androidx.work.testing.WorkManagerTestInitHelper
import com.kmab.deft.core.common.TimeSource
import com.kmab.deft.core.database.AppDatabase
import com.kmab.deft.core.datastore.LibrarySettingsRepository
import com.kmab.deft.core.designsystem.DeftTheme
import com.kmab.deft.library.scanner.DocumentEntry
import com.kmab.deft.library.scanner.DocumentTree
import com.kmab.deft.library.scanner.FolderAccess
import com.kmab.deft.library.scanner.LibraryFolderRepository
import com.kmab.deft.library.scanner.ScanCoordination
import com.kmab.deft.library.scanner.ScanScheduler
import com.kmab.deft.library.scanner.ScanStatusObserver
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.ExternalResource
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.ByteArrayInputStream
import java.time.Instant

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class LibrarySettingsScreenTest {
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
    private val vm by lazy { LibrarySettingsViewModel(folders, settings, scheduler, ScanStatusObserver(manager)) }

    @get:Rule val temp = TemporaryFolder()
    private val context = ApplicationProvider.getApplicationContext<Context>()
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val renderScreen = mutableStateOf(true)
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
    private lateinit var manager: WorkManager
    private val scheduler by lazy { ScanScheduler(manager, settings, time, db) }
    private val folders by lazy { LibraryFolderRepository(db, tree, access, scheduler, time, coordination) }

    @Before fun workManager() {
        val factory =
            object : WorkerFactory() {
                override fun createWorker(
                    appContext: Context,
                    workerClassName: String,
                    workerParameters: WorkerParameters,
                ): ListenableWorker = WaitingWorker(appContext, workerParameters)
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
        compose.runOnIdle { renderScreen.value = false }
        compose.waitForIdle()
        vm.viewModelScope.cancel()
        manager.cancelAllWork().result.get()
        WorkManagerTestInitHelper.closeWorkDatabase()
        runBlocking { scope.coroutineContext[Job]?.cancelAndJoin() }
        db.close()
    }

    private fun requests() = manager.getWorkInfosForUniqueWork(ScanScheduler.WORK_NAME).get()

    @Test fun formatPersistsAndSchedules() {
        compose.setContent { if (renderScreen.value) DeftTheme { LibrarySettingsScreen({}, vm) } }
        compose.runOnIdle { vm.format(com.kmab.deft.core.model.BookFormat.PDF, false) }
        compose.waitUntil {
            runBlocking {
                com.kmab.deft.core.model.BookFormat.PDF !in
                    settings.settings.first().enabledFormats
            }
        }
        compose.waitUntil { requests().any { !it.state.isFinished } }
    }

    @Test fun invalidExclusionReportsErrorAndRemovalUsesRepositoryOutbox() {
        runBlocking { folders.add(Uri.parse("content://test/tree/root")) }
        compose.setContent { if (renderScreen.value) DeftTheme { LibrarySettingsScreen({}, vm) } }
        compose.runOnIdle { vm.exclude("content://test/tree/root", "Books", Uri.parse("content://test/tree/outside")) }
        compose.waitUntil { vm.error.value != null }
        assertEquals("Pick a folder inside Books", vm.error.value)
        compose.runOnIdle { vm.remove("content://test/tree/root") }
        compose.waitUntil { runBlocking { db.libraryFolderDao().pendingRemovals().isNotEmpty() } }
        assertEquals(listOf("content://test/tree/root"), runBlocking { db.libraryFolderDao().pendingRemovals() })
        assertEquals(1, access.releases)
    }

    @Test fun nineCheckboxesRescanAndRemovalConfirmation() {
        runBlocking { folders.add(Uri.parse("content://test/tree/root")) }
        manager.cancelAllWork().result.get()
        compose.setContent { if (renderScreen.value) DeftTheme { LibrarySettingsScreen({}, vm) } }
        compose.onAllNodes(isToggleable()).assertCountEquals(9)
        compose.onNodeWithText("remove").performClick()
        compose.onNodeWithText("cancel").performClick()
        assertEquals(1, runBlocking { db.libraryFolderDao().getAll().size })
        compose.onNodeWithText("remove").performClick()
        compose.onNodeWithText("Its books will be marked missing. Their history stays.").assertExists()
        compose
            .onAllNodes(
                androidx.compose.ui.test
                    .hasText("remove"),
            )[1]
            .performClick()
        compose.waitUntil { runBlocking { db.libraryFolderDao().getAll().isEmpty() } }
        manager.cancelAllWork().result.get()
        compose.onNodeWithText("rescan now").performScrollTo().performClick()
        compose.waitUntil { requests().any { !it.state.isFinished } }
    }

    @Test fun exclusionPickerKeepsItsPurposeAfterStateRestoration() {
        val root = "content://test/tree/root"
        val child = "content://test/tree/child"
        tree.parent = root to child
        runBlocking { folders.add(Uri.parse(root)) }
        var launchedRequestCode = 0
        val registry =
            object : ActivityResultRegistry() {
                override fun <I, O> onLaunch(
                    requestCode: Int,
                    contract: ActivityResultContract<I, O>,
                    input: I,
                    options: ActivityOptionsCompat?,
                ) {
                    launchedRequestCode = requestCode
                }
            }
        val owner =
            object : ActivityResultRegistryOwner {
                override val activityResultRegistry = registry
            }
        val restoration = StateRestorationTester(compose)
        restoration.setContent {
            CompositionLocalProvider(LocalActivityResultRegistryOwner provides owner) {
                if (renderScreen.value) DeftTheme { LibrarySettingsScreen({}, vm) }
            }
        }
        compose.waitUntil {
            compose
                .onAllNodes(
                    androidx.compose.ui.test
                        .hasText("Books"),
                ).fetchSemanticsNodes()
                .isNotEmpty()
        }
        compose.onNodeWithText("Books").performClick()
        compose.onNodeWithText("exclude a subfolder").performClick()
        restoration.emulateSavedInstanceStateRestore()
        compose.runOnUiThread { registry.dispatchResult(launchedRequestCode, Uri.parse(child)) }
        compose.waitUntil { runBlocking { folders.exclusions(root).first().isNotEmpty() } }
        assertEquals(
            "child",
            runBlocking {
                folders
                    .exclusions(root)
                    .first()
                    .single()
                    .documentId
            },
        )
        assertEquals(listOf(root), runBlocking { db.libraryFolderDao().getAll().map { it.treeUri } })
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
    var releases = 0
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
        releases++
        reads -= uri
    }
}
