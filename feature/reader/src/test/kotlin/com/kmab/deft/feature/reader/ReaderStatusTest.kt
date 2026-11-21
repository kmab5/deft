package com.kmab.deft.feature.reader

import android.content.Intent
import android.os.BatteryManager
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import com.github.takahirom.roborazzi.ExperimentalRoborazziApi
import com.github.takahirom.roborazzi.RoborazziOptions
import com.github.takahirom.roborazzi.RoborazziTaskType
import com.github.takahirom.roborazzi.captureRoboImage
import com.kmab.deft.core.model.ReaderThemes
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
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
class ReaderStatusTest {
    @get:Rule val compose = createComposeRule()

    @After fun resetFontScale() = RuntimeEnvironment.setFontScale(1f)

    @Test fun batteryDistinguishesChargingFromDischargingAndUnknownLevel() {
        val intent =
            Intent(Intent.ACTION_BATTERY_CHANGED)
                .putExtra(BatteryManager.EXTRA_LEVEL, 156)
                .putExtra(BatteryManager.EXTRA_SCALE, 200)
                .putExtra(BatteryManager.EXTRA_STATUS, BatteryManager.BATTERY_STATUS_CHARGING)
        assertEquals(ReaderBattery(78, true), readerBattery(intent))
        intent.putExtra(BatteryManager.EXTRA_STATUS, BatteryManager.BATTERY_STATUS_DISCHARGING)
        assertFalse(readerBattery(intent).charging)
        assertEquals(ReaderBattery(null, false), readerBattery(null))
    }

    @OptIn(ExperimentalRoborazziApi::class)
    @Test
    fun largeStatusKeepsTrackingCenteredAndDetailsOnEitherSide() {
        RuntimeEnvironment.setFontScale(1.5f)
        compose.setContent {
            ReaderTheme(ReaderThemes.builtIn.last()) {
                Box(Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.background).testTag("status")) {
                    ReaderStatusContent(
                        "page 110 / 367",
                        true,
                        "8:57 PM",
                        ReaderBattery(64, true),
                        Modifier.fillMaxWidth(),
                    )
                }
            }
        }
        compose.onNodeWithText("reading").assertIsDisplayed()
        compose.onNodeWithContentDescription("charging").assertIsDisplayed()
        val line = compose.onNodeWithTag("status").fetchSemanticsNode().boundsInRoot
        val tracking = compose.onNodeWithTag("tracking_status").fetchSemanticsNode().boundsInRoot
        val page = compose.onNodeWithText("page 110 / 367").fetchSemanticsNode().boundsInRoot
        val time = compose.onNodeWithText("8:57 PM · 64%").fetchSemanticsNode().boundsInRoot
        assertEquals(line.center.x, tracking.center.x, 1f)
        assertTrue(page.right <= tracking.left)
        assertTrue(tracking.right <= time.left)
        val output = File("build/reports/screenshots/reader-status-large.png").apply { parentFile?.mkdirs() }
        compose.onNodeWithTag("status").captureRoboImage(
            output.path,
            roborazziOptions = RoborazziOptions(taskType = RoborazziTaskType.Record),
        )
    }

    @Test fun indicatorCanRemainCenteredWithOtherStatusDetailsDisabled() {
        compose.setContent {
            ReaderTheme(ReaderThemes.builtIn.first()) {
                ReaderStatusContent(null, false, "8:57 PM", ReaderBattery(64, true), Modifier.fillMaxWidth())
            }
        }
        compose.onNodeWithText("paused").assertIsDisplayed()
        compose.onNodeWithText("8:57 PM · 64%").assertDoesNotExist()
        compose.onNodeWithContentDescription("charging").assertDoesNotExist()
    }
}
