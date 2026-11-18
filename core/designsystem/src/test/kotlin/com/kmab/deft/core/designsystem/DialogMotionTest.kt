package com.kmab.deft.core.designsystem

import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class DialogMotionTest {
    @get:Rule val compose = createComposeRule()

    @Test fun confirmRunsOnceAfterExitAndDialogCanBeReopened() {
        val shown = mutableStateOf(true)
        var confirmations = 0
        compose.setContent {
            DeftTheme {
                if (shown.value) {
                    DeftDialog("remove folder?", "History stays.", "remove", {
                        confirmations++
                        shown.value = false
                    }, { shown.value = false })
                }
            }
        }
        compose.onNodeWithText("cancel").performClick()
        compose.waitForIdle()
        compose.runOnIdle { assertEquals(false, shown.value) }
        compose.runOnIdle { shown.value = true }
        compose.onNodeWithText("remove").performClick()
        compose.waitForIdle()
        compose.runOnIdle {
            assertEquals(1, confirmations)
            assertEquals(false, shown.value)
        }
    }
}
