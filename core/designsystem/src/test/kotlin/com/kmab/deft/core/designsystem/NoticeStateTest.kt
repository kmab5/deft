package com.kmab.deft.core.designsystem

import org.junit.Assert.assertEquals
import org.junit.Test

class NoticeStateTest {
    @Test fun overflowingNoticesQueueWithoutLosingErrors() {
        val state = DeftNoticeState()
        val first = state.show("one", DeftNoticeKind.Error)
        state.show("two")
        state.show("three")
        state.show("four", DeftNoticeKind.Error)
        assertEquals(listOf("one", "two", "three"), state.visible.map { it.message })
        state.dismiss(first)
        assertEquals(listOf("two", "three", "four"), state.visible.map { it.message })
    }

    @Test fun identicalErrorsCoalesceButSeparateActionsRemainAvailable() {
        val state = DeftNoticeState()
        val first = state.show("Pick a folder inside Books", DeftNoticeKind.Error)
        assertEquals(first, state.show("Pick a folder inside Books", DeftNoticeKind.Error))
        state.show("Scan complete", actionLabel = "details", onAction = {})
        state.show("Scan complete", actionLabel = "details", onAction = {})
        assertEquals(3, state.visible.size)
    }
}
