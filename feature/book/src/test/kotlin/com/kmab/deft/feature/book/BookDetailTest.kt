package com.kmab.deft.feature.book

import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import com.kmab.deft.core.database.entity.BookEntity
import com.kmab.deft.core.database.entity.BookFileEntity
import com.kmab.deft.core.database.model.BookDetails
import com.kmab.deft.core.designsystem.DeftNoticeHost
import com.kmab.deft.core.designsystem.DeftNoticeState
import com.kmab.deft.core.designsystem.DeftTheme
import com.kmab.deft.core.designsystem.LocalDeftNoticeState
import com.kmab.deft.core.model.BookFormat
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.time.Instant

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class BookDetailTest {
    @get:Rule val compose = createComposeRule()

    @Test fun readUsesSelectedBookAndDisplaysItsFiles() {
        var reads = 0
        compose.setContent {
            DeftTheme {
                BookDetailScreen(
                    BookDetailState(fixture(), false),
                    {},
                    { reads++ },
                    {},
                    {},
                )
            }
        }
        compose.onNodeWithText("read").performScrollTo().performClick()
        assertEquals(1, reads)
        compose.onNodeWithText("Books/Earthsea.epub").performScrollTo().assertExists()
    }

    @Test fun protectedReadShowsNoticeWithoutOpeningContent() {
        var reads = 0
        val notices = DeftNoticeState()
        val details = fixture().let { it.copy(book = it.book.copy(drmProtected = true)) }
        compose.setContent {
            DeftTheme {
                CompositionLocalProvider(LocalDeftNoticeState provides notices) {
                    Box {
                        BookDetailScreen(BookDetailState(details, false), {}, { reads++ }, {}, {})
                        DeftNoticeHost(notices)
                    }
                }
            }
        }
        compose.onNodeWithText("read").performScrollTo().performClick()
        compose.onNodeWithText("This file is DRM-protected. Deft can't open it.").assertExists()
        assertEquals(0, reads)
    }

    @Test fun forgetRequiresSeparateExplicitConfirmation() {
        var forgotten = 0
        compose.setContent {
            DeftTheme {
                BookDetailScreen(
                    BookDetailState(fixture(), false),
                    {},
                    {},
                    {},
                    { forgotten++ },
                )
            }
        }
        compose.onNodeWithContentDescription("book actions").performClick()
        compose.onNodeWithText("forget this book").performClick()
        assertEquals(0, forgotten)
        compose.onNodeWithText("forget").performClick()
        compose.waitForIdle()
        assertEquals(1, forgotten)
    }

    @Test fun missingMetadataHasExplicitReadableLabels() {
        val details = fixture().let { it.copy(book = it.book.copy(title = " "), authorsDisplay = "\t") }
        compose.setContent { DeftTheme { BookDetailScreen(BookDetailState(details, false), {}, {}, {}, {}) } }
        compose.onNodeWithText("<no-title>").assertIsDisplayed()
        compose.onNodeWithText("<no-author>").assertIsDisplayed()
        compose.onNodeWithText("read").assertIsDisplayed()
        assertEquals(" ", details.book.title)
        assertEquals("\t", details.authorsDisplay)
    }

    @Test fun editionMetadataHasLabelsAndDescriptionStaysReadable() {
        val details =
            fixture().let {
                it.copy(
                    book =
                        it.book.copy(
                            description = "<p>A journey through the islands of Earthsea.</p>",
                            publisher = "Parnassus Press",
                            publishedDate = "1968",
                            language = "en",
                            isbn13 = "9780547773742",
                        ),
                )
            }
        compose.setContent { DeftTheme { BookDetailScreen(BookDetailState(details, false), {}, {}, {}, {}) } }
        compose.onNodeWithText("A journey through the islands of Earthsea.").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("publisher").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("Parnassus Press").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("published").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("1968").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("isbn").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("9780547773742").performScrollTo().assertIsDisplayed()
    }

    @Test fun missingBookKeepsItsDetailAndHasAnHonestReadBlock() {
        val details = fixture().copy(files = emptyList())
        assertEquals("Deft can't find this file anymore. Its history is safe.", readBlockReason(details))
    }

    @Test fun assessmentsAndHistoryRemainAvailableForProtectedMissingBooks() {
        val details = fixture().let { it.copy(book = it.book.copy(drmProtected = true), files = emptyList()) }
        compose.setContent { DeftTheme { BookDetailScreen(BookDetailState(details, false), {}, {}, {}, {}) } }
        compose.onNodeWithText("your reading").performScrollTo().assertExists()
        compose.onNodeWithText("your review").performScrollTo().assertExists()
        compose.onNodeWithText("reading history").performScrollTo().assertExists()
    }

    @Test fun resetToNotReadRequiresHistoryLossConfirmation() {
        var reset = false
        compose.setContent {
            DeftTheme {
                BookDetailScreen(
                    BookDetailState(fixture(), false),
                    {},
                    {},
                    {},
                    {},
                    actions = BookTrackingActions(setState = { _, _, confirmed -> reset = confirmed }),
                )
            }
        }
        compose.onNodeWithText("change state").performScrollTo().performClick()
        compose.onNode(hasText("not read") and hasClickAction()).performClick()
        assertEquals(false, reset)
        compose.onNodeWithText("erase reading history").performClick()
        compose.waitForIdle()
        assertEquals(true, reset)
    }

    private fun fixture(): BookDetails {
        val at = Instant.parse("2026-11-18T12:00:00Z")
        return BookDetails(
            BookEntity(
                id = 1,
                contentHash = "a".repeat(64),
                format = BookFormat.EPUB,
                title = "A Wizard of Earthsea",
                sortTitle = "wizard of earthsea",
                addedAt = at,
            ),
            "Ursula K. Le Guin",
            listOf(
                BookFileEntity(
                    1,
                    1,
                    "content://books/tree/root",
                    "content://books/file/one",
                    "Books/Earthsea.epub",
                    12345,
                    1,
                    false,
                    at,
                ),
            ),
        )
    }
}
