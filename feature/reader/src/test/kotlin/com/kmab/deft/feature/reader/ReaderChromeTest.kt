package com.kmab.deft.feature.reader

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextReplacement
import androidx.compose.ui.unit.dp
import com.github.takahirom.roborazzi.ExperimentalRoborazziApi
import com.github.takahirom.roborazzi.RoborazziOptions
import com.github.takahirom.roborazzi.RoborazziTaskType
import com.github.takahirom.roborazzi.captureRoboImage
import com.kmab.deft.core.database.entity.BookmarkEntity
import com.kmab.deft.core.model.ReaderThemes
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.readium.r2.shared.publication.Link
import org.readium.r2.shared.util.Url
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File
import java.time.Instant

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36], qualifiers = "w393dp-h852dp-mdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class ReaderChromeTest {
    @get:Rule val compose = createComposeRule()

    @After fun restoreFontScale() = RuntimeEnvironment.setFontScale(1f)

    @OptIn(ExperimentalRoborazziApi::class)
    @Test
    fun largeTextChromeKeepsActionsAccessibleAndReturnsAfterJump() {
        RuntimeEnvironment.setFontScale(1.5f)
        val state =
            mutableStateOf(
                ReaderChromeState("Chapter two", 0.35f, "section 2 of 8", listOf(0f, 0.2f, 0.5f), false, true, true),
            )
        var back = 0
        var contents = 0
        var settings = 0
        var returns = 0
        compose.setContent {
            ReaderTheme(ReaderThemes.builtIn.first()) {
                Box(
                    Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background).testTag("reader_reference"),
                ) {
                    Text(
                        "A quiet afternoon\n\nThe path turned toward the old house. " +
                            "Beyond the trees, the last light settled on the garden.",
                        Modifier.padding(horizontal = 28.dp, vertical = 150.dp),
                        style = MaterialTheme.typography.bodyLarge,
                    )
                    ReaderChrome(
                        true,
                        state.value,
                        { back++ },
                        { contents++ },
                        { settings++ },
                        { state.value = state.value.copy(bookmarked = !state.value.bookmarked) },
                        {},
                        {
                            returns++
                            state.value = state.value.copy(canReturn = false)
                        },
                    )
                }
            }
        }
        listOf("Close reader", "Contents and bookmarks", "Add bookmark", "Reading settings", "Book progress").forEach {
            compose.onNodeWithContentDescription(it).assertIsDisplayed()
        }
        compose.onNodeWithText("go back to where I was").assertIsDisplayed()
        val output = File("build/reports/screenshots/reader-chrome-large-text.png").apply { parentFile?.mkdirs() }
        compose.onNodeWithTag("reader_reference").captureRoboImage(
            output.path,
            roborazziOptions = RoborazziOptions(taskType = RoborazziTaskType.Record),
        )
        compose.onNodeWithContentDescription("Add bookmark").performClick()
        compose.onNodeWithContentDescription("Remove bookmark").assertIsDisplayed().performClick()
        compose.onNodeWithContentDescription("Contents and bookmarks").performClick()
        compose.onNodeWithContentDescription("Reading settings").performClick()
        compose.onNodeWithText("go back to where I was").performClick()
        compose.onNodeWithText("go back to where I was").assertDoesNotExist()
        compose.onNodeWithContentDescription("Close reader").performClick()
        assertEquals(listOf(1, 1, 1, 1), listOf(back, contents, settings, returns))
        assertEquals(false, state.value.bookmarked)
    }

    @Test fun contentsNavigatesAndBookmarksCanBeRenamedAndRemoved() {
        RuntimeEnvironment.setFontScale(1.5f)
        val chapter = Link(requireNotNull(Url("chapter.xhtml")), title = "Chapter one")
        val bookmark = BookmarkEntity(7, 1, """{"href":"chapter.xhtml"}""", 0.25, "First stop", Instant.EPOCH)
        val saved = mutableStateOf(listOf(bookmark))
        var chapterSelected: Link? = null
        var bookmarkSelected: Long? = null
        compose.setContent {
            ReaderTheme(ReaderThemes.builtIn[2]) {
                ReaderContents(
                    listOf(chapter),
                    saved.value,
                    chapter.url(),
                    { chapterSelected = it },
                    { bookmarkSelected = it.id },
                    { id, name ->
                        saved.value =
                            saved.value.map { if (it.id == id) it.copy(label = name) else it }
                    },
                    { id -> saved.value = saved.value.filterNot { it.id == id } },
                )
            }
        }
        compose.onNodeWithText("Chapter one").assertIsDisplayed().performClick()
        assertEquals(chapter, chapterSelected)
        compose.onNodeWithText("bookmarks").performClick()
        compose.onNodeWithText("First stop").assertIsDisplayed().performClick()
        assertEquals(7L, bookmarkSelected)
        compose.onNodeWithContentDescription("Rename First stop").performClick()
        compose.onNode(hasSetTextAction()).performTextReplacement("Return here")
        compose.onNodeWithText("save").performClick()
        compose.onNodeWithText("Return here").assertIsDisplayed()
        compose.onNodeWithContentDescription("Remove Return here").performClick()
        compose.onNodeWithText("No bookmarks yet. Use the bookmark button to save your place.").assertIsDisplayed()
        assertEquals(emptyList<BookmarkEntity>(), saved.value)
    }
}
