package com.kmab.deft.feature.notes

import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.junit4.StateRestorationTester
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import com.kmab.deft.core.designsystem.DeftTheme
import com.kmab.deft.core.model.HighlightColor
import com.kmab.deft.library.notes.HighlightDraft
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36], qualifiers = "w640dp-h360dp-land-mdpi")
class HighlightEditorTest {
    @get:Rule val compose = createComposeRule()

    @Test fun unsavedDraftSurvivesRecreationAndWritesOnlyOnSave() {
        val original = HighlightDraft(3, "locator", "Selected passage")
        var saved: HighlightDraft? = null
        val restoration = StateRestorationTester(compose)
        restoration.setContent {
            DeftTheme { HighlightEditor(original, { saved = it }, {}) }
        }
        compose.onNodeWithText("purple").performClick()
        compose.onNodeWithText("underline").performClick()
        compose.onNodeWithText("note").performTextInput("Remember this")
        assertNull(saved)
        restoration.emulateSavedInstanceStateRestore()
        compose.onNodeWithText("save").assertIsEnabled().performClick()
        assertEquals(original.copy(color = HighlightColor.PURPLE, note = "Remember this", underline = true), saved)
    }

    @Test fun cancelNeverSavesAndDeletingRequiresConfirmation() {
        var canceled = 0
        var deleted = 0
        var saved = 0
        compose.setContent {
            DeftTheme {
                HighlightEditor(
                    HighlightDraft(3, "locator", "Selected passage", HighlightColor.GREEN, id = 1),
                    { saved++ },
                    { canceled++ },
                    { deleted++ },
                )
            }
        }
        compose.onNodeWithText("cancel").performClick()
        assertEquals(1, canceled)
        assertEquals(0, saved)
        compose.onNodeWithText("delete").performClick()
        assertEquals(0, deleted)
        compose.onNodeWithText("delete annotation").performClick()
        compose.waitForIdle()
        assertEquals(1, deleted)
    }
}
