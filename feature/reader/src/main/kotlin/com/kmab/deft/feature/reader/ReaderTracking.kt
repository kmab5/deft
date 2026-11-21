package com.kmab.deft.feature.reader

import android.os.SystemClock
import android.util.Log
import com.kmab.deft.core.datastore.TrackingSettingsRepository
import com.kmab.deft.core.model.BookState
import com.kmab.deft.core.model.BookTrackingSnapshot
import com.kmab.deft.core.model.ReaderOpenChoice
import com.kmab.deft.core.model.ReadingPosition
import com.kmab.deft.core.model.ReadingSession
import com.kmab.deft.core.model.TrackingPreferences
import com.kmab.deft.tracking.ReadThroughRepository
import com.kmab.deft.tracking.ReadingActivity
import com.kmab.deft.tracking.SessionRepository
import com.kmab.deft.tracking.SessionTargetRemovedException
import com.kmab.deft.tracking.SessionTracker
import com.kmab.deft.tracking.TrackingClock
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.ZoneId
import javax.inject.Inject
import javax.inject.Singleton

internal data class ReaderTrackingState(
    val active: Boolean = false,
    val keepAwake: Boolean = true,
    val finishPrompt: Boolean = false,
    val idle: Boolean = false,
)

/** Application-owned FIFO: lifecycle timestamps are captured before asynchronous database work. */
@Singleton
// One coordinator orders lifecycle/repository mutations and keeps its actor alive for retryable failures.
@Suppress("TooManyFunctions", "TooGenericExceptionCaught")
internal class ReaderTracking(
    private val sessions: SessionRepository,
    private val readThroughs: ReadThroughRepository,
    private val settings: TrackingSettingsRepository,
    private val scope: CoroutineScope,
    private val now: () -> ReadingTime,
) {
    @Inject
    constructor(
        sessions: SessionRepository,
        readThroughs: ReadThroughRepository,
        settings: TrackingSettingsRepository,
    ) : this(
        sessions,
        readThroughs,
        settings,
        CoroutineScope(SupervisorJob() + Dispatchers.IO),
        ReadingTime::now,
    )

    private data class Work(
        val time: ReadingTime,
        val action: suspend () -> Unit,
    )

    private val queue = Channel<Work>(Channel.UNLIMITED)
    private val clock = EventClock()
    private val tracker = SessionTracker(clock)
    private val mutableState = MutableStateFlow(ReaderTrackingState())
    val state = mutableState.asStateFlow()
    private val errors = MutableSharedFlow<String>(extraBufferCapacity = 1)
    val failures = errors.asSharedFlow()
    private val pending = linkedMapOf<String, ReadingSession>()
    private val sessionReadThroughs = mutableMapOf<String, Long>()
    private var preferences = TrackingPreferences()
    private var bookId: Long? = null
    private var readThroughId: Long? = null
    private var browsing = false
    private var gated = false
    private var qualificationNeeded = false
    private var readerVisible = false
    private var awaitingEndPrompt = false
    private var fixedLayout = false
    private var totalPositions = 0
    private var position: ReadingPosition? = null
    private var claimedPrompt: Long? = null

    init {
        scope.launch {
            try {
                sessions.recoverInterrupted()
            } catch (cancel: CancellationException) {
                throw cancel
            } catch (error: Exception) {
                failed("Reading history could not be recovered.", error)
            }
            for (work in queue) {
                clock.time = work.time
                try {
                    work.action()
                } catch (cancel: CancellationException) {
                    throw cancel
                } catch (error: Exception) {
                    failed("Reading history could not be updated.", error)
                }
                publish()
            }
        }
        scope.launch {
            settings.preferences.collect { value ->
                enqueue {
                    preferences = value
                    save(tracker.timeout(idleMillis(), preferences.minimumSessionSeconds))
                }
            }
        }
        scope.launch {
            while (true) {
                delay(TICK_MILLIS)
                enqueue {
                    save(tracker.tick())
                    maybePrompt()
                }
            }
        }
    }

    suspend fun prepare(id: Long): BookTrackingSnapshot =
        request {
            save(tracker.hide())
            val snapshot = readThroughs.get(id)
            preferences = settings.preferences.first()
            bookId = id
            readThroughId = snapshot.latest?.id
            browsing = false
            gated = requiresReadingChoice(snapshot.state)
            qualificationNeeded = snapshot.state == BookState.PAUSED || snapshot.state == BookState.NOT_STARTED
            position = null
            readerVisible = false
            awaitingEndPrompt = false
            claimedPrompt = null
            mutableState.value = ReaderTrackingState()
            snapshot
        }

    fun navigation(
        positions: Int,
        fixed: Boolean,
    ) = enqueue {
        totalPositions = positions
        fixedLayout = fixed
    }

    suspend fun choose(choice: ReaderOpenChoice) =
        request {
            val id = checkNotNull(bookId)
            val readThrough = readThroughs.open(id, choice, clock.instant())
            readThroughId = readThrough.id
            browsing = choice == ReaderOpenChoice.BROWSING
            qualificationNeeded = false
            gated = false
        }

    fun accept(event: ReaderEvent) =
        enqueue {
            if (event.bookId != bookId || gated) return@enqueue
            if (event is ReaderEvent.Visibility) {
                readerVisible = event.visible
                if (!readerVisible) awaitingEndPrompt = false
            }
            event.position?.let { position = it }
            val writes =
                when (event) {
                    is ReaderEvent.Visibility -> trackVisibility(event)
                    is ReaderEvent.PageTurn -> tracker.interact(event.position, ReadingActivity.PAGE)
                    is ReaderEvent.ScrollStep -> tracker.interact(event.position, ReadingActivity.SCROLL)
                    is ReaderEvent.Selection -> tracker.interact(event.position, ReadingActivity.SELECTION)
                    is ReaderEvent.Resume -> resumeReading(event)
                    is ReaderEvent.Location -> {
                        tracker.position(event.position)
                        emptyList()
                    }
                }
            save(writes)
            maybePrompt()
        }

    private fun trackVisibility(event: ReaderEvent.Visibility): List<ReadingSession> =
        if (event.visible && !state.value.finishPrompt) {
            event.position?.let { show(event.bookId, it) }.orEmpty()
        } else {
            tracker.hide()
        }

    private fun resumeReading(event: ReaderEvent.Resume): List<ReadingSession> =
        if (readerVisible && tracker.idle && !state.value.finishPrompt) {
            tracker.interact(event.position, ReadingActivity.RESUME)
        } else {
            emptyList()
        }

    fun dismissFinishPrompt() =
        enqueue {
            mutableState.value = mutableState.value.copy(finishPrompt = false)
        }

    suspend fun finish(): Long =
        request {
            save(tracker.hide())
            check(pending.isEmpty()) { "Reading time could not be saved." }
            val id = checkNotNull(bookId)
            readThroughs.setState(id, BookState.FINISHED, clock.instant())
            gated = true
            mutableState.value = mutableState.value.copy(finishPrompt = false)
            id
        }

    private suspend fun maybePrompt() {
        val location = position
        val canPrompt = readerVisible && (tracker.active || awaitingEndPrompt)
        val unavailable = gated || browsing || !canPrompt
        val reachedEnd = location?.let { reachedReadingEnd(it.totalProgression, it.position, totalPositions) } == true
        if (unavailable || state.value.finishPrompt || !reachedEnd) return
        if (qualificationNeeded && tracker.qualifiesActiveSession) {
            // The prompt obscures reading. Close the qualifying visible dwell before claiming it.
            awaitingEndPrompt = true
            save(tracker.hide())
        }
        if (qualificationNeeded) return
        readThroughId?.takeUnless { it == claimedPrompt }?.let { claimPrompt(it) }
    }

    private suspend fun claimPrompt(id: Long) {
        val claimed = readThroughs.claimFinishPrompt(id)
        claimedPrompt = id
        if (claimed) {
            save(tracker.hide())
            mutableState.value = mutableState.value.copy(finishPrompt = true)
        } else if (awaitingEndPrompt) {
            // A paused read-through may already have declined its prompt before it was paused.
            position?.let { show(checkNotNull(bookId), it) }
        }
        awaitingEndPrompt = false
    }

    private suspend fun save(rows: List<ReadingSession>) {
        rows.forEach { pending[it.id] = it }
        for (row in pending.values.toList()) {
            val bound = row.copy(readThroughId = row.readThroughId ?: sessionReadThroughs[row.id])
            try {
                val saved = if (bound.finalized) sessions.finish(bound) else sessions.checkpoint(bound)
                acceptSavedSession(bound, saved)
            } catch (_: SessionTargetRemovedException) {
                retire(row)
            } catch (cancel: CancellationException) {
                throw cancel
            } catch (error: Exception) {
                failed("Reading history could not be saved. Deft will retry.", error)
                break
            }
        }
    }

    private fun acceptSavedSession(
        row: ReadingSession,
        saved: ReadingSession?,
    ) {
        saved?.readThroughId?.let { sessionReadThroughs[row.id] = it }
        val assigned = saved?.readThroughId?.takeIf { row.bookId == bookId }
        readThroughId = assigned ?: readThroughId
        val qualified = assigned != null && saved?.qualifies == true
        qualificationNeeded = qualificationNeeded && !qualified
        pending.remove(row.id)
        if (row.finalized) sessionReadThroughs.remove(row.id)
    }

    private fun retire(row: ReadingSession) {
        pending.remove(row.id)
        sessionReadThroughs.remove(row.id)
        if (row.bookId == bookId) {
            tracker.hide()
            gated = true
        }
    }

    private fun idleMillis(): Long? {
        val minutes = if (fixedLayout) preferences.fixedLayoutIdleMinutes else preferences.reflowableIdleMinutes
        return minutes?.toLong()?.times(MINUTE_MILLIS)
    }

    private fun show(
        book: Long,
        location: ReadingPosition,
    ): List<ReadingSession> =
        tracker.show(book, readThroughId, browsing, location, idleMillis(), preferences.minimumSessionSeconds)

    private fun publish() {
        mutableState.value =
            mutableState.value.copy(
                active = tracker.active,
                keepAwake = tracker.keepAwake,
                idle = readerVisible && tracker.idle && !gated && !mutableState.value.finishPrompt,
            )
    }

    private fun failed(
        message: String,
        cause: Exception,
    ) {
        Log.w(TAG, message, cause)
        errors.tryEmit(message)
    }

    private fun enqueue(action: suspend () -> Unit) {
        check(queue.trySend(Work(now(), action)).isSuccess)
    }

    // Repository/provider failures propagate to the caller's notice handler; cancellation remains cancellation.
    @Suppress("TooGenericExceptionCaught")
    private suspend fun <T> request(action: suspend () -> T): T {
        val result = CompletableDeferred<T>()
        enqueue {
            try {
                result.complete(action())
            } catch (cancel: CancellationException) {
                result.cancel(cancel)
                throw cancel
            } catch (error: Exception) {
                Log.w(TAG, "Reading history action failed.", error)
                result.completeExceptionally(error)
            }
        }
        return result.await()
    }

    private companion object {
        const val MINUTE_MILLIS = 60_000L
        const val TICK_MILLIS = 1_000L
        const val TAG = "ReaderTracking"
    }
}

internal data class ReadingTime(
    val elapsed: Long,
    val instant: Instant,
    val offset: Int,
) {
    companion object {
        fun now(): ReadingTime {
            val instant = Instant.now()
            val offset =
                ZoneId
                    .systemDefault()
                    .rules
                    .getOffset(instant)
                    .totalSeconds
            return ReadingTime(SystemClock.elapsedRealtime(), instant, offset)
        }
    }
}

private class EventClock : TrackingClock {
    var time = ReadingTime.now()

    override fun elapsedMillis() = time.elapsed

    override fun instant() = time.instant

    override fun zoneOffsetSeconds() = time.offset
}
