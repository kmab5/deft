package com.kmab.deft.tracking

import com.kmab.deft.core.model.ReadingPosition
import com.kmab.deft.core.model.ReadingSession
import java.time.Instant
import java.util.UUID

interface TrackingClock {
    fun elapsedMillis(): Long

    fun instant(): Instant

    fun zoneOffsetSeconds(): Int
}

enum class ReadingActivity { PAGE, SCROLL, SELECTION, ZOOM_PAN }

/**
 * Deterministic session policy. Call serially with the timestamp of the original lifecycle/input
 * event, not the time its database write completes. Returned rows are cumulative, idempotent writes.
 * Checkpoints confirm only time through the last reading interaction when idle detection is enabled.
 * A clean exit confirms the short trailing dwell; an idle timeout retrospectively removes it.
 */
class SessionTracker(
    private val clock: TrackingClock,
    private val newId: () -> String = { UUID.randomUUID().toString() },
) {
    private data class Context(
        val bookId: Long,
        val readThroughId: Long?,
        val browsing: Boolean,
        var idleMillis: Long?,
        var position: ReadingPosition,
    )

    private data class OpenSession(
        val row: ReadingSession,
        val started: Long,
        var lastInteraction: Long,
        var checkpoint: Long,
        var end: ReadingPosition,
        var pages: Int = 0,
        var confirmedThrough: Long = started,
    )

    private var context: Context? = null
    private var current: OpenSession? = null
    val active: Boolean get() = current != null
    val activeSeconds: Long
        get() = current?.let { (clock.elapsedMillis() - it.started).coerceAtLeast(0) / MILLIS_PER_SECOND } ?: 0
    val keepAwake: Boolean
        get() =
            context?.idleMillis != null || current?.let {
                clock.elapsedMillis() - it.lastInteraction < OFF_AWAKE_MILLIS
            } == true

    fun show(
        bookId: Long,
        readThroughId: Long?,
        browsing: Boolean,
        position: ReadingPosition,
        idleMillis: Long?,
    ): List<ReadingSession> {
        require(idleMillis == null || idleMillis > 0)
        if (context?.bookId == bookId) return expire()
        val ended = hide()
        context = Context(bookId, readThroughId, browsing, idleMillis, position)
        start()
        return ended
    }

    fun timeout(idleMillis: Long?): List<ReadingSession> {
        require(idleMillis == null || idleMillis > 0)
        if (context?.idleMillis == null) current?.confirmedThrough = clock.elapsedMillis()
        context?.idleMillis = idleMillis
        return expire()
    }

    fun position(position: ReadingPosition) {
        context?.position = position
        current?.end = position
    }

    fun interact(
        position: ReadingPosition,
        activity: ReadingActivity,
    ): List<ReadingSession> {
        if (context == null) return emptyList()
        val ended = expire()
        position(position)
        if (current == null) start()
        current?.let {
            it.lastInteraction = clock.elapsedMillis()
            if (activity == ReadingActivity.PAGE || activity == ReadingActivity.SCROLL) it.pages++
        }
        return ended + checkpoint()
    }

    fun tick(): List<ReadingSession> = expire().ifEmpty { checkpoint() }

    fun hide(): List<ReadingSession> {
        val expired = expire()
        val remaining = current?.let { listOf(snapshot(it, clock.elapsedMillis(), finalized = true)) }.orEmpty()
        current = null
        context = null
        return expired + remaining
    }

    private fun start() {
        val book = context ?: return
        val now = clock.elapsedMillis()
        val instant = clock.instant()
        current =
            OpenSession(
                row =
                    ReadingSession(
                        id = newId(),
                        bookId = book.bookId,
                        readThroughId = book.readThroughId,
                        startedAt = instant,
                        endedAt = instant,
                        activeSeconds = 0,
                        startLocator = book.position.locatorJson,
                        startProgression = book.position.totalProgression,
                        startPosition = book.position.position,
                        browsing = book.browsing,
                        zoneOffsetSeconds = clock.zoneOffsetSeconds(),
                    ),
                started = now,
                lastInteraction = now,
                checkpoint = now,
                end = book.position,
            )
    }

    private fun expire(): List<ReadingSession> {
        val session = current
        val timeout = context?.idleMillis
        if (session == null || timeout == null || clock.elapsedMillis() - session.lastInteraction < timeout) {
            return emptyList()
        }
        current = null
        return listOf(snapshot(session, session.lastInteraction, finalized = true))
    }

    private fun checkpoint(): List<ReadingSession> {
        val session = current
        val now = clock.elapsedMillis()
        if (session == null || now - session.checkpoint < CHECKPOINT_MILLIS) return emptyList()
        session.checkpoint = now
        val confirmed = if (context?.idleMillis == null) now else session.lastInteraction
        session.confirmedThrough = maxOf(session.confirmedThrough, confirmed)
        return listOf(snapshot(session, confirmed, finalized = false))
    }

    private fun snapshot(
        session: OpenSession,
        through: Long,
        finalized: Boolean,
    ): ReadingSession {
        val duration = (maxOf(through, session.confirmedThrough) - session.started).coerceAtLeast(0)
        return session.row.copy(
            endedAt = session.row.startedAt.plusMillis(duration),
            activeSeconds = duration / MILLIS_PER_SECOND,
            endLocator = session.end.locatorJson,
            endProgression = session.end.totalProgression,
            endPosition = session.end.position,
            pagesTurned = session.pages,
            finalized = finalized,
        )
    }

    private companion object {
        const val MILLIS_PER_SECOND = 1000L
        const val CHECKPOINT_MILLIS = 30_000L
        const val OFF_AWAKE_MILLIS = 900_000L
    }
}
