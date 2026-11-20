package com.kmab.deft.tracking

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.kmab.deft.core.database.AppDatabase
import com.kmab.deft.core.database.entity.BookEntity
import com.kmab.deft.core.model.BookAssessment
import com.kmab.deft.core.model.BookFormat
import com.kmab.deft.core.model.BookState
import com.kmab.deft.core.model.ReaderOpenChoice
import com.kmab.deft.core.model.ReadingSession
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.time.Instant

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class TrackingRepositoryTest {
    private lateinit var db: AppDatabase
    private lateinit var reads: ReadThroughRepository
    private lateinit var sessions: SessionRepository
    private var book = 0L
    private val now = Instant.ofEpochSecond(1000)

    @Before fun setup() =
        runBlocking {
            db =
                Room
                    .inMemoryDatabaseBuilder(
                        ApplicationProvider.getApplicationContext(),
                        AppDatabase::class.java,
                    ).build()
            reads = ReadThroughRepository(db)
            sessions = SessionRepository(db)
            book =
                db.bookDao().insert(
                    BookEntity(
                        contentHash = "hash",
                        format = BookFormat.EPUB,
                        title = "Book",
                        sortTitle = "book",
                        addedAt = now,
                    ),
                )
        }

    @After fun close() = db.close()

    private fun session(
        id: String = "session",
        seconds: Long = 60,
        read: Long? = null,
        browsing: Boolean = false,
    ) = ReadingSession(id, book, read, now, now.plusSeconds(seconds), seconds, browsing = browsing)

    @Test fun firstQualifyingSessionCreatesAttemptAndCheckpointIsIdempotent() =
        runBlocking {
            sessions.checkpoint(session(seconds = 29))
            assertEquals(BookState.NOT_STARTED, reads.get(book).state)
            val saved = sessions.checkpoint(session(seconds = 30))
            assertNotNull(saved.readThroughId)
            sessions.checkpoint(session())
            sessions.checkpoint(session())
            assertEquals(1, reads.get(book).readThroughs.size)
            assertEquals(60L, sessions.aggregateSeconds())
            sessions.finish(session(seconds = 90))
            sessions.checkpoint(session(seconds = 60))
            assertEquals(90L, reads.get(book).activeSeconds)
        }

    @Test fun cumulativeCheckpointsMatchAcrossRoomsMillisecondTimestampRoundTrip() =
        runBlocking {
            val startedAt = now.plusNanos(123_456_789)
            val first = session(seconds = 60).copy(startedAt = startedAt)
            sessions.checkpoint(first)
            sessions.checkpoint(first.copy(activeSeconds = 90, endedAt = startedAt.plusSeconds(90)))
            sessions.finish(first.copy(activeSeconds = 120, endedAt = startedAt.plusSeconds(120)))

            val saved = reads.get(book).sessions.single()
            assertEquals(120L, saved.activeSeconds)
            assertTrue(saved.finalized)
        }

    @Test fun aLateShortFinalizationKeepsTheAlreadyQualifiedCheckpoint() =
        runBlocking {
            sessions.checkpoint(session(seconds = 45))
            sessions.finish(session(seconds = 20))

            val saved = reads.get(book).sessions.single()
            assertEquals(45L, saved.activeSeconds)
            assertTrue(saved.finalized)
        }

    @Test fun pausedAndAbandonedResumeButStartOverCreatesNewAttempt() =
        runBlocking {
            val first = sessions.finish(session())!!.readThroughId
            reads.setState(book, BookState.PAUSED, now.plusSeconds(100))
            sessions.finish(session("second"))
            assertEquals(first, reads.get(book).latest!!.id)
            assertEquals(BookState.READING, reads.get(book).state)
            reads.setState(book, BookState.ABANDONED, now.plusSeconds(100))
            assertEquals(first, reads.open(book, ReaderOpenChoice.RESUME, now.plusSeconds(200)).id)
            reads.setState(book, BookState.ABANDONED, now.plusSeconds(300))
            assertNotEquals(first, reads.open(book, ReaderOpenChoice.START_OVER, now.plusSeconds(400)).id)
            assertEquals(2, reads.get(book).readThroughs.size)
        }

    @Test fun finishedBrowsingPreservesOutcomeAndIsExcludedWithIgnoredBooks() =
        runBlocking {
            sessions.finish(session())
            val finished = reads.setState(book, BookState.FINISHED, now.plusSeconds(100))!!
            val browsing = reads.open(book, ReaderOpenChoice.BROWSING, now.plusSeconds(200))
            sessions.finish(session("browse", 120, browsing.id, true))
            assertEquals(BookState.FINISHED, reads.get(book).state)
            assertEquals(60L, sessions.aggregateSeconds())
            assertEquals(2, reads.get(book).sessions.size)
            assertEquals(1, reads.get(book).sessionCount)
            db.bookDao().setIgnored(book, true)
            assertEquals(0L, sessions.aggregateSeconds())
            assertEquals(2, reads.get(book).sessions.size)
            db.bookDao().setIgnored(book, false)
            assertEquals(60L, sessions.aggregateSeconds())
            assertNotEquals(finished.id, reads.open(book, ReaderOpenChoice.RE_READ, now.plusSeconds(300)).id)
        }

    @Test fun datesPromptClaimsAndDeletingLatestAreDurable() =
        runBlocking {
            val first = reads.setState(book, BookState.FINISHED, now)!!
            assertEquals(now, first.endedAt)
            val second = reads.open(book, ReaderOpenChoice.RE_READ, now.plusSeconds(100))
            assertTrue(reads.claimFinishPrompt(second.id))
            assertFalse(ReadThroughRepository(db).claimFinishPrompt(second.id))
            reads.editDates(second.id, Instant.EPOCH, null)
            assertEquals(second.id, reads.get(book).latest!!.id)
            assertEquals(BookState.READING, reads.observeAll().first()[book]!!.state)
            assertThrows(IllegalArgumentException::class.java) {
                runBlocking { reads.editDates(first.id, now, now.minusSeconds(1)) }
            }
            reads.delete(second.id)
            assertEquals(BookState.FINISHED, reads.get(book).state)
            assertThrows(
                IllegalArgumentException::class.java,
            ) { runBlocking { reads.setState(book, BookState.NOT_STARTED, now) } }
            reads.setState(book, BookState.NOT_STARTED, now, confirmHistoryLoss = true)
            assertEquals(BookState.NOT_STARTED, reads.get(book).state)
        }

    @Test fun crashRecoveryNeverAddsTimeAndForgetCascadesHistory() =
        runBlocking {
            sessions.checkpoint(session("short", 39).copy(minimumSessionSeconds = 40))
            sessions.checkpoint(session("kept", 20).copy(minimumSessionSeconds = 20))
            sessions.recoverInterrupted()
            val snapshot = reads.get(book)
            assertEquals(listOf("kept"), snapshot.sessions.map { it.id })
            assertTrue(snapshot.sessions.single().finalized)
            assertEquals(20L, snapshot.activeSeconds)
            assertEquals(20L, sessions.aggregateSeconds())
            db.bookDao().forget(book)
            assertTrue(reads.get(book).sessions.isEmpty())
            assertTrue(reads.get(book).readThroughs.isEmpty())
        }

    @Test fun manualStatesCreateOrReuseReadThroughAndKeepSessions() =
        runBlocking {
            val started = reads.setState(book, BookState.READING, now)!!
            sessions.finish(session(read = started.id))
            listOf(BookState.PAUSED, BookState.ABANDONED, BookState.READING, BookState.FINISHED).forEach { state ->
                val updated = reads.setState(book, state, now.plusSeconds(120))!!
                assertEquals(started.id, updated.id)
                assertEquals(state, reads.get(book).state)
                assertEquals(1, reads.get(book).sessions.size)
            }
            assertThrows(IllegalArgumentException::class.java) {
                runBlocking { reads.editDates(started.id, now, null) }
            }
            val reread = reads.setState(book, BookState.READING, now.plusSeconds(200))!!
            assertNotEquals(started.id, reread.id)
            reads.delete(started.id)
            assertTrue(reads.get(book).sessions.isEmpty())
            assertEquals(BookState.READING, reads.get(book).state)
        }

    @Test fun unchosenFinishedOrAbandonedReadingCannotMutateHistory() =
        runBlocking<Unit> {
            reads.setState(book, BookState.FINISHED, now)
            assertThrows(IllegalArgumentException::class.java) { runBlocking { sessions.finish(session()) } }
            reads.setState(book, BookState.ABANDONED, now)
            assertThrows(IllegalArgumentException::class.java) { runBlocking { sessions.finish(session()) } }
            assertThrows(IllegalArgumentException::class.java) {
                runBlocking { reads.open(book, ReaderOpenChoice.BROWSING, now) }
            }
        }

    @Test fun shortVisitsNeitherCreateHistoryNorResumePausedReadThrough() =
        runBlocking {
            assertNull(sessions.finish(session(seconds = 29)))
            assertEquals(BookState.NOT_STARTED, reads.get(book).state)
            reads.setState(book, BookState.PAUSED, now)
            assertNull(sessions.finish(session("paused-short", 89).copy(minimumSessionSeconds = 90)))
            assertEquals(BookState.PAUSED, reads.get(book).state)
            assertTrue(reads.get(book).sessions.isEmpty())
        }

    @Test fun raisingMinimumKeepsPreviouslyAcceptedHistoryAndDiscardsNewShortSessions() =
        runBlocking {
            sessions.finish(session("first", 1).copy(minimumSessionSeconds = 1))
            assertNull(sessions.finish(session("second", 299).copy(minimumSessionSeconds = 300)))
            sessions.finish(session("third", 300).copy(minimumSessionSeconds = 300))

            val snapshot = reads.get(book)
            assertEquals(setOf("first", "third"), snapshot.sessions.map { it.id }.toSet())
            assertEquals(301L, snapshot.activeSeconds)
            assertEquals(301L, sessions.aggregateSeconds())
        }

    @Test fun concurrentEndEventsClaimOnlyOnePrompt() =
        runBlocking {
            val read = reads.setState(book, BookState.READING, now)!!
            val claims = coroutineScope { (1..8).map { async { reads.claimFinishPrompt(read.id) } }.awaitAll() }
            assertEquals(1, claims.count { it })
        }

    @Test fun independentAssessmentEditsKeepEachOthersFields() =
        runBlocking {
            val ratings = RatingRepository(db)
            coroutineScope {
                listOf(
                    async { ratings.update(book) { it.copy(rating = 4.5) } },
                    async { ratings.update(book) { it.copy(favorite = true) } },
                    async { ratings.update(book) { it.copy(review = "kept") } },
                ).awaitAll()
            }
            assertEquals(BookAssessment(4.5, true, "kept"), reads.get(book).assessment)
            assertEquals(BookAssessment(4.5, true, "kept"), reads.observeAll().first()[book]!!.assessment)
        }

    @Test fun ratingsAndPrivateReviewPersistOnMissingProtectedBooks() =
        runBlocking<Unit> {
            val existing = db.bookDao().getById(book)!!
            db.bookDao().update(existing.copy(drmProtected = true))
            val assessment = BookAssessment(4.5, true, "My private review")
            RatingRepository(db).update(book, assessment)
            assertEquals(assessment, reads.get(book).assessment)
            assertThrows(IllegalArgumentException::class.java) { runBlocking { sessions.finish(session()) } }
        }
}
