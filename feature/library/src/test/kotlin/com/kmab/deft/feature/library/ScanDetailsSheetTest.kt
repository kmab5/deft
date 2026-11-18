package com.kmab.deft.feature.library

import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.test.assertIsDisplayed
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
import com.kmab.deft.library.scanner.ScanDetailEntry
import com.kmab.deft.library.scanner.ScanDetailOutcome
import com.kmab.deft.library.scanner.ScanDetails
import com.kmab.deft.library.scanner.ScanReport
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
class ScanDetailsSheetTest {
    @get:Rule val compose = createComposeRule()
    private val report = ScanReport(1, 0, 1, 0, 0, 2, emptyList(), processedFiles = 3)

    @OptIn(ExperimentalRoborazziApi::class)
    @Test
    fun namedFilesReasonsAndDiagnosticGroupsAreAccessible() {
        val open = mutableStateOf(true)
        val details =
            ScanDetails(
                listOf(
                    ScanDetailEntry(
                        "1",
                        "Earthsea.epub",
                        "Fiction/Earthsea.epub",
                        ScanDetailOutcome.ADDED,
                        "Added to your library.",
                    ),
                    ScanDetailEntry(
                        "2",
                        "notes.txt",
                        "notes.txt",
                        ScanDetailOutcome.SKIPPED,
                        "TXT is disabled in library settings.",
                    ),
                    ScanDetailEntry(
                        "3",
                        "Damaged.epub",
                        "Fiction/Damaged.epub",
                        ScanDetailOutcome.ERROR,
                        "The file could not be read.",
                    ),
                    ScanDetailEntry(
                        "3",
                        "Damaged.epub",
                        "Fiction/Damaged.epub",
                        ScanDetailOutcome.FALLBACK,
                        "Using filename details.",
                    ),
                ),
                omittedCount = 2,
            )
        compose.setContent { DeftTheme { if (open.value) ScanDetailsSheet(report, details, { open.value = false }) } }
        compose.onNodeWithText("The file could not be read.").assertIsDisplayed()
        compose.onNodeWithText("Using filename details.").assertIsDisplayed()
        val output = File("build/reports/screenshots/scan-details.png").apply { parentFile?.mkdirs() }
        compose.onNodeWithTag("books_sheet").captureRoboImage(
            output.path,
            roborazziOptions = RoborazziOptions(taskType = RoborazziTaskType.Record),
        )
        compose.onNodeWithText("added 1").performClick()
        compose.onNodeWithText("Earthsea.epub").assertIsDisplayed()
        compose.onNodeWithText("skipped 1").performClick()
        compose.onNodeWithText("notes.txt").assertIsDisplayed()
        compose.onNodeWithText("TXT is disabled in library settings.").assertIsDisplayed()
        compose.onNodeWithText("2 more scan records weren’t retained. Scan totals are unchanged.").assertIsDisplayed()
        compose.onNodeWithContentDescription("close popup").performClick()
        compose.onNodeWithTag("books_sheet").assertDoesNotExist()
    }

    @Test fun cancelledScanShowsNamedRecordsWithoutInventingZeroTotals() {
        val details =
            ScanDetails(
                listOf(
                    ScanDetailEntry(
                        "kept",
                        "Kept.epub",
                        "Kept.epub",
                        ScanDetailOutcome.ADDED,
                        "Added before the scan stopped.",
                    ),
                ),
            )
        compose.setContent { DeftTheme { ScanDetailsSheet(null, details, {}) } }
        compose.onNodeWithText("Scan stopped. Files saved before it stopped are listed below.").assertIsDisplayed()
        compose.onNodeWithText("Kept.epub").assertIsDisplayed()
        compose.onNodeWithText("scan complete · 0 files checked · 0 books added").assertDoesNotExist()
    }

    @Test fun legacyReportDoesNotPretendDetailedNamesWereRecorded() {
        compose.setContent { DeftTheme { ScanDetailsSheet(report, null, {}) } }
        compose.onNodeWithText("Detailed file names aren’t available for this scan.").assertIsDisplayed()
    }
}
