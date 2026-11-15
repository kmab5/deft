package com.kmab.deft

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class SmokeTest {
    @get:Rule val compose = createAndroidComposeRule<MainActivity>()

    @Test fun launchesBooksDestination() {
        compose.onNodeWithText("books").assertIsDisplayed()
    }
}
