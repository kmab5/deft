package com.kmab.deft.feature.celebrate

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsOff
import androidx.compose.ui.test.assertIsOn
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

    @Test fun favoriteHeartExposesBothChangesToAccessibility() {
        compose.setContent {
            var favorite by remember { mutableStateOf(false) }
            DeftTheme { AssessmentControls(null, favorite, {}, { favorite = it }) }
        }
        val heart = compose.onNodeWithContentDescription("favorite")
        heart.assertIsOff().performClick()
        heart.assertIsOn().performClick()
        heart.assertIsOff()
    }

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

    @Test fun animationHoldsUntilTheCloseControlIsPressed() {
        compose.mainClock.autoAdvance = false
        compose.setContent {
            DeftTheme {
                FinishCelebration(
                    1,
                    "Earthsea",
                    "Le Guin",
                    35,
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
        compose.mainClock.advanceTimeBy(15000)
        compose.onNodeWithText("how was it?").assertDoesNotExist()
        compose.onNodeWithText("35s").assertExists()
        compose.mainClock.autoAdvance = true
        compose.onNodeWithContentDescription("close celebration").assertIsDisplayed().performClick()
        compose.waitForIdle()
        compose.onNodeWithContentDescription("close celebration").assertDoesNotExist()
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
