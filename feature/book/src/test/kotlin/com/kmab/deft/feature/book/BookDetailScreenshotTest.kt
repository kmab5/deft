package com.kmab.deft.feature.book

import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performSemanticsAction
import com.github.takahirom.roborazzi.ExperimentalRoborazziApi
import com.github.takahirom.roborazzi.RoborazziOptions
import com.github.takahirom.roborazzi.RoborazziTaskType
import com.github.takahirom.roborazzi.captureRoboImage
import com.kmab.deft.core.database.entity.BookEntity
import com.kmab.deft.core.database.entity.BookFileEntity
import com.kmab.deft.core.database.model.BookDetails
import com.kmab.deft.core.designsystem.DeftTheme
import com.kmab.deft.core.model.BookAssessment
import com.kmab.deft.core.model.BookFormat
import com.kmab.deft.core.model.BookTrackingSnapshot
import com.kmab.deft.core.model.ReadThrough
import com.kmab.deft.core.model.ReadThroughOutcome
import com.kmab.deft.core.model.ReadingSession
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
        val details = bookFixture()
        val tracking = trackingFixture()
        compose.setContent {
            DeftTheme(darkTheme = dark) {
                BookDetailScreen(BookDetailState(details, false, tracking = tracking), {}, {}, {}, {})
            }
        }
        compose.onNodeWithText("A Wizard of Earthsea").assertIsDisplayed()
        compose.onNodeWithText("Ursula K. Le Guin").assertIsDisplayed()
        compose.onNodeWithText("continue").assertIsDisplayed()
        capture("book-$name")
        scrollSectionToTop("your reading")
        capture("book-tracking-$name")
        scrollSectionToTop("reading history")
        compose.onNodeWithText("reading history").assertIsDisplayed()
        capture("book-history-$name")
    }

    private fun bookFixture(): BookDetails {
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
        return BookDetails(book, "Ursula K. Le Guin", listOf(file))
    }

    private fun trackingFixture(): BookTrackingSnapshot {
        val firstStart = Instant.parse("2026-11-10T15:00:00Z")
        val finished = Instant.parse("2026-11-14T17:30:00Z")
        val reread = Instant.parse("2026-11-19T16:00:00Z")
        return BookTrackingSnapshot(
            bookId = 1,
            readThroughs =
                listOf(
                    ReadThrough(1, 1, firstStart, finished, ReadThroughOutcome.FINISHED),
                    ReadThrough(2, 1, reread, null, ReadThroughOutcome.IN_PROGRESS),
                ),
            sessions = sessionFixtures(firstStart, finished, reread),
            assessment =
                BookAssessment(
                    4.5,
                    true,
                    "Quiet, precise, and full of wonder. Returning to the islands feels like coming home.",
                ),
        )
    }

    private fun sessionFixtures(
        firstStart: Instant,
        finished: Instant,
        reread: Instant,
    ): List<ReadingSession> =
        listOf(
            ReadingSession(
                "first-evening",
                1,
                1,
                firstStart,
                firstStart.plusSeconds(2400),
                2400,
                pagesTurned = 28,
                zoneOffsetSeconds = 10800,
            ),
            ReadingSession(
                "last-chapter",
                1,
                1,
                finished.minusSeconds(1800),
                finished,
                1800,
                pagesTurned = 22,
                zoneOffsetSeconds = 10800,
            ),
            ReadingSession(
                "browsing",
                1,
                1,
                finished.plusSeconds(86400),
                finished.plusSeconds(86700),
                300,
                browsing = true,
                pagesTurned = 3,
                zoneOffsetSeconds = 10800,
            ),
            ReadingSession(
                "reread",
                1,
                2,
                reread,
                reread.plusSeconds(1500),
                1500,
                pagesTurned = 18,
                zoneOffsetSeconds = 10800,
            ),
        )

    private fun capture(name: String) {
        val output = File("build/reports/screenshots/$name.png").also { it.parentFile?.mkdirs() }
        compose.onRoot().captureRoboImage(
            output.path,
            roborazziOptions = RoborazziOptions(taskType = RoborazziTaskType.Record),
        )
    }

    private fun scrollSectionToTop(label: String) {
        val heading = compose.onNodeWithText(label)
        heading.performScrollTo()
        val targetTop = 72f * compose.density.density
        val remaining = (heading.fetchSemanticsNode().boundsInRoot.top - targetTop).coerceAtLeast(0f)
        compose.onNodeWithTag("book_detail").performSemanticsAction(SemanticsActions.ScrollBy) {
            it(0f, remaining)
        }
        compose.waitForIdle()
    }
}
