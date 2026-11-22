package com.kmab.deft.feature.library

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import com.github.takahirom.roborazzi.ExperimentalRoborazziApi
import com.github.takahirom.roborazzi.RoborazziOptions
import com.github.takahirom.roborazzi.RoborazziTaskType
import com.github.takahirom.roborazzi.captureRoboImage
import com.kmab.deft.core.designsystem.DeftTheme
import com.kmab.deft.core.designsystem.LocalDeftColors
import org.junit.After
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
@Config(sdk = [36], qualifiers = "w360dp-h800dp-mdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class SelectionBarRenderTest {
    @get:Rule val compose = createComposeRule()

    @After fun resetFont() = RuntimeEnvironment.setFontScale(1f)

    @OptIn(ExperimentalRoborazziApi::class)
    @Test
    fun largeSelectionKeepsActionsVisibleAtLargeText() {
        RuntimeEnvironment.setFontScale(1.5f)
        compose.setContent {
            DeftTheme(darkTheme = true) {
                Box(Modifier.fillMaxWidth().background(LocalDeftColors.current.bg).testTag("selection_reference")) {
                    BooksSelectionBar(140400, {}, {}, {})
                }
            }
        }
        val count = compose.onNodeWithContentDescription("140400 selected")
        count.assertIsDisplayed()
        val add = compose.onNodeWithContentDescription("add to category")
        add.assertIsDisplayed().assertHasClickAction()
        compose.onNodeWithContentDescription("select all").assertIsDisplayed().assertHasClickAction()
        compose.onNodeWithContentDescription("clear selection").assertIsDisplayed().assertHasClickAction()
        assertTrue(count.fetchSemanticsNode().boundsInRoot.right <= add.fetchSemanticsNode().boundsInRoot.left)
        val image = File("build/reports/screenshots/selection-041-ink-large.png").apply { parentFile?.mkdirs() }
        compose.onNodeWithTag("selection_reference").captureRoboImage(
            image.path,
            roborazziOptions = RoborazziOptions(taskType = RoborazziTaskType.Record),
        )
    }
}
