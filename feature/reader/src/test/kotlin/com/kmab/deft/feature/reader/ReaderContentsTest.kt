package com.kmab.deft.feature.reader

import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.StateRestorationTester
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToIndex
import com.kmab.deft.core.model.ReaderThemes
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.readium.r2.shared.publication.Link
import org.readium.r2.shared.util.Url
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ReaderContentsTest {
    @get:Rule val compose = createComposeRule()

    @Test fun contentsRestoresCurrentChapterAndUserExpansion() {
        val links =
            (0 until 40).map { index ->
                Link(
                    requireNotNull(Url("chapter-$index.xhtml")),
                    title = "Chapter $index",
                    children =
                        listOf(
                            Link(requireNotNull(Url("chapter-$index.xhtml#child")), title = "Section $index"),
                        ),
                )
            }
        val shown = mutableStateOf(true)
        var navigations = 0
        val restoration = StateRestorationTester(compose)
        restoration.setContent {
            val state = rememberReaderContentsState()
            if (shown.value) {
                ReaderTheme(ReaderThemes.builtIn.first()) {
                    ReaderContentsPanel(
                        links,
                        emptyList(),
                        Url("chapter-30.xhtml#child"),
                        { navigations++ },
                        {},
                        { _, _ -> },
                        {},
                        state,
                    )
                }
            }
        }
        compose.onNodeWithText("Section 30").assertIsDisplayed()
        compose.onNodeWithTag("reader_contents_list").performScrollToIndex(0)
        compose.onNodeWithContentDescription("expand Chapter 0").performClick()
        compose.onNodeWithText("Section 0").assertIsDisplayed()
        assertEquals(0, navigations)
        compose.runOnIdle { shown.value = false }
        compose.onNodeWithText("Section 0").assertDoesNotExist()
        compose.runOnIdle { shown.value = true }
        compose.onNodeWithText("Section 0").assertIsDisplayed()
        restoration.emulateSavedInstanceStateRestore()
        compose.onNodeWithText("Section 0").assertIsDisplayed()
        compose.onNodeWithContentDescription("collapse Chapter 0").performClick()
        compose.onNodeWithText("Section 0").assertDoesNotExist()
        compose.onNodeWithText("Chapter 0").performClick()
        assertEquals(1, navigations)
    }
}
