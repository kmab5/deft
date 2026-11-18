package com.kmab.deft.core.designsystem

import android.graphics.BitmapFactory
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.unit.dp
import com.github.takahirom.roborazzi.ExperimentalRoborazziApi
import com.github.takahirom.roborazzi.RoborazziOptions
import com.github.takahirom.roborazzi.RoborazziTaskType
import com.github.takahirom.roborazzi.captureRoboImage
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36], qualifiers = "w400dp-h1600dp-mdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class ComponentScreenshotTest {
    @get:Rule val compose = createComposeRule()

    @Test fun inkComponents() = screenshot("ink", true)

    @Test fun paperComponents() = screenshot("paper", false)

    @Test fun inkDialog() = dialog("ink-dialog", true)

    @Test fun paperDialog() = dialog("paper-dialog", false)

    @Test fun inkSheet() = sheet("ink-sheet", true)

    @Test fun paperSheet() = sheet("paper-sheet", false)

    private fun dialog(
        name: String,
        dark: Boolean,
    ) = screenshot(name, dark) {
        DeftDialog("remove this folder?", "Its books will be marked missing. Their history stays.", "remove", {
        }, {}, modifier = Modifier.testTag("gallery"))
    }

    private fun sheet(
        name: String,
        dark: Boolean,
    ) = screenshot(name, dark) {
        DeftSheet({}, Modifier.testTag("gallery")) {
            Text("reading state", style = DeftTypography.headline, color = LocalDeftColors.current.text)
            StateChip(DeftReadingState.Reading, selected = true)
            StateChip(DeftReadingState.Finished)
            DeftButton("done", {})
        }
    }

    @OptIn(ExperimentalRoborazziApi::class)
    private fun screenshot(
        name: String,
        dark: Boolean,
        content: @Composable () -> Unit = { Gallery() },
    ) {
        compose.setContent {
            DeftTheme(dark) { Box(Modifier.fillMaxSize().background(LocalDeftColors.current.bg)) { content() } }
        }
        compose.waitForIdle()
        val golden = File("src/test/screenshots/$name.png")
        val output = File("build/reports/screenshots/$name.png").apply { parentFile?.mkdirs() }
        compose.onNodeWithTag("gallery").captureRoboImage(
            filePath = output.path,
            roborazziOptions = RoborazziOptions(taskType = RoborazziTaskType.Record),
        )
        val actual = BitmapFactory.decodeFile(output.absolutePath)
        if (System.getProperty("deft.recordScreenshots") == "true") {
            golden.parentFile?.mkdirs()
            output.copyTo(golden, overwrite = true)
        }
        assertTrue(
            "Missing golden: record with -Pdeft.recordScreenshots=true and visually review $output",
            golden.exists(),
        )
        val expected = BitmapFactory.decodeFile(golden.absolutePath)
        assertEquals(expected.width, actual.width)
        assertEquals(expected.height, actual.height)
        var differences = 0
        for (y in 0 until actual.height) {
            for (x in 0 until actual.width) {
                if (expected.getPixel(x, y) != actual.getPixel(x, y)) differences++
            }
        }
        assertEquals("Screenshot differs; inspect $output", 0, differences)
    }
}

@Composable
private fun Gallery() {
    val c = LocalDeftColors.current
    Column(
        Modifier.width(400.dp).background(c.bg).testTag("gallery"),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        DeftTopBar("components", scrolled = true)
        Column(Modifier.padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Deft", color = c.text, style = DeftTypography.display)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                DeftButton("read", {})
                DeftButton("settings", {}, style = DeftButtonStyle.Secondary)
                DeftButton("cancel", {}, style = DeftButtonStyle.Text)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                StateChip(DeftReadingState.Reading, selected = true)
                StateChip(DeftReadingState.Finished)
                StateChip(DeftReadingState.Paused)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                StateChip(DeftReadingState.NotRead)
                StateChip(DeftReadingState.Abandoned)
                FilterChip("all books", true, {})
            }
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                BookCover("A book", "An author", DeftReadingState.Reading, 0.43f, Modifier.width(100.dp))
                CoverImage("Missing book", "Another author", Modifier.width(100.dp), missing = true, isProtected = true)
            }
            BookRow("A quiet book", "An author", DeftReadingState.Paused, 0.5f, "2h")
            SegmentedProgress(
                listOf(
                    ProgressSegment(DeftReadingState.Finished, 0.4f),
                    ProgressSegment(DeftReadingState.Reading, 0.3f),
                    ProgressSegment(DeftReadingState.Abandoned, 0.1f),
                ),
                legend = "4 finished · 3 reading · 1 abandoned",
            )
            RatingBar(3.5f, {})
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                StatTile("time read", "2h 14m", Modifier.weight(1f), "+12m")
                StatTile("books finished", "12", Modifier.weight(1f), "2 fewer", increasing = false)
            }
            ContinueCard("A quiet book", "An author", 0.43f, "2h 10m", {})
            DeftSnackbar("added 12 · missing 1 · 0 errors", action = "undo")
        }
        PersonalBestStrip("fastest finish")
        DeftBottomBar(0, {})
    }
}
