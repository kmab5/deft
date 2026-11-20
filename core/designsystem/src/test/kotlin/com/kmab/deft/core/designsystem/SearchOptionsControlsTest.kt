package com.kmab.deft.core.designsystem

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.OutlinedTextField
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.dp
import com.github.takahirom.roborazzi.ExperimentalRoborazziApi
import com.github.takahirom.roborazzi.RoborazziOptions
import com.github.takahirom.roborazzi.RoborazziTaskType
import com.github.takahirom.roborazzi.captureRoboImage
import com.kmab.deft.core.model.SearchOptions
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
class SearchOptionsControlsTest {
    @get:Rule val compose = createComposeRule()

    @After fun restoreFontScale() = RuntimeEnvironment.setFontScale(1f)

    @Test fun alignedCardContainsOptionsAndHelp() = render(1f)

    @Test fun largeTextKeepsHelpInsideTheCard() = render(1.5f)

    @OptIn(ExperimentalRoborazziApi::class)
    private fun render(scale: Float) {
        RuntimeEnvironment.setFontScale(scale)
        val options = mutableStateOf(SearchOptions(regex = true))
        compose.setContent {
            DeftTheme {
                Column(Modifier.padding(16.dp).testTag("search_reference")) {
                    OutlinedTextField("moon", {}, Modifier.fillMaxWidth().testTag("query"))
                    SearchOptionsControls(options.value, { options.value = it })
                }
            }
        }
        val card = compose.onNodeWithTag("search_options").fetchSemanticsNode().boundsInRoot
        val query = compose.onNodeWithTag("query").fetchSemanticsNode().boundsInRoot
        val chip = compose.onNodeWithText("Match case").fetchSemanticsNode().boundsInRoot
        val help = compose.onNodeWithText("Regex: try", substring = true).fetchSemanticsNode().boundsInRoot
        assertEquals(query.left, card.left, 1f)
        assertEquals(query.right, card.right, 1f)
        assertTrue(chip.left > card.left)
        assertTrue(help.left > card.left && help.right < card.right && help.bottom < card.bottom)
        compose.onNodeWithText("Match case").performClick()
        assertTrue(options.value.matchCase)
        val output = File("build/reports/screenshots/search-options-$scale.png").apply { parentFile?.mkdirs() }
        compose.onNodeWithTag("search_reference").captureRoboImage(
            output.path,
            roborazziOptions = RoborazziOptions(taskType = RoborazziTaskType.Record),
        )
    }
}
