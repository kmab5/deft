package com.kmab.deft.feature.reader

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class ReaderVisibilityTest {
    @Test fun loadingAndErrorsCannotStartTrackingEvenWhenResumed() {
        val changes = mutableListOf<Boolean>()
        val gate = ReaderVisibility(changes::add)
        gate.resumed(true)
        gate.rendered(false)
        assertFalse(gate.visible)
        assertEquals(emptyList<Boolean>(), changes)
        gate.rendered(true)
        gate.rendered(false)
        assertEquals(listOf(true, false), changes)
    }

    @Test fun repeatedLifecycleAndRenderingCallbacksDoNotDuplicateSessions() {
        val changes = mutableListOf<Boolean>()
        val gate = ReaderVisibility(changes::add)
        gate.rendered(true)
        gate.resumed(true)
        gate.resumed(true)
        gate.rendered(true)
        gate.resumed(false)
        gate.resumed(false)
        gate.rendered(false)
        gate.resumed(true)
        assertEquals(listOf(true, false), changes)
        gate.rendered(true)
        assertEquals(listOf(true, false, true), changes)
    }

    @Test fun sheetsAndPromptsStayPausedAcrossBackgroundAndResume() {
        val changes = mutableListOf<Boolean>()
        val gate = ReaderVisibility(changes::add)
        gate.resumed(true)
        gate.rendered(true)
        gate.obscured(true)
        gate.resumed(false)
        gate.resumed(true)
        assertEquals(listOf(true, false), changes)
        gate.obscured(false)
        assertEquals(listOf(true, false, true), changes)
    }
}
