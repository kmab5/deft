package com.kmab.deft.feature.book

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import com.github.takahirom.roborazzi.ExperimentalRoborazziApi
import com.github.takahirom.roborazzi.RoborazziOptions
import com.github.takahirom.roborazzi.RoborazziTaskType
import com.github.takahirom.roborazzi.captureRoboImage
import com.kmab.deft.core.database.entity.BookEntity
import com.kmab.deft.core.database.entity.BookFileEntity
import com.kmab.deft.core.database.model.BookDetails
import com.kmab.deft.core.designsystem.DeftTheme
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

@OptIn(ExperimentalRoborazziApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36], qualifiers = "w393dp-h852dp-420dpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class BookDetailScreenshotTest {
    @get:Rule val compose = createComposeRule()

    @After fun resetFontScale() = RuntimeEnvironment.setFontScale(1f)

    @Test fun paper() = render(false, 1f, "paper")

    @Test fun inkLargeText() = render(true, 1.5f, "ink-large")

    @Test
    @Config(qualifiers = "w852dp-h393dp-420dpi")
    fun paperLandscape() = render(false, 1f, "paper-landscape")

    private fun render(
        dark: Boolean,
        fontScale: Float,
        name: String,
    ) {
        RuntimeEnvironment.setFontScale(fontScale)
        val book =
            BookEntity(
                id = 1,
                contentHash = "a".repeat(64),
                format = BookFormat.EPUB,
                title = "A Wizard of Earthsea",
                sortTitle = "wizard of earthsea",
                addedAt = Instant.EPOCH,
                currentLocator = "{\"href\":\"chapter.xhtml\"}",
                totalProgression = 0.28,
                description = "A young wizard begins a journey through the islands of Earthsea.",
                publisher = "Parnassus Press",
                publishedDate = "1968",
                language = "en",
            )
        val file =
            BookFileEntity(
                1,
                1,
                "content://books/tree/root",
                "content://books/file/one",
                "Books/Earthsea.epub",
                12345,
                1,
                false,
                Instant.EPOCH,
            )
        val details = BookDetails(book, "Ursula K. Le Guin", listOf(file))
        compose.setContent {
            DeftTheme(
                darkTheme = dark,
            ) { BookDetailScreen(BookDetailState(details, false), {}, {}, {}, {}) }
        }
        compose.onNodeWithText("A Wizard of Earthsea").assertIsDisplayed()
        compose.onNodeWithText("Ursula K. Le Guin").assertIsDisplayed()
        compose.onNodeWithText("continue").assertIsDisplayed()
        val output = File("build/reports/screenshots/book-$name.png").also { it.parentFile?.mkdirs() }
        compose.onRoot().captureRoboImage(
            output.path,
            roborazziOptions = RoborazziOptions(taskType = RoborazziTaskType.Record),
        )
    }
}
