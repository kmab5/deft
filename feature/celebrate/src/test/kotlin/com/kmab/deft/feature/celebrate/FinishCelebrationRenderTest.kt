package com.kmab.deft.feature.celebrate

import androidx.compose.ui.test.junit4.StateRestorationTester
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.github.takahirom.roborazzi.ExperimentalRoborazziApi
import com.github.takahirom.roborazzi.RoborazziOptions
import com.github.takahirom.roborazzi.RoborazziTaskType
import com.github.takahirom.roborazzi.captureRoboImage
import com.kmab.deft.core.designsystem.DeftTheme
import org.junit.After
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36], qualifiers = "w393dp-h852dp-420dpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@OptIn(ExperimentalRoborazziApi::class)
class FinishCelebrationRenderTest {
    @get:Rule val compose = createComposeRule()

    @After fun resetScale() = RuntimeEnvironment.setFontScale(1f)

    @Test fun fullMomentRendersAndRestorationKeepsItsCompletedPhase() {
        val restoration = StateRestorationTester(compose)
        compose.mainClock.autoAdvance = false
        restoration.setContent {
            DeftTheme {
                FinishCelebration(
                    7,
                    "A Wizard of Earthsea",
                    "Ursula K. Le Guin",
                    7340,
                    12,
                    4,
                    true,
                    4.5,
                    true,
                    {},
                    {},
                    {},
                    animationsEnabled = true,
                )
            }
        }
        compose.mainClock.advanceTimeBy(1900)
        val file = File("build/reports/screenshots/finish-full.png").also { it.parentFile?.mkdirs() }
        compose.onNodeWithContentDescription("skip celebration").captureRoboImage(
            file.path,
            roborazziOptions = RoborazziOptions(taskType = RoborazziTaskType.Record),
        )
        compose.onNodeWithContentDescription("skip celebration").performClick()
        compose.mainClock.advanceTimeBy(500)
        restoration.emulateSavedInstanceStateRestore()
        compose.mainClock.advanceTimeBy(500)
        compose.onNodeWithText("how was it?").assertExists()
        compose.onNodeWithContentDescription("skip celebration").assertDoesNotExist()
    }

    @Test fun quietMomentSupportsLargeText() {
        RuntimeEnvironment.setFontScale(1.5f)
        compose.setContent {
            DeftTheme(darkTheme = true) {
                FinishCelebration(
                    7,
                    "A Wizard of Earthsea",
                    "Ursula K. Le Guin",
                    7340,
                    12,
                    4,
                    false,
                    4.5,
                    true,
                    {},
                    {},
                    {},
                )
            }
        }
        compose.onNodeWithText("how was it?").assertExists()
        compose.onNodeWithText("done").assertExists()
        val file = File("build/reports/screenshots/finish-quiet-ink-large.png").also { it.parentFile?.mkdirs() }
        compose.onNodeWithTag("finish_sheet").captureRoboImage(
            file.path,
            roborazziOptions = RoborazziOptions(taskType = RoborazziTaskType.Record),
        )
    }
}
