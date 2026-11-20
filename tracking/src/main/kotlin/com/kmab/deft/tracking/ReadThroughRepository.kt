package com.kmab.deft.tracking

import androidx.room.withTransaction
import com.kmab.deft.core.database.AppDatabase
import com.kmab.deft.core.database.entity.ReadThroughEntity
import com.kmab.deft.core.model.BookAssessment
import com.kmab.deft.core.model.BookState
import com.kmab.deft.core.model.BookTrackingSnapshot
import com.kmab.deft.core.model.BookTrackingSummary
import com.kmab.deft.core.model.ReadThrough
import com.kmab.deft.core.model.ReadThroughOutcome
import com.kmab.deft.core.model.ReaderOpenChoice
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton

/** All state transitions and history edits share the database transaction with their sessions. */
@Singleton
class ReadThroughRepository
    @Inject
    constructor(
        private val database: AppDatabase,
    ) {
        private val dao get() = database.trackingDao()

        fun observe(bookId: Long): Flow<BookTrackingSnapshot> =
            database.invalidationTracker
                .createFlow("books", "read_throughs", "reading_sessions")
                .map { get(bookId) }
                .distinctUntilChanged()

        fun observeAll(): Flow<Map<Long, BookTrackingSummary>> =
            database.invalidationTracker
                .createFlow("books", "read_throughs")
                .map {
                    database.withTransaction {
                        val reads = dao.latestReadThroughs().groupBy { it.bookId }
                        dao.bookAssessments().associate { book ->
                            val state =
                                BookTrackingSnapshot(
                                    book.bookId,
                                    reads[book.bookId].orEmpty().map { it.model() },
                                ).state
                            book.bookId to
                                BookTrackingSummary(
                                    book.bookId,
                                    state,
                                    BookAssessment(book.rating, book.favorite, book.review),
                                )
                        }
                    }
                }.distinctUntilChanged()

        suspend fun get(bookId: Long): BookTrackingSnapshot =
            database.withTransaction {
                BookTrackingSnapshot(
                    bookId,
                    dao.getReadThroughs(bookId).map { it.model() },
                    dao.getSessions(bookId).map { it.model() }.filter { it.qualifies },
                    database.bookDao().getById(bookId)?.assessment() ?: BookAssessment(),
                )
            }

        suspend fun open(
            bookId: Long,
            choice: ReaderOpenChoice,
            at: Instant,
        ): ReadThrough =
            database.withTransaction {
                require(database.bookDao().getById(bookId)?.drmProtected == false) { "Protected books cannot be read." }
                val current =
                    requireNotNull(dao.getReadThroughs(bookId).firstOrNull()) { "This book has no read-through." }
                when (choice) {
                    ReaderOpenChoice.RESUME -> {
                        require(current.outcome == ReadThroughOutcome.ABANDONED)
                        current
                            .copy(
                                outcome = ReadThroughOutcome.IN_PROGRESS,
                                endedAt = null,
                            ).also { dao.updateReadThrough(it) }
                            .model()
                    }
                    ReaderOpenChoice.START_OVER -> {
                        require(current.outcome == ReadThroughOutcome.ABANDONED)
                        create(bookId, at).model()
                    }
                    ReaderOpenChoice.RE_READ -> {
                        require(current.outcome == ReadThroughOutcome.FINISHED)
                        create(bookId, at).model()
                    }
                    ReaderOpenChoice.BROWSING -> {
                        require(current.outcome == ReadThroughOutcome.FINISHED)
                        current.model()
                    }
                }
            }

        suspend fun setState(
            bookId: Long,
            state: BookState,
            at: Instant,
            confirmHistoryLoss: Boolean = false,
        ): ReadThrough? =
            database.withTransaction {
                requireNotNull(database.bookDao().getById(bookId)) { "Book no longer exists." }
                if (state == BookState.NOT_STARTED) {
                    require(confirmHistoryLoss) { "Confirm removing this book's reading history." }
                    dao.clearSessions(bookId)
                    dao.clearReadThroughs(bookId)
                    return@withTransaction null
                }
                val latest = dao.getReadThroughs(bookId).firstOrNull()
                val current =
                    if (latest == null ||
                        (latest.outcome == ReadThroughOutcome.FINISHED && state == BookState.READING)
                    ) {
                        create(bookId, at)
                    } else {
                        latest
                    }
                val outcome =
                    when (state) {
                        BookState.READING -> ReadThroughOutcome.IN_PROGRESS
                        BookState.PAUSED -> ReadThroughOutcome.PAUSED
                        BookState.ABANDONED -> ReadThroughOutcome.ABANDONED
                        BookState.FINISHED -> ReadThroughOutcome.FINISHED
                        BookState.NOT_STARTED -> error("Handled above")
                    }
                val end =
                    at.takeIf {
                        outcome == ReadThroughOutcome.FINISHED || outcome == ReadThroughOutcome.ABANDONED
                    }
                require(
                    end == null || !end.isBefore(current.startedAt),
                ) { "End date must be on or after the start date." }
                current.copy(outcome = outcome, endedAt = end).also { dao.updateReadThrough(it) }.model()
            }

        suspend fun editDates(
            readThroughId: Long,
            startedAt: Instant,
            endedAt: Instant?,
        ) = database.withTransaction {
            val current = requireNotNull(dao.readThrough(readThroughId)) { "Read-through no longer exists." }
            require(endedAt == null || !endedAt.isBefore(startedAt)) { "End date must be on or after the start date." }
            require(current.outcome != ReadThroughOutcome.FINISHED || endedAt != null) {
                "Finished read-throughs need a finish date."
            }
            require(current.outcome != ReadThroughOutcome.IN_PROGRESS || endedAt == null) {
                "An active read-through has no end date."
            }
            dao.updateReadThrough(current.copy(startedAt = startedAt, endedAt = endedAt))
        }

        suspend fun delete(readThroughId: Long) = database.withTransaction { dao.deleteReadThrough(readThroughId) }

        suspend fun claimFinishPrompt(readThroughId: Long): Boolean =
            database.withTransaction {
                dao.claimFinishPrompt(readThroughId) == 1
            }

        private suspend fun create(
            bookId: Long,
            at: Instant,
        ): ReadThroughEntity {
            val value = ReadThroughEntity(bookId = bookId, startedAt = at)
            return value.copy(id = dao.insertReadThrough(value))
        }
    }
