package com.kmab.deft.tracking

import com.kmab.deft.core.model.ReadingPosition
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant

class SessionTrackerTest {
    private val clock = FakeClock()
    private var sequence = 0
    private val tracker = SessionTracker(clock) { "session-${++sequence}" }
    private val start = ReadingPosition("start", 0.0)

    @Test fun idleTailIsRemovedAndNextReadingStartsAnotherSession() {
        show()
        repeat(6) {
            clock.advance(30)
            tracker.interact(ReadingPosition("page-$it", it / 10.0), ReadingActivity.PAGE)
        }
        clock.advance(360)
        val ended = tracker.tick().single()
        assertEquals(180L, ended.activeSeconds)
        assertTrue(ended.finalized)
        assertFalse(tracker.active)
        tracker.interact(start, ReadingActivity.SELECTION)
        clock.advance(60)
        assertEquals(60L, tracker.hide().single().activeSeconds)
    }

    @Test fun fiftyNineSecondsIsDiscardedButSixtyQualifies() {
        show()
        clock.advance(59)
        assertEquals(59L, tracker.hide().single().activeSeconds)
        show()
        clock.advance(60)
        assertEquals(60L, tracker.hide().single().activeSeconds)
    }

    @Test fun checkpointsAreEveryThirtySecondsAndDoNotPersistUnconfirmedIdleTail() {
        show()
        clock.advance(29)
        assertTrue(tracker.tick().isEmpty())
        clock.advance(1)
        assertEquals(0L, tracker.tick().single().activeSeconds)
        tracker.interact(start, ReadingActivity.SCROLL)
        clock.advance(30)
        val saved = tracker.tick().single()
        assertEquals(30L, saved.activeSeconds)
        assertFalse(saved.finalized)
        assertEquals(1, saved.pagesTurned)
    }

    @Test fun leavingAndScreenOffEndImmediatelyAndNeverCountBackground() {
        show()
        clock.advance(90)
        assertEquals(90L, tracker.hide().single().activeSeconds)
        clock.advance(900)
        assertTrue(tracker.tick().isEmpty())
        assertTrue(tracker.hide().isEmpty())
        show()
        clock.advance(60)
        assertEquals(60L, tracker.hide().single().activeSeconds)
    }

    @Test fun repeatedVisibilityDoesNotRestartSessionOrResumeIdle() {
        show()
        clock.advance(90)
        show()
        assertEquals(90L, tracker.hide().single().activeSeconds)
        show()
        clock.advance(301)
        tracker.tick()
        show()
        assertFalse(tracker.active)
    }

    @Test fun wallClockChangesNeverInflateDurationOrReverseEndDate() {
        show()
        clock.wall = clock.wall.minusSeconds(10000)
        clock.advance(60)
        val session = tracker.hide().single()
        assertEquals(60L, session.activeSeconds)
        assertEquals(session.startedAt.plusSeconds(60), session.endedAt)
    }

    @Test fun offKeepsAccruingButReleasesAwakeAfterFifteenMinutesUntilReading() {
        show(idle = null)
        clock.advance(899)
        assertEquals(899L, tracker.tick().single().activeSeconds)
        assertTrue(tracker.keepAwake)
        clock.advance(1)
        assertTrue(tracker.tick().isEmpty())
        assertFalse(tracker.keepAwake)
        assertTrue(tracker.active)
        tracker.interact(start, ReadingActivity.SELECTION)
        assertTrue(tracker.keepAwake)
        clock.advance(60)
        assertEquals(960L, tracker.hide().single().activeSeconds)
    }

    @Test fun bookSwitchFinalizesOldBookAndSeparatesBrowsing() {
        show()
        clock.advance(60)
        val old = tracker.show(2, 9, true, start, 600_000).single()
        assertEquals(1L, old.bookId)
        clock.advance(90)
        val browsing = tracker.hide().single()
        assertEquals(2L, browsing.bookId)
        assertEquals(9L, browsing.readThroughId)
        assertTrue(browsing.browsing)
    }

    @Test fun rotationLikePauseResumeDoesNotDoubleCountAndCreatesSeparateStableIds() {
        show()
        clock.advance(60)
        val before = tracker.hide().single()
        clock.advance(2)
        show()
        clock.advance(60)
        val after = tracker.hide().single()
        assertNotEquals(before.id, after.id)
        assertEquals(120L, before.activeSeconds + after.activeSeconds)
    }

    @Test fun selectionRenewsActivityWithoutCountingPagesAndPositionsRemainIndependent() {
        show()
        clock.advance(60)
        tracker.interact(ReadingPosition("selected", 0.3, 14), ReadingActivity.SELECTION)
        clock.advance(60)
        tracker.interact(ReadingPosition("scrolled", 0.4, 19), ReadingActivity.SCROLL)
        val session = tracker.hide().single()
        assertEquals(1, session.pagesTurned)
        assertEquals(19, session.endPosition)
        assertEquals(120L, session.activeSeconds)
    }

    @Test fun interactionAtTimeoutDoesNotCountTheIdleGap() {
        show()
        clock.advance(60)
        tracker.interact(start, ReadingActivity.PAGE)
        clock.advance(300)
        val ended = tracker.interact(start, ReadingActivity.PAGE).single()
        assertEquals(60L, ended.activeSeconds)
        clock.advance(60)
        assertEquals(60L, tracker.hide().single().activeSeconds)
    }

    @Test fun fixedLayoutTenMinuteIdleAndZoomPanKeepReadingWithoutPageCount() {
        show(idle = 600_000)
        clock.advance(540)
        tracker.interact(start, ReadingActivity.ZOOM_PAN)
        clock.advance(540)
        assertTrue(tracker.tick().none { it.finalized })
        clock.advance(60)
        val end = tracker.tick().single()
        assertEquals(540L, end.activeSeconds)
        assertEquals(0, end.pagesTurned)
    }

    @Test fun locatorJumpsUpdateMetadataWithoutRenewingIdleOrCountingPages() {
        show()
        clock.advance(60)
        tracker.interact(start, ReadingActivity.SELECTION)
        clock.advance(240)
        tracker.position(ReadingPosition("jump", 0.99, 100))
        clock.advance(60)
        val end = tracker.tick().single()
        assertEquals(60L, end.activeSeconds)
        assertEquals(0, end.pagesTurned)
        assertEquals("jump", end.endLocator)
    }

    @Test fun checkpointsKeepStableIdentityAndStartZoneOffset() {
        show(idle = null)
        clock.advance(30)
        val first = tracker.tick().single()
        clock.advance(30)
        val second = tracker.tick().single()
        assertEquals(first.id, second.id)
        assertEquals(first.startedAt, second.startedAt)
        assertEquals(10800, second.zoneOffsetSeconds)
        assertEquals(60L, second.activeSeconds)
    }

    @Test fun changingTimeoutPreservesElapsedAndAppliesNewCutoffToLastReading() {
        show()
        clock.advance(60)
        tracker.interact(start, ReadingActivity.SELECTION)
        clock.advance(120)
        val ended = tracker.timeout(60_000).single()
        assertEquals(60L, ended.activeSeconds)
        assertFalse(tracker.active)
    }

    @Test fun switchingOffDoesNotResetLastInteractionOrElapsedTime() {
        show(idle = 3_600_000)
        clock.advance(901)
        tracker.timeout(null)
        assertFalse(tracker.keepAwake)
        assertEquals(901L, tracker.activeSeconds)
        assertEquals(901L, tracker.hide().single().activeSeconds)
    }

    @Test fun lastPageCanQualifyAtSixtyWithoutAnotherInteraction() {
        show()
        clock.advance(59)
        assertEquals(59L, tracker.activeSeconds)
        clock.advance(1)
        assertEquals(60L, tracker.activeSeconds)
        assertEquals(60L, tracker.hide().single().activeSeconds)
    }

    @Test fun changingFromOffRetainsTimeAlreadyValidUnderOffPolicy() {
        show(idle = null)
        clock.advance(600)
        tracker.tick()
        val ended = tracker.timeout(60_000).single()
        assertEquals(600L, ended.activeSeconds)
        assertTrue(ended.finalized)
    }

    private fun show(idle: Long? = 300_000) = tracker.show(1, null, false, start, idle)

    private class FakeClock : TrackingClock {
        var elapsed = 0L
        var wall = Instant.parse("2026-11-20T12:00:00Z")

        override fun elapsedMillis() = elapsed

        override fun instant() = wall

        override fun zoneOffsetSeconds() = 10800

        fun advance(seconds: Long) {
            elapsed += seconds * 1000
            wall = wall.plusSeconds(seconds)
        }
    }
}
