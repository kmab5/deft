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
import com.kmab.deft.core.database.entity.HighlightEntity
import com.kmab.deft.core.model.HighlightColor
import com.kmab.deft.core.model.ReaderThemes
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.readium.r2.shared.publication.Link
import org.readium.r2.shared.util.Url
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.time.Instant

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
        val parentLeft =
            compose
                .onNodeWithText("Chapter 0")
                .fetchSemanticsNode()
                .boundsInRoot.left
        val childLeft =
            compose
                .onNodeWithText("Section 0")
                .fetchSemanticsNode()
                .boundsInRoot.left
        assertTrue("A nested title must start after its parent's title", childLeft > parentLeft)
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

    @Test fun notesTabRestoresAndJumpsToColorlessSavedPassage() {
        val passage =
            HighlightEntity(
                id = 1,
                bookId = 2,
                locator = """{"href":"chapter.xhtml","type":"application/xhtml+xml"}""",
                text = "A saved passage",
                color = HighlightColor.NONE,
                note = "A colorless note",
                totalProgression = 0.5,
                createdAt = Instant.EPOCH,
                updatedAt = Instant.EPOCH,
            )
        var opened: String? = null
        var edited: Long? = null
        val restoration = StateRestorationTester(compose)
        restoration.setContent {
            ReaderTheme(ReaderThemes.builtIn.first()) {
                ReaderContentsPanel(
                    contents = emptyList(),
                    savedBookmarks = emptyList(),
                    currentHref = null,
                    onChapter = {},
                    onBookmark = {},
                    onRename = { _, _ -> },
                    onRemove = {},
                    savedAnnotations = listOf(passage),
                    onAnnotation = { opened = it.locator },
                    onEditAnnotation = { edited = it },
                )
            }
        }
        compose.onNodeWithText("notes").performClick()
        restoration.emulateSavedInstanceStateRestore()
        compose.onNodeWithText("A colorless note", substring = true).assertIsDisplayed()
        compose.onNodeWithText("A saved passage", substring = true).performClick()
        compose.onNodeWithContentDescription("edit annotation").performClick()
        compose.runOnIdle {
            assertEquals(passage.locator, opened)
            assertEquals(passage.id, edited)
        }
    }
}
