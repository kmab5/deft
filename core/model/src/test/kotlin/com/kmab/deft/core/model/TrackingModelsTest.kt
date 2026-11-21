package com.kmab.deft.core.model

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.time.Instant

class TrackingModelsTest {
    @Test fun `assessment accepts only half stars or absent`() {
        assertEquals(null, BookAssessment().rating)
        (1..10).forEach { assertEquals(it / 2.0, BookAssessment(rating = it / 2.0).rating) }
        listOf(0.0, 0.2, 4.9, 5.5, Double.NaN).forEach { rating ->
            assertThrows(IllegalArgumentException::class.java) { BookAssessment(rating = rating) }
        }
    }

    @Test fun `timeouts validate whole minutes and off`() {
        assertEquals(5, TrackingPreferences().reflowableIdleMinutes)
        assertEquals(10, TrackingPreferences().fixedLayoutIdleMinutes)
        assertTrue(TrackingPreferences().celebrations)
        assertFalse(TrackingPreferences().showIndicator)
        assertEquals(null, TrackingPreferences(reflowableIdleMinutes = null).reflowableIdleMinutes)
        listOf(0, 61, -1).forEach { timeout ->
            assertThrows(IllegalArgumentException::class.java) { TrackingPreferences(fixedLayoutIdleMinutes = timeout) }
        }
    }

    @Test fun `latest means attempt order not editable dates`() {
        val latest = ReadThrough(2, 1, Instant.EPOCH, null, ReadThroughOutcome.PAUSED)
        val old = ReadThrough(1, 1, Instant.ofEpochSecond(100), null, ReadThroughOutcome.IN_PROGRESS)
        assertEquals(BookState.NOT_STARTED, BookTrackingSnapshot(1).state)
        assertEquals(BookState.PAUSED, BookTrackingSnapshot(1, listOf(old, latest)).state)
    }

    @Test fun `minimum session duration accepts one through three hundred seconds`() {
        listOf(1, 300).forEach { TrackingPreferences(minimumSessionSeconds = it) }
        listOf(0, 301).forEach { seconds ->
            assertThrows(IllegalArgumentException::class.java) {
                TrackingPreferences(minimumSessionSeconds = seconds)
            }
        }
    }
}
