package com.kmab.deft.feature.book

import android.app.DatePickerDialog
import android.os.Looper
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.assertIsSelected
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
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowDialog
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId

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
        compose.onNodeWithText("2m · 1 session").performScrollTo().performClick()
        compose.onNodeWithText("just browsing · excluded from totals").assertExists()
        compose.onNodeWithText("10m · 0 page turns").assertExists()
        compose.onNodeWithContentDescription("close popup").performClick()
        compose.onNodeWithText("reading history").performScrollTo().assertExists()
        assertEquals(120L, history.activeSeconds)
        assertEquals(1, history.sessionCount)
    }

    @Test fun statePickerIdentifiesTheCurrentChoiceAndWrapsCompactChoices() {
        show()
        compose.onNodeWithContentDescription("change reading state").performClick()
        compose.onNodeWithContentDescription("mark as finished").assertIsSelected()
        val notRead = compose.onNodeWithContentDescription("mark as not read").fetchSemanticsNode().boundsInRoot
        val reading = compose.onNodeWithContentDescription("mark as reading").fetchSemanticsNode().boundsInRoot
        org.junit.Assert.assertEquals(notRead.top, reading.top, 1f)
        org.junit.Assert.assertTrue(notRead.right <= reading.left)
    }

    @Test fun deletionWaitsForSpecificHistoryConfirmation() {
        var deleted: Long? = null
        show(BookTrackingActions(delete = { deleted = it }))
        compose.onNodeWithContentDescription("read-through options", substring = true).performScrollTo().performClick()
        compose.onNodeWithText("delete").performClick()
        assertNull(deleted)
        compose.onNodeWithText("delete read-through").performClick()
        compose.waitForIdle()
        assertEquals(1L, deleted)
    }

    @Test fun dateEditorRejectsInvalidDatesBeforeSaving() {
        var saved: Instant? = null
        show(BookTrackingActions(editDates = { _, at, _ -> saved = at }))
        compose.onNodeWithContentDescription("read-through options", substring = true).performScrollTo().performClick()
        compose.onNodeWithText("edit dates").performClick()
        pickStart(20)
        compose.onNodeWithText("save date").performClick()
        compose.onNodeWithText("The end date must be on or after the start date.").assertExists()
        assertNull(saved)
        pickStart(18)
        compose.onNodeWithText("save date").performClick()
        assertEquals("2026-11-18", saved?.localDate())
    }

    @Test fun sameDayEditAcceptsAnEveningStartAndMorningEnd() {
        val zone = ZoneId.systemDefault()
        val evening = LocalDateTime.parse("2026-11-19T20:00").atZone(zone).toInstant()
        val morning = LocalDateTime.parse("2026-11-20T09:00").atZone(zone).toInstant()
        var result: Pair<Instant, Instant?>? = null
        compose.setContent {
            DeftTheme { TrackingDates(evening, morning, {}) { start, end -> result = start to end } }
        }
        pickStart(20)
        compose.onNodeWithText("save date").performClick()
        val expected = LocalDateTime.parse("2026-11-20T20:00").atZone(zone).toInstant()
        assertEquals(expected to expected, result)
    }

    @Test fun ratingSupportsHalfStarsAndClearAndReviewSavesText() {
        var rating: Double? = 3.5
        var review = ""
        show(BookTrackingActions(rating = { rating = it }, review = { review = it }))
        compose.onNodeWithContentDescription("rating").performSemanticsAction(SemanticsActions.SetProgress) { it(4.5f) }
        assertEquals(4.5, rating)
        compose.onNodeWithContentDescription("clear rating").performClick()
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
        compose.onNodeWithContentDescription("change reading state").performScrollTo().performClick()
        compose.onNodeWithContentDescription("mark as finished").performClick()
        assertNull(finishedAt)
        compose.runOnIdle {
            val picker = ShadowDialog.getLatestDialog() as DatePickerDialog
            picker.getButton(DatePickerDialog.BUTTON_NEGATIVE).performClick()
        }
        compose.waitForIdle()
        assertNull(finishedAt)
        compose.onNodeWithContentDescription("change reading state").performClick()
        compose.onNodeWithContentDescription("mark as finished").performClick()
        compose.runOnIdle {
            val picker = ShadowDialog.getLatestDialog() as DatePickerDialog
            picker.getButton(DatePickerDialog.BUTTON_POSITIVE).performClick()
        }
        // AlertDialog posts its button listener after performClick returns.
        shadowOf(Looper.getMainLooper()).idle()
        org.junit.Assert.assertTrue(checkNotNull(finishedAt) >= before)
    }

    private fun pickStart(day: Int) {
        compose.onNodeWithText("started").performClick()
        compose.runOnIdle {
            val picker = ShadowDialog.getLatestDialog() as DatePickerDialog
            picker.updateDate(2026, 10, day)
            picker.getButton(DatePickerDialog.BUTTON_POSITIVE).performClick()
        }
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
