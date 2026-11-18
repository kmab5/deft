package com.kmab.deft.feature.library

import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.github.takahirom.roborazzi.ExperimentalRoborazziApi
import com.github.takahirom.roborazzi.RoborazziOptions
import com.github.takahirom.roborazzi.RoborazziTaskType
import com.github.takahirom.roborazzi.captureRoboImage
import com.kmab.deft.core.database.model.BookListItem
import com.kmab.deft.core.designsystem.DeftTheme
import com.kmab.deft.core.model.BookDisplaySize
import com.kmab.deft.core.model.BookFormat
import org.junit.After
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File
import java.time.Instant

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36], qualifiers = "w393dp-h852dp-mdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class BooksScreenshotTest {
    @get:Rule val compose = createComposeRule()

    @After fun resetFontScale() = RuntimeEnvironment.setFontScale(1f)

    @Test fun inkGrid() = render("books-ink-grid", dark = true)

    @Test fun paperList() = render("books-paper-list", dark = false, layout = PreviewLayout(grid = false))

    @Test fun largeTextDisplayControls() =
        render("books-display-large-text", dark = true, fontScale = 1.5f, sheet = "display")

    @Test fun smallGridStatusIcons() =
        render(
            "books-small-grid-status",
            dark = true,
            layout = PreviewLayout(size = BookDisplaySize.SMALL, statuses = true),
        )

    @Test fun smallListStatusLabels() =
        render(
            "books-small-list-status",
            dark = false,
            layout = PreviewLayout(grid = false, size = BookDisplaySize.SMALL, statuses = true),
        )

    @Test fun paperFilters() = render("books-paper-filters", dark = false, sheet = "filters")

    @Test fun inkSort() = render("books-ink-sort", dark = true, sheet = "sort")

    @OptIn(ExperimentalRoborazziApi::class)
    private fun render(
        name: String,
        dark: Boolean,
        layout: PreviewLayout = PreviewLayout(),
        fontScale: Float = 1f,
        sheet: String? = null,
    ) {
        RuntimeEnvironment.setFontScale(fontScale)
        val state =
            mutableStateOf(
                BooksUiState(
                    books = sampleBooks(layout.statuses),
                    hasFolders = true,
                    isLoading = false,
                    gridView = layout.grid,
                    gridSize = layout.size,
                    listSize = layout.size,
                ),
            )
        compose.setContent {
            DeftTheme(dark) {
                BooksScreen(
                    state.value,
                    {},
                    {},
                    {},
                    { state.value = state.value.copy(gridView = it) },
                    { state.value = state.value.copy(sort = it) },
                    { state.value = state.value.copy(showMissing = it) },
                    { _, _ -> },
                    { state.value = state.value.copy(reverseSort = it) },
                    { state.value = state.value.copy(gridSize = it) },
                    { state.value = state.value.copy(listSize = it) },
                )
            }
        }
        if (sheet != null) compose.onNodeWithText(sheet).performClick()
        if (sheet == "display") {
            compose.onNodeWithText("small").assertIsDisplayed()
            compose.onNodeWithText("medium").assertIsDisplayed()
            compose.onNodeWithText("large").assertIsDisplayed()
            compose.onNodeWithText("done").assertIsDisplayed()
        }
        if (layout.statuses) {
            if (layout.grid) {
                compose.onNodeWithContentDescription("missing", useUnmergedTree = true).assertIsDisplayed()
                compose.onNodeWithContentDescription("protected", useUnmergedTree = true).assertIsDisplayed()
            } else {
                compose.onNodeWithText("epub · missing · protected", useUnmergedTree = true).assertIsDisplayed()
            }
        }
        compose.waitForIdle()
        val output = File("build/reports/screenshots/$name.png").apply { parentFile?.mkdirs() }
        val node = if (sheet == null) compose.onNodeWithTag("books_screen") else compose.onNodeWithTag("books_sheet")
        node.captureRoboImage(output.path, roborazziOptions = RoborazziOptions(taskType = RoborazziTaskType.Record))
    }

    private fun sampleBooks(statuses: Boolean): List<BookListItem> {
        val titles =
            listOf(
                "The Well of Ascension",
                "A Wizard of Earthsea",
                "The Left Hand of Darkness",
                "Kindred",
                "The Fifth Season",
                "Piranesi",
            )
        val authors =
            listOf(
                "Brandon Sanderson",
                "Ursula K. Le Guin",
                "Ursula K. Le Guin",
                "Octavia E. Butler",
                "N. K. Jemisin",
                "Susanna Clarke",
            )
        return titles.mapIndexed { index, title ->
            BookListItem(
                index.toLong(),
                title,
                title,
                authors[index],
                BookFormat.EPUB,
                null,
                Instant.EPOCH,
                statuses && index == 0,
                statuses && index == 0,
            )
        }
    }

    private data class PreviewLayout(
        val grid: Boolean = true,
        val size: BookDisplaySize = BookDisplaySize.MEDIUM,
        val statuses: Boolean = false,
    )
}
