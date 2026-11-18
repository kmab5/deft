package com.kmab.deft.core.designsystem

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.platform.AccessibilityManager
import androidx.compose.ui.platform.LocalAccessibilityManager
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class NoticeHostTest {
    @get:Rule val compose = createComposeRule()

    @Test fun actionOpensDetailsAndConsumesNotice() {
        val state = DeftNoticeState()
        var opened = false
        state.show("Scan complete", actionLabel = "scan details", onAction = { opened = true })
        compose.setContent { DeftTheme { DeftNoticeHost(state) } }
        compose.onNodeWithText("scan details").performClick()
        compose.runOnIdle {
            assertEquals(true, opened)
            assertEquals(0, state.visible.size)
        }
    }

    @Test fun closeDismissesErrorWithoutChangingTheScreen() {
        val state = DeftNoticeState()
        state.show("Pick a folder inside Books", DeftNoticeKind.Error)
        compose.setContent { DeftTheme { DeftNoticeHost(state) } }
        compose.onNodeWithText("Pick a folder inside Books").assertIsDisplayed()
        compose.onNodeWithContentDescription("dismiss notice").performClick()
        compose.runOnIdle { assertEquals(0, state.visible.size) }
    }

    @Test fun timeoutHonorsAccessibilityReadingTime() {
        val state = DeftNoticeState()
        state.show("Scan complete")
        val accessibility =
            object : AccessibilityManager {
                override fun calculateRecommendedTimeoutMillis(
                    originalTimeoutMillis: Long,
                    containsIcons: Boolean,
                    containsText: Boolean,
                    containsControls: Boolean,
                ) = 12_000L
            }
        compose.setContent {
            CompositionLocalProvider(LocalAccessibilityManager provides accessibility) {
                DeftTheme { DeftNoticeHost(state) }
            }
        }
        compose.waitForIdle()
        compose.mainClock.advanceTimeBy(7_000)
        compose.runOnIdle { assertEquals(1, state.visible.size) }
        compose.mainClock.advanceTimeBy(6_000)
        compose.runOnIdle { assertEquals(0, state.visible.size) }
    }

    @Test fun pausedNoticeGetsAFullReadingIntervalAfterInteraction() {
        val state = DeftNoticeState()
        val paused = mutableStateOf(true)
        state.show("Scan complete")
        compose.setContent { DeftTheme { DeftNoticeHost(state, paused = paused.value) } }
        compose.mainClock.advanceTimeBy(7_000)
        compose.runOnIdle { assertEquals(1, state.visible.size) }
        compose.runOnIdle { paused.value = false }
        compose.waitForIdle()
        compose.mainClock.advanceTimeBy(5_000)
        compose.runOnIdle { assertEquals(1, state.visible.size) }
        compose.mainClock.advanceTimeBy(2_000)
        compose.runOnIdle { assertEquals(0, state.visible.size) }
    }
}
