package com.kmab.deft.feature.book

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.performTextReplacement
import com.kmab.deft.core.designsystem.DeftTheme
import com.kmab.deft.core.model.BookAssessment
import com.kmab.deft.core.model.BookTrackingSnapshot
import com.kmab.deft.core.model.ReadThrough
import com.kmab.deft.core.model.ReadThroughOutcome
import com.kmab.deft.core.model.ReadingSession
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.time.Instant

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class BookHistoryTest {
    @get:Rule val compose = createComposeRule()
    private val start = Instant.parse("2026-11-19T12:00:00Z")
    private val end = start.plusSeconds(7200)
    private val history =
        BookTrackingSnapshot(
            1,
            listOf(ReadThrough(1, 1, start, end, ReadThroughOutcome.FINISHED)),
            listOf(
                ReadingSession("read", 1, 1, start, start.plusSeconds(120), 120),
                ReadingSession("browse", 1, 1, end, end.plusSeconds(600), 600, browsing = true),
            ),
            BookAssessment(3.5, true, "A private thought."),
        )

    @Test fun historyLabelsBrowsingWithoutAddingItToTotals() {
        show()
        compose.onNodeWithText("sessions").performScrollTo().performClick()
        compose.onNodeWithText("just browsing · excluded from totals").assertExists()
        compose.onNodeWithText("10m · 0 page turns").assertExists()
        compose.onNodeWithContentDescription("close popup").performClick()
        compose.onNodeWithText("reading history").performScrollTo().assertExists()
        assertEquals(120L, history.activeSeconds)
        assertEquals(1, history.sessionCount)
    }

    @Test fun deletionWaitsForSpecificHistoryConfirmation() {
        var deleted: Long? = null
        show(BookTrackingActions(delete = { deleted = it }))
        compose.onNodeWithText("delete").performScrollTo().performClick()
        assertNull(deleted)
        compose.onNodeWithText("delete read-through").performClick()
        compose.waitForIdle()
        assertEquals(1L, deleted)
    }

    @Test fun dateEditorRejectsInvalidDatesBeforeSaving() {
        var saved: Instant? = null
        show(BookTrackingActions(editDates = { _, at, _ -> saved = at }))
        compose.onNodeWithText("edit dates").performScrollTo().performClick()
        compose.onNodeWithText("started").performTextReplacement("bad date")
        compose.onNodeWithText("save date").performClick()
        compose.onNodeWithText("Enter a valid date as YYYY-MM-DD.").assertExists()
        assertNull(saved)
        compose.onNodeWithText("started").performTextReplacement("2026-11-18")
        compose.onNodeWithText("save date").performClick()
        assertEquals("2026-11-18", saved?.localDate())
    }

    @Test fun ratingSupportsHalfStarsAndClearAndReviewSavesText() {
        var rating: Double? = 3.5
        var review = ""
        show(BookTrackingActions(rating = { rating = it }, review = { review = it }))
        compose.onNodeWithContentDescription("rating").performSemanticsAction(SemanticsActions.SetProgress) { it(4.5f) }
        assertEquals(4.5, rating)
        compose.onNodeWithText("clear rating").performClick()
        assertNull(rating)
        compose.onNodeWithText("edit review").performScrollTo().performClick()
        compose.onNodeWithText("review").performTextReplacement("My own words.")
        compose.onNodeWithText("save review").performClick()
        assertEquals("My own words.", review)
    }

    @Test fun manualFinishAsksForDateBeforeChangingState() {
        var finishedAt: Instant? = null
        val before = Instant.now()
        show(
            BookTrackingActions(setState = { state, at, _ ->
                if (state == com.kmab.deft.core.model.BookState.FINISHED) finishedAt = at
            }),
        )
        compose.onNodeWithText("change state").performScrollTo().performClick()
        compose.onNode(hasText("finished") and hasClickAction()).performClick()
        compose.onNodeWithText("finish date").assertExists()
        assertNull(finishedAt)
        compose.onNodeWithText("save date").performClick()
        org.junit.Assert.assertTrue(checkNotNull(finishedAt) >= before)
    }

    private fun show(actions: BookTrackingActions = BookTrackingActions()) {
        compose.setContent {
            DeftTheme {
                Column(
                    Modifier.verticalScroll(rememberScrollState()),
                ) { BookTrackingCards(history, false, actions) }
            }
        }
    }
}
