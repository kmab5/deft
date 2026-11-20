package com.kmab.deft.feature.celebrate

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.kmab.deft.core.designsystem.DeftTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class FinishCelebrationTest {
    @get:Rule val compose = createComposeRule()

    @Test fun quietCompletionOffersAssessmentImmediately() {
        var dismissals = 0
        compose.setContent {
            DeftTheme {
                FinishCelebration(
                    1,
                    "Earthsea",
                    "Le Guin",
                    120,
                    2,
                    1,
                    false,
                    null,
                    false,
                    {},
                    {},
                    { dismissals++ },
                )
            }
        }
        compose.onNodeWithText("finished.").assertExists()
        compose.onNodeWithText("how was it?").assertExists()
        compose.onNodeWithText("done").performClick()
        assertEquals(1, dismissals)
    }

    @Test fun animationFinishesWithoutInputAfterTwoPointEightSeconds() {
        compose.mainClock.autoAdvance = false
        compose.setContent {
            DeftTheme {
                FinishCelebration(
                    1,
                    "Earthsea",
                    "Le Guin",
                    120,
                    2,
                    1,
                    true,
                    null,
                    false,
                    {},
                    {},
                    {},
                    animationsEnabled = true,
                )
            }
        }
        compose.mainClock.advanceTimeBy(3100)
        compose.onNodeWithText("how was it?").assertExists()
    }

    @Test fun systemAnimationsOffUsesQuietVersion() {
        compose.setContent {
            DeftTheme {
                FinishCelebration(
                    1,
                    "Earthsea",
                    "Le Guin",
                    120,
                    2,
                    1,
                    true,
                    null,
                    false,
                    {},
                    {},
                    {},
                    animationsEnabled = false,
                )
            }
        }
        compose.onNodeWithText("how was it?").assertExists()
        compose.onNodeWithContentDescription("skip celebration").assertDoesNotExist()
    }

    @Test fun fullCompletionCanSkipStraightToAssessment() {
        compose.mainClock.autoAdvance = false
        compose.setContent {
            DeftTheme {
                FinishCelebration(
                    1,
                    "Earthsea",
                    "Le Guin",
                    120,
                    2,
                    1,
                    true,
                    null,
                    false,
                    {},
                    {},
                    {},
                    animationsEnabled = true,
                )
            }
        }
        compose.onNodeWithContentDescription("skip celebration").performClick()
        compose.mainClock.advanceTimeBy(500)
        compose.onNodeWithText("how was it?").assertExists()
    }
}
