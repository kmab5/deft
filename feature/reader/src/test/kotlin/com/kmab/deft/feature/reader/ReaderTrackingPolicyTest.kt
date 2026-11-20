package com.kmab.deft.feature.reader

import com.kmab.deft.core.model.BookState
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ReaderTrackingPolicyTest {
    @Test fun onlyFinishedAndAbandonedRequireAChoiceBeforeTracking() {
        assertTrue(requiresReadingChoice(BookState.FINISHED))
        assertTrue(requiresReadingChoice(BookState.ABANDONED))
        assertFalse(requiresReadingChoice(BookState.READING))
        assertFalse(requiresReadingChoice(BookState.PAUSED))
        assertFalse(requiresReadingChoice(BookState.NOT_STARTED))
    }

    @Test fun endThresholdRequiresNinetyNinePercentOrLastKnownPosition() {
        assertFalse(reachedReadingEnd(0.989, 98, 100))
        assertTrue(reachedReadingEnd(0.99, null, 100))
        assertTrue(reachedReadingEnd(0.98, 100, 100))
        assertFalse(reachedReadingEnd(0.0, null, 0))
        assertFalse(reachedReadingEnd(0.5, 1, 100))
    }
}
