package com.kmab.deft.core.designsystem

import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ProgressSemanticsTest {
    @get:Rule val compose = createComposeRule()

    @Test fun stateBarExposesStateAndClampedProgress() {
        compose.setContent { DeftTheme { StateBar(DeftReadingState.Reading, 1.4f, Modifier.testTag("bar")) } }
        compose
            .onNodeWithTag("bar")
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.StateDescription, "reading"))
            .assert(
                SemanticsMatcher.expectValue(
                    SemanticsProperties.ProgressBarRangeInfo,
                    ProgressBarRangeInfo(
                        1f,
                        0f..1f,
                    ),
                ),
            )
    }

    @Test fun gridCoverExposesReadingStateAndProgress() {
        compose.setContent { DeftTheme { BookCover("A book", "An author", DeftReadingState.Paused, 0.43f) } }
        compose
            .onNode(SemanticsMatcher.expectValue(SemanticsProperties.StateDescription, "paused"))
            .assert(
                SemanticsMatcher.expectValue(
                    SemanticsProperties.ProgressBarRangeInfo,
                    ProgressBarRangeInfo(
                        0.43f,
                        0f..1f,
                    ),
                ),
            )
    }

    @Test fun segmentedBarSpeaksStatesWithoutLegend() {
        compose.setContent {
            DeftTheme {
                SegmentedProgress(
                    listOf(
                        ProgressSegment(DeftReadingState.Finished, 0.4f),
                        ProgressSegment(DeftReadingState.Reading, 0.3f),
                        ProgressSegment(DeftReadingState.Abandoned, 0.1f),
                    ),
                    Modifier.testTag("segments"),
                )
            }
        }
        compose
            .onNodeWithTag("segments")
            .assert(
                SemanticsMatcher.expectValue(
                    SemanticsProperties.StateDescription,
                    "finished 40%, reading 30%, abandoned 10%",
                ),
            ).assert(
                SemanticsMatcher.expectValue(
                    SemanticsProperties.ProgressBarRangeInfo,
                    ProgressBarRangeInfo(
                        0.4f,
                        0f..1f,
                    ),
                ),
            )
    }

    @Test fun emptySegmentsExposeZeroProgress() {
        compose.setContent { DeftTheme { SegmentedProgress(emptyList(), Modifier.testTag("empty")) } }
        compose
            .onNodeWithTag("empty")
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.StateDescription, "no reading progress"))
            .assert(
                SemanticsMatcher.expectValue(
                    SemanticsProperties.ProgressBarRangeInfo,
                    ProgressBarRangeInfo(
                        0f,
                        0f..1f,
                    ),
                ),
            )
    }
}
