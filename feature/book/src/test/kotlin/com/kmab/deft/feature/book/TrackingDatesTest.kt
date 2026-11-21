package com.kmab.deft.feature.book

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant
import java.time.ZoneId

class TrackingDatesTest {
    @Test fun choosingTodayPreservesCurrentTimeInsteadOfMidnight() {
        val now = Instant.parse("2026-11-20T14:20:30Z")
        val today = now.atZone(ZoneId.systemDefault()).toLocalDate().toString()
        val finish = dateInstant(today, now)
        assertEquals(now, finish)
        assertTrue(finish.isAfter(now.minusSeconds(3600)))
    }

    @Test fun historicalEditRetainsLocalTime() {
        val original = Instant.parse("2026-11-20T14:20:30Z")
        val result = dateInstant("2026-11-12", original).atZone(ZoneId.systemDefault())
        assertEquals("2026-11-12", result.toLocalDate().toString())
        assertEquals(original.atZone(ZoneId.systemDefault()).toLocalTime(), result.toLocalTime())
    }
}
