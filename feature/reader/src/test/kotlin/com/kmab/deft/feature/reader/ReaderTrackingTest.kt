package com.kmab.deft.feature.reader

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.kmab.deft.core.database.AppDatabase
import com.kmab.deft.core.database.entity.BookEntity
import com.kmab.deft.core.datastore.TrackingSettingsRepository
import com.kmab.deft.core.model.BookFormat
import com.kmab.deft.core.model.BookState
import com.kmab.deft.core.model.ReaderOpenChoice
import com.kmab.deft.core.model.ReadingPosition
import com.kmab.deft.tracking.ReadThroughRepository
import com.kmab.deft.tracking.SessionRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.job
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.time.Instant

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ReaderTrackingTest {
    @get:Rule val temp = TemporaryFolder()
    private lateinit var database: AppDatabase
    private lateinit var reads: ReadThroughRepository
    private lateinit var settings: TrackingSettingsRepository
    private lateinit var tracking: ReaderTracking
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var book = 0L
    private var other = 0L

    @Volatile private var elapsed = 0L
    private val epoch = Instant.parse("2026-11-20T12:00:00Z")
    private val position = ReadingPosition("page", 0.2, 20)

    @Before fun setup() =
        runBlocking {
            database =
                Room
                    .inMemoryDatabaseBuilder(
                        ApplicationProvider.getApplicationContext(),
                        AppDatabase::class.java,
                    ).build()
            reads = ReadThroughRepository(database)
            book =
                database.bookDao().insert(
                    BookEntity(
                        contentHash = "one",
                        format = BookFormat.EPUB,
                        title = "One",
                        sortTitle = "one",
                        addedAt = epoch,
                    ),
                )
            other =
                database.bookDao().insert(
                    BookEntity(
                        contentHash = "two",
                        format = BookFormat.EPUB,
                        title = "Two",
                        sortTitle = "two",
                        addedAt = epoch,
                    ),
                )
            settings =
                TrackingSettingsRepository(
                    PreferenceDataStoreFactory.create(scope = scope) {
                        temp.root.resolve("tracking.preferences_pb")
                    },
                )
            tracking =
                ReaderTracking(SessionRepository(database), reads, settings, scope) {
                    ReadingTime(elapsed, epoch.plusMillis(elapsed), 10800)
                }
        }

    @After fun close() =
        runBlocking {
            scope.coroutineContext.job.cancelAndJoin()
            database.close()
        }

    @Test fun queuedReadingAndFinalVisibilityAreNeverDroppedUnderBurstLoad() =
        runBlocking {
            tracking.prepare(book)
            tracking.accept(ReaderEvent.Visibility(book, position, true))
            elapsed = 60_000
            repeat(200) { tracking.accept(ReaderEvent.PageTurn(book, position)) }
            tracking.accept(ReaderEvent.Visibility(book, position, false))
            tracking.prepare(other)
            val saved = reads.get(book).sessions.single()
            assertEquals(60L, saved.activeSeconds)
            assertEquals(200, saved.pagesTurned)
            assertTrue(saved.finalized)
        }

    @Test fun staleBookEventsCannotStopNewBooksSession() =
        runBlocking {
            tracking.prepare(book)
            tracking.accept(ReaderEvent.Visibility(book, position, true))
            elapsed = 60_000
            tracking.prepare(other)
            tracking.accept(ReaderEvent.Visibility(other, position, true))
            tracking.accept(ReaderEvent.Visibility(book, position, false))
            tracking.accept(ReaderEvent.PageTurn(book, position))
            elapsed = 120_000
            tracking.accept(ReaderEvent.Visibility(other, position, false))
            tracking.prepare(book)
            assertEquals(
                60L,
                reads
                    .get(other)
                    .sessions
                    .single()
                    .activeSeconds,
            )
            assertEquals(
                0,
                reads
                    .get(other)
                    .sessions
                    .single()
                    .pagesTurned,
            )
        }

    @Test fun finishedChoiceBlocksAllTimeUntilBrowsingIsChosen() =
        runBlocking {
            reads.setState(book, BookState.FINISHED, epoch)
            tracking.prepare(book)
            tracking.accept(ReaderEvent.Visibility(book, position, true))
            elapsed = 60_000
            tracking.accept(ReaderEvent.PageTurn(book, position))
            tracking.choose(ReaderOpenChoice.BROWSING)
            tracking.accept(ReaderEvent.Visibility(book, position, true))
            elapsed = 120_000
            tracking.accept(ReaderEvent.Visibility(book, position, false))
            tracking.prepare(other)
            val snapshot = reads.get(book)
            assertEquals(BookState.FINISHED, snapshot.state)
            assertEquals(60L, snapshot.sessions.single().activeSeconds)
            assertTrue(snapshot.sessions.single().browsing)
            assertEquals(0L, snapshot.activeSeconds)
        }

    @Test fun lastPageDwellingQualifiesAndPromptClaimSurvivesReopening() =
        runBlocking {
            tracking.prepare(book)
            val end = ReadingPosition("end", 0.99, 100)
            tracking.accept(ReaderEvent.Visibility(book, end, true))
            elapsed = 60_000
            tracking.accept(ReaderEvent.Location(book, end))
            withTimeout(5_000) { tracking.state.first { it.finishPrompt } }
            assertEquals(60L, reads.get(book).activeSeconds)
            assertEquals(BookState.READING, reads.get(book).state)
            assertTrue(reads.get(book).latest!!.finishPromptShown)
            tracking.dismissFinishPrompt()
            tracking.prepare(book)
            tracking.accept(ReaderEvent.Visibility(book, end, true))
            withTimeout(5_000) { tracking.state.first { it.active } }
            assertFalse(tracking.state.value.finishPrompt)
        }

    @Test fun deletedReadThroughCannotBlockAnotherBooksSessionOrBeResurrected() =
        runBlocking {
            val read = reads.setState(book, BookState.READING, epoch)!!
            tracking.prepare(book)
            tracking.accept(ReaderEvent.Visibility(book, position, true))
            reads.delete(read.id)
            elapsed = 60_000
            tracking.accept(ReaderEvent.PageTurn(book, position))
            tracking.accept(ReaderEvent.Visibility(book, position, false))
            tracking.prepare(other)
            tracking.accept(ReaderEvent.Visibility(other, position, true))
            elapsed = 120_000
            tracking.accept(ReaderEvent.Visibility(other, position, false))
            assertEquals(other, tracking.finish())
            assertTrue(reads.get(book).readThroughs.isEmpty())
            assertTrue(reads.get(book).sessions.isEmpty())
            assertEquals(60L, reads.get(other).activeSeconds)
        }

    @Test fun forgottenBookCannotBlockAnotherBooksSession() =
        runBlocking {
            tracking.prepare(book)
            tracking.accept(ReaderEvent.Visibility(book, position, true))
            database.bookDao().forget(book)
            elapsed = 60_000
            tracking.accept(ReaderEvent.PageTurn(book, position))
            tracking.accept(ReaderEvent.Visibility(book, position, false))
            tracking.prepare(other)
            tracking.accept(ReaderEvent.Visibility(other, position, true))
            elapsed = 120_000
            tracking.accept(ReaderEvent.Visibility(other, position, false))
            assertEquals(other, tracking.finish())
            assertEquals(60L, reads.get(other).activeSeconds)
        }

    @Test fun pausedLastPageResumesOnQualificationAndPromptsExactlyOnce() =
        runBlocking {
            val read = reads.setState(book, BookState.PAUSED, epoch)!!
            tracking.prepare(book)
            val end = ReadingPosition("end", 0.99, 100)
            tracking.accept(ReaderEvent.Visibility(book, end, true))
            elapsed = 60_000
            tracking.accept(ReaderEvent.Location(book, end))
            withTimeout(5_000) { tracking.state.first { it.finishPrompt } }
            assertEquals(BookState.READING, reads.get(book).state)
            assertEquals(read.id, reads.get(book).latest!!.id)
            assertTrue(reads.get(book).latest!!.finishPromptShown)
            tracking.dismissFinishPrompt()
            tracking.prepare(book)
            tracking.accept(ReaderEvent.Visibility(book, end, true))
            withTimeout(5_000) { tracking.state.first { it.active } }
            assertFalse(tracking.state.value.finishPrompt)
        }

    @Test fun finishPromptWaitsForTheConfiguredMinimum() =
        runBlocking {
            settings.update { it.copy(minimumSessionSeconds = 20) }
            tracking.prepare(book)
            val end = ReadingPosition("end", 0.99, 100)
            tracking.accept(ReaderEvent.Visibility(book, end, true))
            elapsed = 19_000
            tracking.accept(ReaderEvent.Location(book, end))
            tracking.navigation(100, false)
            elapsed = 20_000
            tracking.accept(ReaderEvent.Location(book, end))
            withTimeout(5_000) { tracking.state.first { it.finishPrompt } }
            assertEquals(20L, reads.get(book).activeSeconds)
        }
}
