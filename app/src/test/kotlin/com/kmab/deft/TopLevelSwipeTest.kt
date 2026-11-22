package com.kmab.deft

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class TopLevelSwipeTest {
    @Test fun swipeMovesOneTabWithoutWrappingOrLeavingDetails() {
        assertEquals(1, topLevelSwipeTarget(0, -100f, 72f))
        assertEquals(0, topLevelSwipeTarget(1, 100f, 72f))
        assertEquals(2, topLevelSwipeTarget(1, -100f, 72f))
        assertNull(topLevelSwipeTarget(0, 100f, 72f))
        assertNull(topLevelSwipeTarget(2, -100f, 72f))
        assertNull(topLevelSwipeTarget(-1, -100f, 72f))
        assertNull(topLevelSwipeTarget(1, -40f, 72f))
    }
}
