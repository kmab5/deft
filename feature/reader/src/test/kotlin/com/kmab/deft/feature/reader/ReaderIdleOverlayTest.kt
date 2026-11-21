package com.kmab.deft.feature.reader

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.click
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeLeft
import androidx.compose.ui.unit.dp
import com.github.takahirom.roborazzi.ExperimentalRoborazziApi
import com.github.takahirom.roborazzi.RoborazziOptions
import com.github.takahirom.roborazzi.RoborazziTaskType
import com.github.takahirom.roborazzi.captureRoboImage
import com.kmab.deft.core.model.ReaderThemes
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36], qualifiers = "w393dp-h852dp-mdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class ReaderIdleOverlayTest {
    @get:Rule val compose = createComposeRule()

    @OptIn(ExperimentalRoborazziApi::class)
    @Test
    fun wakeTouchDismissesFilmWithoutReachingBook() {
        var turns = 0
        var resumes = 0
        compose.setContent {
            ReaderTheme(ReaderThemes.builtIn.first()) {
                var idle by remember { mutableStateOf(true) }
                Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
                    Box(Modifier.fillMaxSize().clickable { turns++ }.padding(24.dp)) {
                        Text(
                            "The library was quiet. She opened her book at the place she had left it, " +
                                "and continued reading.\n\nThe afternoon light fell across the page.",
                        )
                    }
                    ReaderIdleOverlay(idle, {
                        resumes++
                        idle = false
                    })
                }
            }
        }
        compose.onNodeWithText("idling").assertIsDisplayed()
        val image = File("build/reports/screenshots/reader-idle-film.png").apply { parentFile?.mkdirs() }
        compose.onRoot().captureRoboImage(
            image.path,
            roborazziOptions = RoborazziOptions(taskType = RoborazziTaskType.Record),
        )
        compose.onNodeWithContentDescription("resume reading").performTouchInput { click() }
        compose.onNodeWithText("idling").assertDoesNotExist()
        assertEquals(1, resumes)
        assertEquals(0, turns)
    }

    @Test fun activeReadingHasNoFilmOrResumeAction() {
        compose.setContent {
            ReaderTheme(ReaderThemes.builtIn.first()) { ReaderIdleOverlay(false, {}) }
        }
        compose.onNodeWithText("idling").assertDoesNotExist()
        compose.onNodeWithContentDescription("resume reading").assertDoesNotExist()
    }

    @Test fun swipeAlsoWakesWithoutTurningTheUnderlyingPage() {
        var turns = 0
        var resumes = 0
        compose.setContent {
            ReaderTheme(ReaderThemes.builtIn.last()) {
                var idle by remember { mutableStateOf(true) }
                Box(Modifier.fillMaxSize().clickable { turns++ }) {
                    ReaderIdleOverlay(idle, {
                        resumes++
                        idle = false
                    })
                }
            }
        }
        compose.onNodeWithContentDescription("resume reading").performTouchInput { swipeLeft() }
        compose.onNodeWithText("idling").assertDoesNotExist()
        assertEquals(1, resumes)
        assertEquals(0, turns)
    }
}
