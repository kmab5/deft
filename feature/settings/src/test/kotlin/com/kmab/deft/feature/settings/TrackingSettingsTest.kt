package com.kmab.deft.feature.settings

import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextReplacement
import com.github.takahirom.roborazzi.ExperimentalRoborazziApi
import com.github.takahirom.roborazzi.RoborazziOptions
import com.github.takahirom.roborazzi.RoborazziTaskType
import com.github.takahirom.roborazzi.captureRoboImage
import com.kmab.deft.core.designsystem.DeftTheme
import com.kmab.deft.core.model.TrackingPreferences
import org.junit.After
import org.junit.Assert.assertEquals
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
class TrackingSettingsTest {
    @get:Rule val compose = createComposeRule()

    @After fun resetScale() = RuntimeEnvironment.setFontScale(1f)

    @Test fun validatesWholeMinutesAndCanTurnIdleOff() {
        val preferences = mutableStateOf(TrackingPreferences())
        compose.setContent {
            DeftTheme {
                TrackingSettingsContent(preferences.value, {}, {
                    preferences.value =
                        it(preferences.value)
                })
            }
        }
        compose
            .onNodeWithContentDescription(
                "reflowable books · minutes",
            ).performScrollTo()
            .performTextReplacement("61")
        compose.onNodeWithContentDescription("apply reflowable books").performScrollTo().performClick()
        assertEquals(5, preferences.value.reflowableIdleMinutes)
        compose.onNodeWithText("Enter a whole number from 1 to 60.").assertExists()
        compose.onNodeWithContentDescription("reflowable books · minutes").performTextReplacement("1")
        compose.onNodeWithContentDescription("apply reflowable books").performClick()
        assertEquals(1, preferences.value.reflowableIdleMinutes)
        compose.onNodeWithContentDescription("reflowable books").performScrollTo().performClick()
        assertEquals(null, preferences.value.reflowableIdleMinutes)
    }

    @Test fun largeTextTrackingSettingsRemainScrollable() {
        RuntimeEnvironment.setFontScale(1.5f)
        compose.setContent { DeftTheme(darkTheme = true) { TrackingSettingsContent(TrackingPreferences(), {}, {}) } }
        capture("tracking-settings-ink-large")
        compose.onNodeWithText("minimum session · 60 seconds").performScrollTo().assertExists()
    }

    @Test fun appearanceToggleHasQuietFallbackExplanation() {
        val preferences = mutableStateOf(TrackingPreferences())
        compose.setContent {
            DeftTheme {
                TrackingSettingsContent(preferences.value, {}, {
                    preferences.value =
                        it(preferences.value)
                }, true)
            }
        }
        capture("appearance-settings-paper")
        compose.onNodeWithContentDescription("celebrations").performClick()
        assertEquals(false, preferences.value.celebrations)
    }

    private fun capture(name: String) {
        val file = File("build/reports/screenshots/$name.png").also { it.parentFile?.mkdirs() }
        compose.onRoot().captureRoboImage(
            file.path,
            roborazziOptions = RoborazziOptions(taskType = RoborazziTaskType.Record),
        )
    }
}
