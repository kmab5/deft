package com.kmab.deft.feature.notes

import androidx.compose.material3.Surface
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import com.github.takahirom.roborazzi.ExperimentalRoborazziApi
import com.github.takahirom.roborazzi.RoborazziOptions
import com.github.takahirom.roborazzi.RoborazziTaskType
import com.github.takahirom.roborazzi.captureRoboImage
import com.kmab.deft.core.designsystem.DeftTheme
import com.kmab.deft.core.designsystem.LocalDeftColors
import com.kmab.deft.core.model.BookFormat
import com.kmab.deft.core.model.HighlightColor
import com.kmab.deft.library.notes.HighlightDraft
import com.kmab.deft.library.notes.NoteEntry
import com.kmab.deft.library.notes.NoteFilter
import com.kmab.deft.library.notes.NoteType
import org.junit.After
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.time.Instant

@OptIn(ExperimentalRoborazziApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36], qualifiers = "w393dp-h852dp-420dpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class NotesScreenshotTest {
    @get:Rule val compose = createComposeRule()

    @After fun resetFontScale() = RuntimeEnvironment.setFontScale(1f)

    @Test fun paper() = render(false, 1f, "paper")

    @Test
    @Config(qualifiers = "w852dp-h393dp-land-420dpi")
    fun inkLargeLandscape() = render(true, 1.5f, "ink-large-landscape")

    private fun render(
        dark: Boolean,
        fontScale: Float,
        name: String,
    ) {
        RuntimeEnvironment.setFontScale(fontScale)
        val entry =
            NoteEntry(
                id = 1,
                bookId = 1,
                bookTitle = "A Wizard of Earthsea",
                authors = "Ursula K. Le Guin",
                bookFormat = BookFormat.EPUB,
                missing = true,
                drmProtected = false,
                locator = """{"href":"chapter.xhtml","title":"The School for Wizards"}""",
                totalProgression = 0.28,
                text = "The passage I marked tells of islands divided by the sea and held together by their names.",
                note = "Return to this when the story comes back to Roke.",
                color = HighlightColor.PURPLE,
                label = null,
                createdAt = Instant.EPOCH,
                updatedAt = Instant.EPOCH,
                type = NoteType.HIGHLIGHT,
            )
        compose.setContent {
            var editing by remember { mutableStateOf(false) }
            DeftTheme(darkTheme = dark) {
                Surface(color = LocalDeftColors.current.bg) {
                    NotesContent(
                        NotesList.Ready(listOf(entry)),
                        listOf(entry),
                        NoteFilter(),
                        {},
                        {},
                        {},
                        { editing = true },
                        null,
                        false,
                        true,
                    )
                    if (editing) {
                        HighlightEditor(
                            HighlightDraft(1, entry.locator, entry.text, HighlightColor.PURPLE, entry.note, id = 1),
                            {},
                            { editing = false },
                            {},
                        )
                    }
                }
            }
        }
        val options = RoborazziOptions(taskType = RoborazziTaskType.Record)
        compose.onNodeWithText("search notes").assertIsDisplayed()
        compose.onRoot().captureRoboImage("build/reports/screenshots/notes-$name.png", roborazziOptions = options)
        compose.onNodeWithContentDescription("edit annotation").performClick()
        compose.onNodeWithText("save").assertIsDisplayed()
        compose.onNodeWithText("cancel").assertIsDisplayed()
        compose.onNodeWithTag("notes_editor_sheet").captureRoboImage(
            "build/reports/screenshots/notes-editor-$name.png",
            roborazziOptions = options,
        )
    }
}
