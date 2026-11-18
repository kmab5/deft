package com.kmab.deft.feature.readersettings

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import com.kmab.deft.core.designsystem.DeftTheme
import com.kmab.deft.core.model.ReaderTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ReaderThemeEditorTest {
    @get:Rule val compose = createComposeRule()

    @Test fun invalidThemeShowsCorrectiveNoticeAndDoesNotSave() {
        var saved: ReaderTheme? = null
        compose.setContent { DeftTheme { ReaderThemeEditor(null, { saved = it }, {}) } }
        compose.onNodeWithText("save theme").performScrollTo().performClick()
        compose.onNodeWithText("Use a theme name between 1 and 40 characters.").assertIsDisplayed()
        compose.runOnIdle { assertNull(saved) }
    }

    @Test fun validEditSavesSameIdentityAndCloses() {
        val initial = ReaderTheme("custom-test", "test theme", "#000000", "#FFFFFF", "#C373FF")
        var saved: ReaderTheme? = null
        var dismissed = false
        compose.setContent { DeftTheme { ReaderThemeEditor(initial, { saved = it }, { dismissed = true }) } }
        compose.onNodeWithText("save theme").performScrollTo().performClick()
        compose.runOnIdle {
            assertEquals(initial, saved)
            assertEquals(true, dismissed)
        }
    }
}
