package com.kmab.deft.feature.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.unit.dp
import com.github.takahirom.roborazzi.ExperimentalRoborazziApi
import com.github.takahirom.roborazzi.RoborazziOptions
import com.github.takahirom.roborazzi.RoborazziTaskType
import com.github.takahirom.roborazzi.captureRoboImage
import com.kmab.deft.core.datastore.LibrarySettings
import com.kmab.deft.core.designsystem.DeftTheme
import com.kmab.deft.core.model.BookFormat
import com.kmab.deft.library.scanner.ScanStatus
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class LibraryScanPanelTest {
    @get:Rule val compose = createComposeRule()

    @After fun restoreFontScale() {
        RuntimeEnvironment.setFontScale(1f)
    }

    @Test fun scanActivityStaysInsideItsPanelAndPreventsDuplicateStarts() {
        val scan = mutableStateOf(ScanStatus())
        var starts = 0
        compose.setContent {
            DeftTheme {
                Column(Modifier.padding(16.dp)) {
                    LibraryScanPanel(scan.value, { starts++ }, {})
                }
            }
        }
        compose.onNodeWithTag("library-scan-panel").assertIsDisplayed()
        compose.onNodeWithText("rescan now").assertIsEnabled().performClick()
        assertEquals(1, starts)
        compose.runOnIdle { scan.value = ScanStatus(isScanning = true, message = "Checking 1,400 book files") }
        compose.onNodeWithText("rescan now").assertIsNotEnabled()
        compose.onNodeWithText("scanning your folders").assertIsDisplayed()
        compose.onNodeWithTag("library-scan-progress").assertIsDisplayed()
        val panel = compose.onNodeWithTag("library-scan-panel").fetchSemanticsNode().boundsInRoot
        val progress = compose.onNodeWithTag("library-scan-progress").fetchSemanticsNode().boundsInRoot
        check(panel.contains(progress.topLeft) && panel.contains(progress.bottomRight))
        compose.runOnIdle { scan.value = ScanStatus(isFinished = true, workId = "finished") }
        compose.onNodeWithTag("library-scan-progress").assertDoesNotExist()
        compose.onNodeWithText("rescan now").assertIsEnabled()
    }

    @Test fun scanDetailsAreOnlyOfferedAfterARecordedScan() {
        val scan = mutableStateOf(ScanStatus())
        var details = 0
        compose.setContent { DeftTheme { LibraryScanPanel(scan.value, {}, { details++ }) } }
        compose.onNodeWithContentDescription("last scan details").assertDoesNotExist()
        compose.runOnIdle { scan.value = ScanStatus(isFinished = true, workId = "finished") }
        compose.onNodeWithContentDescription("last scan details").performClick()
        assertEquals(1, details)
    }

    @OptIn(ExperimentalRoborazziApi::class)
    @GraphicsMode(GraphicsMode.Mode.NATIVE)
    @Config(sdk = [36], qualifiers = "w411dp-h891dp-mdpi")
    @Test
    fun activeScanPanelContainsItsProgressAtLargeText() {
        RuntimeEnvironment.setFontScale(1.5f)
        compose.setContent {
            DeftTheme(false) {
                Column(Modifier.padding(16.dp)) {
                    LibraryScanPanel(ScanStatus(isScanning = true, message = "Checking 1,400 book files"), {}, {})
                }
            }
        }
        compose.onNodeWithText("Checking 1,400 book files").assertIsDisplayed()
        compose.onNodeWithText("rescan now").assertIsNotEnabled()
        val output = File("build/reports/screenshots/settings-scan-paper-large-text.png")
        output.parentFile?.mkdirs()
        compose.onNodeWithTag("library-scan-panel").captureRoboImage(
            filePath = output.path,
            roborazziOptions = RoborazziOptions(taskType = RoborazziTaskType.Record),
        )
    }

    @OptIn(ExperimentalRoborazziApi::class)
    @GraphicsMode(GraphicsMode.Mode.NATIVE)
    @Config(sdk = [36], qualifiers = "w320dp-h800dp-mdpi")
    @Test
    fun fileTypesRemainSelectableOnNarrowLargeTextScreen() {
        RuntimeEnvironment.setFontScale(1.5f)
        val settings = mutableStateOf(LibrarySettings())
        compose.setContent {
            DeftTheme(true) {
                Column(Modifier.verticalScroll(rememberScrollState()).padding(16.dp).testTag("file-types-preview")) {
                    LibraryFileTypes(settings.value) { format, enabled ->
                        val current = settings.value.enabledFormats
                        settings.value =
                            settings.value.copy(enabledFormats = if (enabled) current + format else current - format)
                    }
                }
            }
        }
        compose.onNodeWithText("txt").performScrollTo().performClick()
        assertFalse(BookFormat.TXT in settings.value.enabledFormats)
        compose
            .onNodeWithText("8 of 9 types enabled · unchecked types are not scanned")
            .performScrollTo()
            .assertIsDisplayed()
        val output = File("build/reports/screenshots/settings-file-types-ink-large-text.png")
        output.parentFile?.mkdirs()
        compose.onNodeWithTag("file-types-preview").captureRoboImage(
            filePath = output.path,
            roborazziOptions = RoborazziOptions(taskType = RoborazziTaskType.Record),
        )
    }
}
