package com.kmab.deft.tracking

import androidx.room.withTransaction
import com.kmab.deft.core.database.AppDatabase
import com.kmab.deft.core.database.entity.ReadThroughEntity
import com.kmab.deft.core.model.ReadThroughOutcome
import com.kmab.deft.core.model.ReadingSession
import javax.inject.Inject
import javax.inject.Singleton

/** Stable session IDs make cumulative checkpoints idempotent, including after process recovery. */
@Singleton
class SessionRepository
    @Inject
    constructor(
        private val database: AppDatabase,
    ) {
        private val dao get() = database.trackingDao()

        suspend fun checkpoint(session: ReadingSession): ReadingSession =
            database.withTransaction {
                checkNotNull(persist(session.copy(finalized = false)))
            }

        suspend fun finish(session: ReadingSession): ReadingSession? =
            database.withTransaction {
                persist(session.copy(finalized = true))
            }

        suspend fun recoverInterrupted() =
            database.withTransaction {
                dao.discardInterruptedShortSessions()
                dao.finalizeInterruptedSessions()
            }

        suspend fun aggregateSeconds(): Long = dao.aggregateSeconds()

        private suspend fun persist(session: ReadingSession): ReadingSession? {
            val book = database.bookDao().getById(session.bookId) ?: throw SessionTargetRemovedException()
            require(!book.drmProtected) { "Protected books cannot have reading sessions." }
            val existing = findMatchingSession(session)
            val olderCheckpoint = !session.finalized && (existing?.activeSeconds ?: -1) > session.activeSeconds
            if (existing != null && (existing.finalized || olderCheckpoint)) {
                return existing
            }
            val latest =
                if (existing != null && existing.activeSeconds > session.activeSeconds) {
                    existing.copy(finalized = session.finalized)
                } else {
                    session
                }
            return if (latest.finalized && !latest.qualifies) {
                dao.deleteSession(latest.id)
                null
            } else {
                val linked = existing?.readThroughId ?: session.readThroughId
                val readId = if (latest.qualifies) resolveReadThrough(latest, linked) else linked
                latest.copy(readThroughId = readId).also { dao.upsertSession(it.entity()) }
            }
        }

        private suspend fun findMatchingSession(session: ReadingSession): ReadingSession? =
            dao.session(session.id)?.model()?.also { existing ->
                require(
                    existing.bookId == session.bookId &&
                        existing.startedAt.toEpochMilli() == session.startedAt.toEpochMilli() &&
                        existing.browsing == session.browsing &&
                        existing.minimumSessionSeconds == session.minimumSessionSeconds,
                ) {
                    "A session ID cannot be reused for a different reading session."
                }
            }

        private suspend fun resolveReadThrough(
            session: ReadingSession,
            linked: Long?,
        ): Long {
            if (linked != null) {
                val read = dao.readThrough(linked) ?: throw SessionTargetRemovedException()
                require(read.bookId == session.bookId) { "Read-through belongs to another book." }
                require(!session.browsing || read.outcome == ReadThroughOutcome.FINISHED) {
                    "Browsing requires a finished read-through."
                }
                if (!session.browsing && read.outcome == ReadThroughOutcome.PAUSED) {
                    dao.updateReadThrough(read.copy(outcome = ReadThroughOutcome.IN_PROGRESS, endedAt = null))
                }
                return linked
            }
            require(!session.browsing) { "Choose a finished read-through before browsing." }
            val latest = dao.getReadThroughs(session.bookId).firstOrNull()
            return when (latest?.outcome) {
                null -> dao.insertReadThrough(ReadThroughEntity(bookId = session.bookId, startedAt = session.startedAt))
                ReadThroughOutcome.IN_PROGRESS -> latest.id
                ReadThroughOutcome.PAUSED -> {
                    dao.updateReadThrough(latest.copy(outcome = ReadThroughOutcome.IN_PROGRESS, endedAt = null))
                    latest.id
                }
                ReadThroughOutcome.ABANDONED, ReadThroughOutcome.FINISHED -> errorChoice()
            }
        }

        private fun errorChoice(): Nothing =
            throw IllegalArgumentException("Choose how to open this book before recording reading.")
    }

/** Intentional history removal is terminal; a retry must not resurrect the removed target. */
class SessionTargetRemovedException : IllegalStateException("The book or read-through was explicitly removed.")
