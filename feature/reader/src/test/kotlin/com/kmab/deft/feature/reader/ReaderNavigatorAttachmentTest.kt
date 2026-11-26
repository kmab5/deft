@file:OptIn(org.readium.r2.shared.ExperimentalReadiumApi::class)

package com.kmab.deft.feature.reader

import android.app.Application
import android.net.Uri
import android.os.Looper
import android.view.View
import android.widget.FrameLayout
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.fragment.app.FragmentActivity
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.kmab.deft.core.database.AppDatabase
import com.kmab.deft.core.database.entity.BookEntity
import com.kmab.deft.core.database.entity.BookFileEntity
import com.kmab.deft.core.database.entity.BookmarkEntity
import com.kmab.deft.core.datastore.LibrarySettingsRepository
import com.kmab.deft.core.datastore.ReaderSettingsRepository
import com.kmab.deft.core.datastore.TrackingSettingsRepository
import com.kmab.deft.core.model.BookFormat
import com.kmab.deft.core.model.ReaderPreferences
import com.kmab.deft.core.model.ReaderThemes
import com.kmab.deft.feature.readersettings.ReaderFontRepository
import com.kmab.deft.library.notes.NotesRepository
import com.kmab.deft.library.scanner.BookRepository
import com.kmab.deft.tracking.ReadThroughRepository
import com.kmab.deft.tracking.SessionRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.job
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.readium.r2.navigator.epub.EpubNavigatorFragment
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import java.security.MessageDigest
import java.time.Instant

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class ReaderNavigatorAttachmentTest {
    @get:Rule val temporary = TemporaryFolder()

    @Test fun ordinaryBookWithoutBookmarksAttachesAndReattachesReader() = openReader(false, false)

    @Test fun ordinaryBookWithBookmarksAttachesAndReattachesReader() = openReader(true, false)

    @Test fun bookmarkFromNotesLaunchesItsLocatorAndAttachesReader() = openReader(true, true)

    private fun openReader(
        withBookmark: Boolean,
        fromBookmark: Boolean,
    ) {
        val app = ApplicationProvider.getApplicationContext<Application>()
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        val db = Room.inMemoryDatabaseBuilder(app, AppDatabase::class.java).build()
        val controller = Robolectric.buildActivity(FragmentActivity::class.java).setup().visible()
        var renderer: ReaderNavigator? = null
        try {
            val book = insertBook(app, db, withBookmark)
            val dependencies = readerDependencies(app, db, scope)
            val model = dependencies.first
            val launcher = dependencies.second
            controller.get().viewModelStore.put("reader", model)
            if (fromBookmark) launcher.openAt(book, BOOKMARK) else launcher.open(book)
            val intent = shadowOf(app).nextStartedActivity
            assertEquals(book, intent.getLongExtra(ReaderLauncher.BOOK_ID, -1))
            model.open(book, intent.getStringExtra(ReaderLauncher.LOCATOR))
            awaitOpened(model, if (withBookmark) 1 else 0)
            val session = requireNotNull(model.state.value.session)
            if (fromBookmark) assertEquals("EPUB/two.xhtml", session.initialLocator?.href.toString())
            val host = FrameLayout(controller.get()).apply { id = View.generateViewId() }
            controller.get().setContentView(host)
            renderer = ReaderNavigator(controller.get(), model, session) {}
            assertAttachment(renderer, controller.get(), host)
            renderer.detach()
            assertTrue(
                controller
                    .get()
                    .supportFragmentManager.fragments
                    .isEmpty(),
            )
            assertAttachment(renderer, controller.get(), host)
        } finally {
            renderer?.detach()
            controller.pause().stop().destroy()
            runBlocking { scope.coroutineContext.job.cancelAndJoin() }
            db.close()
        }
    }

    private fun assertAttachment(
        renderer: ReaderNavigator,
        activity: FragmentActivity,
        host: FrameLayout,
    ) {
        renderer.attach(host, ReaderPreferences(), ReaderThemes.builtIn.first())
        val fragment = activity.supportFragmentManager.findFragmentByTag("deft-epub") as EpubNavigatorFragment
        assertTrue(fragment.isAdded)
        assertNotNull(fragment.viewModelStore)
        assertSame(host, fragment.requireView().parent)
        renderer.attach(host, ReaderPreferences(), ReaderThemes.builtIn.first())
        assertSame(fragment, activity.supportFragmentManager.findFragmentByTag("deft-epub"))
    }

    private fun awaitOpened(
        model: ReaderViewModel,
        bookmarks: Int,
    ) {
        val deadline = System.nanoTime() + OPEN_TIMEOUT_NANOS
        while (System.nanoTime() < deadline &&
            (model.state.value.loading || model.state.value.bookmarks.size != bookmarks)
        ) {
            shadowOf(Looper.getMainLooper()).idle()
            if (model.state.value.error != null) break
            Thread.sleep(10)
        }
        assertFalse("Reader did not finish opening: ${model.state.value}", model.state.value.loading)
        assertEquals(null, model.state.value.error)
        assertEquals(bookmarks, model.state.value.bookmarks.size)
    }

    private fun readerDependencies(
        app: Application,
        db: AppDatabase,
        scope: CoroutineScope,
    ): Pair<ReaderViewModel, ReaderLauncher> {
        val store = PreferenceDataStoreFactory.create(scope = scope) { temporary.root.resolve("reader.preferences_pb") }
        val settings = ReaderSettingsRepository(store)
        val trackingSettings = TrackingSettingsRepository(store)
        val books = BookRepository(db)
        val tracking =
            ReaderTracking(SessionRepository(db), ReadThroughRepository(db), trackingSettings, scope, ReadingTime::now)
        val launcher = ReaderLauncher(app, settings, books)
        val model =
            ReaderViewModel(
                app,
                books,
                settings,
                LibrarySettingsRepository(store),
                ReaderFontRepository(app, settings),
                ReaderPositionWriter(books),
                ReaderEvents(tracking),
                tracking,
                trackingSettings,
                launcher,
                NotesRepository(db),
            )
        return model to launcher
    }

    private fun insertBook(
        app: Application,
        db: AppDatabase,
        withBookmark: Boolean,
    ): Long =
        runBlocking {
            val bytes = navigatorEpubBytes()
            val uri = Uri.parse("content://reader-attachment/book.epub")
            shadowOf(app.contentResolver).registerInputStream(uri, bytes.inputStream())
            val id =
                db.bookDao().insert(
                    BookEntity(
                        contentHash =
                            MessageDigest
                                .getInstance(
                                    "SHA-256",
                                ).digest(bytes)
                                .joinToString("") { "%02x".format(it) },
                        format = BookFormat.EPUB,
                        title = "Attachment fixture",
                        sortTitle = "attachment fixture",
                        addedAt = Instant.EPOCH,
                    ),
                )
            db.bookFileDao().insert(
                BookFileEntity(
                    bookId = id,
                    rootUri = "content://reader-attachment",
                    uri = uri.toString(),
                    displayPath = "book.epub",
                    size = bytes.size.toLong(),
                    lastModified = 0,
                    missing = false,
                    lastSeenAt = Instant.EPOCH,
                ),
            )
            if (withBookmark) {
                db.bookmarkDao().insert(
                    BookmarkEntity(
                        bookId = id,
                        locator = BOOKMARK,
                        totalProgression = 0.5,
                        label = "Second chapter",
                        createdAt = Instant.EPOCH,
                    ),
                )
            }
            id
        }
}

private const val OPEN_TIMEOUT_NANOS = 10_000_000_000L
private const val BOOKMARK =
    """{"href":"EPUB/two.xhtml","type":"application/xhtml+xml",""" +
        """"locations":{"progression":0.0}}"""
