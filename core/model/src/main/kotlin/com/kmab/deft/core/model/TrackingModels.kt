package com.kmab.deft.core.model

import java.time.Instant

enum class BookState { NOT_STARTED, READING, FINISHED, PAUSED, ABANDONED }

enum class ReadThroughOutcome { IN_PROGRESS, FINISHED, PAUSED, ABANDONED }

enum class ReaderOpenChoice { RESUME, START_OVER, RE_READ, BROWSING }

data class ReadThrough(
    val id: Long,
    val bookId: Long,
    val startedAt: Instant,
    val endedAt: Instant?,
    val outcome: ReadThroughOutcome,
    val finishPromptShown: Boolean = false,
)

/** Cumulative session values. A checkpoint contains only observed, confirmed reading time. */
data class ReadingSession(
    val id: String,
    val bookId: Long,
    val readThroughId: Long?,
    val startedAt: Instant,
    val endedAt: Instant,
    val activeSeconds: Long,
    val startLocator: String? = null,
    val endLocator: String? = null,
    val startProgression: Double = 0.0,
    val endProgression: Double = 0.0,
    val pagesTurned: Int = 0,
    val startPosition: Int? = null,
    val endPosition: Int? = null,
    val browsing: Boolean = false,
    val zoneOffsetSeconds: Int = 0,
    val finalized: Boolean = false,
    val minimumSessionSeconds: Int = TrackingPreferences.DEFAULT_MINIMUM_SESSION_SECONDS,
) {
    init {
        require(id.isNotBlank())
        require(activeSeconds >= 0 && pagesTurned >= 0)
        require(startProgression in 0.0..1.0 && endProgression in 0.0..1.0)
        require(minimumSessionSeconds in TrackingPreferences.MINIMUM_SESSION_RANGE)
    }

    val qualifies: Boolean get() = activeSeconds >= minimumSessionSeconds
}

data class BookAssessment(
    val rating: Double? = null,
    val favorite: Boolean = false,
    val review: String? = null,
) {
    init {
        require(rating == null || (rating in MIN_RATING..MAX_RATING && rating * HALF_STARS % 1.0 == 0.0)) {
            "Choose a rating from 0.5 to 5 in half-star steps."
        }
    }

    private companion object {
        const val MIN_RATING = 0.5
        const val MAX_RATING = 5.0
        const val HALF_STARS = 2
    }
}

data class BookTrackingSnapshot(
    val bookId: Long,
    val readThroughs: List<ReadThrough> = emptyList(),
    val sessions: List<ReadingSession> = emptyList(),
    val assessment: BookAssessment = BookAssessment(),
) {
    val latest: ReadThrough? get() = readThroughs.maxByOrNull { it.id }
    val state: BookState get() =
        when (latest?.outcome) {
            null -> BookState.NOT_STARTED
            ReadThroughOutcome.IN_PROGRESS -> BookState.READING
            ReadThroughOutcome.FINISHED -> BookState.FINISHED
            ReadThroughOutcome.PAUSED -> BookState.PAUSED
            ReadThroughOutcome.ABANDONED -> BookState.ABANDONED
        }
    val activeSeconds: Long get() = countedSessions.sumOf { it.activeSeconds }
    val sessionCount: Int get() = countedSessions.size
    private val countedSessions get() = sessions.filter { !it.browsing && it.qualifies }
}

data class TrackingPreferences(
    val reflowableIdleMinutes: Int? = DEFAULT_REFLOWABLE_MINUTES,
    val fixedLayoutIdleMinutes: Int? = DEFAULT_FIXED_MINUTES,
    val showIndicator: Boolean = false,
    val celebrations: Boolean = true,
    val minimumSessionSeconds: Int = DEFAULT_MINIMUM_SESSION_SECONDS,
) {
    init {
        require(reflowableIdleMinutes == null || reflowableIdleMinutes in 1..MAX_IDLE_MINUTES)
        require(fixedLayoutIdleMinutes == null || fixedLayoutIdleMinutes in 1..MAX_IDLE_MINUTES)
        require(minimumSessionSeconds in MINIMUM_SESSION_RANGE)
    }

    companion object {
        const val DEFAULT_REFLOWABLE_MINUTES = 5
        const val DEFAULT_FIXED_MINUTES = 10
        const val MAX_IDLE_MINUTES = 60
        const val DEFAULT_MINIMUM_SESSION_SECONDS = 30
        val MINIMUM_SESSION_RANGE = 1..300
    }
}

data class BookTrackingSummary(
    val bookId: Long,
    val state: BookState = BookState.NOT_STARTED,
    val assessment: BookAssessment = BookAssessment(),
)
