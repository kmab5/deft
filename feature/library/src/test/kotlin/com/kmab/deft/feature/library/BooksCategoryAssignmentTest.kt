package com.kmab.deft.feature.library

import androidx.compose.foundation.layout.Column
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.junit4.StateRestorationTester
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import com.kmab.deft.core.designsystem.DeftButton
import com.kmab.deft.core.designsystem.DeftTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class BooksCategoryAssignmentTest {
    @get:Rule val compose = createComposeRule()

    @Test fun openAssignmentAndDraftSurviveRestorationThenCompleteWithoutAStoredCallback() {
        val restoration = StateRestorationTester(compose)
        restoration.setContent {
            DeftTheme {
                Column {
                    BooksCategoryAssignment(picker = { ids, _, complete ->
                        var draft by rememberSaveable { mutableStateOf("") }
                        OutlinedTextField(draft, { draft = it }, label = { Text("category draft") })
                        DeftButton("save ${ids.sorted()}", complete)
                    }) { add, completed ->
                        DeftButton("open assignment", { add(setOf(9, 3)) })
                        Text("completed $completed")
                    }
                }
            }
        }
        compose.onNodeWithText("open assignment").performClick()
        compose.onNodeWithText("category draft").performTextInput("My reading list")
        restoration.emulateSavedInstanceStateRestore()
        compose.onNodeWithText("My reading list").assertExists()
        compose.onNodeWithText("save [3, 9]").performClick()
        compose.onNodeWithText("completed 1").assertExists()
        compose.onNodeWithText("My reading list").assertDoesNotExist()
    }
}
