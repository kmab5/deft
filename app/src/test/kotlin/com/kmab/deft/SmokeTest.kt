package com.kmab.deft

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.printToString
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class SmokeTest {
    @get:Rule val compose = createAndroidComposeRule<MainActivity>()

    @Test fun launchesThreeWorkingDestinations() {
        compose.onNodeWithTag("books_screen").assertIsDisplayed()
        compose.onNodeWithText("categories").performClick()
        compose.waitUntil(5_000) { compose.onAllNodesWithText("new category").fetchSemanticsNodes().size == 1 }
        compose.onNodeWithText("new category").assertIsDisplayed()
        // Startup scanning can post a notice over the bottom tabs on this small test display.
        if (compose.onAllNodesWithContentDescription("dismiss notice").fetchSemanticsNodes().isNotEmpty()) {
            compose.onNodeWithContentDescription("dismiss notice").performClick()
        }
        compose.onNodeWithText("authors").performClick()
        try {
            compose.waitUntil(5_000) { compose.onAllNodesWithText("find authors").fetchSemanticsNodes().size == 1 }
        } finally {
            println(compose.onRoot(useUnmergedTree = true).printToString())
        }
        compose.onNodeWithText("find authors").assertIsDisplayed()
        compose.onNodeWithText("books").performClick()
        compose.onNodeWithTag("books_screen").assertIsDisplayed()
    }
}
