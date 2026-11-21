package com.kmab.deft.core.designsystem

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.AccessibilityManager
import androidx.compose.ui.platform.LocalAccessibilityManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.github.takahirom.roborazzi.ExperimentalRoborazziApi
import com.github.takahirom.roborazzi.RoborazziOptions
import com.github.takahirom.roborazzi.RoborazziTaskType
import com.github.takahirom.roborazzi.captureRoboImage
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36], qualifiers = "w393dp-h852dp-mdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class NoticeHostTest {
    @get:Rule val compose = createComposeRule()

    @After fun restoreFontScale() = RuntimeEnvironment.setFontScale(1f)

    @Test fun inkNoticesSeparateMessageActionAndDismiss() = noticeLayout("notices-ink", true, 1f)

    @Test fun largePaperNoticesWrapWithoutCrampingTheActionOrDismiss() =
        noticeLayout(
            "notices-paper-large",
            false,
            1.5f,
        )

    @OptIn(ExperimentalRoborazziApi::class)
    private fun noticeLayout(
        name: String,
        dark: Boolean,
        fontScale: Float,
    ) {
        RuntimeEnvironment.setFontScale(fontScale)
        val state = DeftNoticeState()
        val message = "Scan complete · added 42 · updated 6 · 2 files need attention."
        state.show(message, DeftNoticeKind.Success, "scan details", {})
        state.show("One folder needs access again.", DeftNoticeKind.Error)
        compose.setContent {
            DeftTheme(dark) {
                Box(Modifier.fillMaxSize().background(LocalDeftColors.current.bg).testTag("notice_reference")) {
                    DeftNoticeHost(state, Modifier.align(Alignment.BottomCenter), paused = true)
                }
            }
        }
        compose.onNodeWithText(message).assertIsDisplayed()
        compose.onNodeWithText("One folder needs access again.").assertIsDisplayed()
        val text = compose.onNodeWithText(message, useUnmergedTree = true).fetchSemanticsNode().boundsInRoot
        val action = compose.onNodeWithText("scan details", useUnmergedTree = true).fetchSemanticsNode().boundsInRoot
        assertTrue("Action has its own row below the complete message", action.top >= text.bottom)
        assertTrue("Action follows the message without an empty header row", action.top - text.bottom <= 16f)
        assertEquals("Action text aligns with the message", text.left, action.left, 1f)
        val actionTarget = compose.onNodeWithText("scan details").fetchSemanticsNode().boundsInRoot
        assertTrue("Compact notices retain a 48dp action target", actionTarget.height >= 48f)
        val dismiss = compose.onAllNodesWithContentDescription("dismiss notice").fetchSemanticsNodes()
        assertEquals(2, dismiss.size)
        dismiss.forEach {
            assertTrue("Dismiss target is at least 48dp wide", it.boundsInRoot.width >= 48f)
            assertTrue("Dismiss target is at least 48dp high", it.boundsInRoot.height >= 48f)
        }
        val output = File("build/reports/screenshots/$name.png").apply { parentFile?.mkdirs() }
        compose.onNodeWithTag("notice_reference").captureRoboImage(
            output.path,
            roborazziOptions = RoborazziOptions(taskType = RoborazziTaskType.Record),
        )
    }

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

    @Test fun multilineNoticeCentersMessageWithItsCloseControl() {
        val state = DeftNoticeState()
        val message = "Reading history could not be saved. Deft will retry automatically."
        state.show(message, DeftNoticeKind.Error)
        compose.setContent { DeftTheme { DeftNoticeHost(state, paused = true) } }
        val text = compose.onNodeWithText(message).fetchSemanticsNode().boundsInRoot
        val close = compose.onNodeWithContentDescription("dismiss notice").fetchSemanticsNode().boundsInRoot
        assertEquals("Message and controls share the vertical center", close.center.y, text.center.y, 1f)
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
