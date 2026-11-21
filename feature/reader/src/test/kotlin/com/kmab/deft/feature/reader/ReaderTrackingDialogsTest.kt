package com.kmab.deft.feature.reader

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.kmab.deft.core.model.BookState
import com.kmab.deft.core.model.ReaderOpenChoice
import com.kmab.deft.core.model.ReaderThemes
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36], qualifiers = "w393dp-h852dp-mdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class ReaderTrackingDialogsTest {
    @get:Rule val compose = createComposeRule()

    @Test fun finishedOpenExposesDistinctRereadAndBrowsingChoicesAtLargeText() {
        RuntimeEnvironment.setFontScale(1.5f)
        val choices = mutableListOf<ReaderOpenChoice>()
        try {
            compose.setContent {
                ReaderTheme(ReaderThemes.builtIn.first()) {
                    ReaderTrackingDialogs(BookState.FINISHED, false, false, choices::add, {}, {}, {})
                }
            }
            compose.onNodeWithText("Re-read").assertIsDisplayed().performClick()
            compose.onNodeWithText("Just browsing").assertIsDisplayed().performClick()
            assertEquals(listOf(ReaderOpenChoice.RE_READ, ReaderOpenChoice.BROWSING), choices)
        } finally {
            RuntimeEnvironment.setFontScale(1f)
        }
    }

    @Test fun abandonedOpenOffersResumeAndStartOver() {
        val choices = mutableListOf<ReaderOpenChoice>()
        compose.setContent {
            ReaderTheme(ReaderThemes.builtIn.first()) {
                ReaderTrackingDialogs(BookState.ABANDONED, false, false, choices::add, {}, {}, {})
            }
        }
        compose.onNodeWithText("Resume").performClick()
        compose.onNodeWithText("Start over").performClick()
        assertEquals(listOf(ReaderOpenChoice.RESUME, ReaderOpenChoice.START_OVER), choices)
    }

    @Test fun reachingEndDoesNotFinishUntilYesAndNoContinues() {
        var finishes = 0
        var declines = 0
        compose.setContent {
            ReaderTheme(ReaderThemes.builtIn.first()) {
                ReaderTrackingDialogs(null, true, false, {}, {}, { finishes++ }, { declines++ })
            }
        }
        assertEquals(0, finishes)
        compose.onNodeWithText("No").performClick()
        assertEquals(1, declines)
        assertEquals(0, finishes)
        compose.onNodeWithText("Yes").performClick()
        assertEquals(1, finishes)
    }
}
