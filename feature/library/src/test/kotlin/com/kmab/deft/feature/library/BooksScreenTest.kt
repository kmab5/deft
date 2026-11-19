package com.kmab.deft.feature.library

import android.content.Context
import android.provider.Settings
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeDown
import androidx.compose.ui.test.swipeUp
import androidx.test.core.app.ApplicationProvider
import com.kmab.deft.core.database.model.BookListItem
import com.kmab.deft.core.designsystem.DeftTheme
import com.kmab.deft.core.model.BookDisplaySize
import com.kmab.deft.core.model.BookFormat
import com.kmab.deft.core.model.BookSort
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.time.Instant

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class BooksScreenTest {
    @get:Rule val compose = createComposeRule()

    @Test fun noFoldersUsesApprovedCopyAndOpensFolderSettings() {
        var additions = 0
        compose.setContent {
            DeftTheme { BooksScreen(BooksUiState(isLoading = false), {}, { additions++ }, {}, {}, {}, {}, { _, _ -> }) }
        }
        compose.onNodeWithText("no books yet.").assertIsDisplayed()
        compose.onNodeWithText("Point Deft at a folder and it will find them.").assertIsDisplayed()
        compose.onNodeWithText("add a folder").performClick()
        assertEquals(1, additions)
    }

    @Test fun emptyFolderOffersRealRescanAndSettingsOnly() {
        var scans = 0
        var settings = 0
        compose.setContent {
            DeftTheme {
                BooksScreen(BooksUiState(isLoading = false, hasFolders = true), {
                    settings++
                }, {}, { scans++ }, {}, {}, {}, { _, _ -> })
            }
        }
        compose.onNodeWithText("nothing here.").assertIsDisplayed()
        compose.onNodeWithText("rescan").performClick()
        compose.onNodeWithText("settings").performClick()
        assertEquals(1, scans)
        assertEquals(1, settings)
        compose.onNodeWithText("categories").assertDoesNotExist()
    }

    @Test fun missingProtectedBadgesAndGridListControlAreAccessible() {
        val book = BookListItem(1, "A book", "A book", "A writer", BookFormat.EPUB, null, Instant.EPOCH, true, true)
        val state = mutableStateOf(BooksUiState(books = listOf(book), isLoading = false, hasFolders = true))
        compose.setContent {
            DeftTheme {
                BooksScreen(state.value, {}, {}, {}, {
                    state.value =
                        state.value.copy(gridView = it)
                }, {}, {}, { _, _ -> })
            }
        }
        compose.onNodeWithText("missing", useUnmergedTree = true).assertIsDisplayed()
        compose.onNodeWithText("protected", useUnmergedTree = true).assertIsDisplayed()
        compose.onNodeWithText("A book").assertHasClickAction()
        compose.onNodeWithContentDescription("display options").performClick()
        compose.onNodeWithText("list").performClick()
        compose.onNodeWithContentDescription("close popup").performClick()
        compose.onNodeWithTag("book_list").assertIsDisplayed()
        compose.onNodeWithTag("book_grid").assertDoesNotExist()
    }

    @Test fun missingMetadataRemainsReadableInGridAndList() {
        val book = BookListItem(1, "  ", "", "\t", BookFormat.EPUB, null, Instant.EPOCH, false, false)
        val state = mutableStateOf(BooksUiState(books = listOf(book), isLoading = false, hasFolders = true))
        compose.setContent {
            DeftTheme {
                BooksScreen(
                    state.value,
                    {},
                    {},
                    {},
                    { state.value = state.value.copy(gridView = it) },
                    {},
                    {},
                    { _, _ -> },
                )
            }
        }
        compose.onNodeWithText("<no-title>").assertIsDisplayed()
        compose.onNodeWithText("<no-author>").assertIsDisplayed()
        compose.onNodeWithContentDescription("display options").performClick()
        compose.onNodeWithText("list").performClick()
        compose.onNodeWithContentDescription("close popup").performClick()
        compose.onNodeWithText("<no-title>").assertIsDisplayed()
        compose.onNodeWithText("<no-author>").assertIsDisplayed()
        assertEquals("  ", book.title)
        assertEquals("\t", book.authorsDisplay)
    }

    @Test fun focusedSheetsApplyFormatsSortAndMissingLive() {
        val state = mutableStateOf(BooksUiState(isLoading = false, hasFolders = true))
        compose.setContent {
            DeftTheme {
                BooksScreen(
                    state.value,
                    {},
                    {},
                    {},
                    {},
                    { state.value = state.value.copy(sort = it) },
                    { state.value = state.value.copy(showMissing = it) },
                    { format, enabled ->
                        state.value =
                            state.value.copy(
                                formatFilter =
                                    if (enabled) {
                                        state.value.formatFilter + format
                                    } else {
                                        state.value.formatFilter -
                                            format
                                    },
                            )
                    },
                )
            }
        }
        compose.onNodeWithContentDescription("filter books").performClick()
        compose.onNodeWithText("txt").performClick()
        compose
            .onNodeWithContentDescription("show missing")
            .performScrollTo()
            .assertIsDisplayed()
            .performClick()
        assertFalse(BookFormat.TXT in state.value.formatFilter)
        assertEquals(true, state.value.showMissing)
        compose.onNodeWithContentDescription("close popup").performClick()
        compose.onNodeWithContentDescription("sort books").performClick()
        compose.onNodeWithText("author").performClick()
        assertEquals(BookSort.AUTHOR, state.value.sort)
        compose.onNodeWithContentDescription("close popup").performClick()
        compose.onNodeWithText("author").assertIsDisplayed()
    }

    @Test fun displaySizesAndSortDirectionApplyWithoutChangingTheOtherLayoutSize() {
        val state = mutableStateOf(BooksUiState(isLoading = false))
        compose.setContent {
            DeftTheme {
                BooksScreen(
                    state.value,
                    {},
                    {},
                    {},
                    { state.value = state.value.copy(gridView = it) },
                    { state.value = state.value.copy(sort = it) },
                    {},
                    { _, _ -> },
                    { state.value = state.value.copy(reverseSort = it) },
                    { state.value = state.value.copy(gridSize = it) },
                    { state.value = state.value.copy(listSize = it) },
                )
            }
        }
        compose.onNodeWithContentDescription("display options").performClick()
        compose.onNodeWithText("small").performClick()
        compose.onNodeWithText("list").performClick()
        compose.onNodeWithText("large").performClick()
        assertEquals(BookDisplaySize.SMALL, state.value.gridSize)
        assertEquals(BookDisplaySize.LARGE, state.value.listSize)
        compose.onNodeWithContentDescription("close popup").performClick()
        compose.onNodeWithContentDescription("sort books").performClick()
        compose.onNodeWithText("last read").performClick()
        compose.onNodeWithText("oldest first").performClick()
        compose.onNodeWithText("Books with no reading history stay at the end.").assertIsDisplayed()
        compose.onNodeWithContentDescription("close popup").performClick()
        assertEquals(BookSort.LAST_READ, state.value.sort)
        assertEquals(true, state.value.reverseSort)
        compose.onNodeWithText("last read").assertIsDisplayed()
    }

    @Test fun runningProgressDisappearsWhenScanningStops() {
        val state =
            mutableStateOf(
                BooksUiState(isLoading = false, isScanning = true, scanMessage = "scanning · 30 files checked"),
            )
        compose.setContent { DeftTheme { BooksScreen(state.value, {}, {}, {}, {}, {}, {}, { _, _ -> }) } }
        compose.onNodeWithText("scanning · 30 files checked").assertIsDisplayed()
        compose.runOnIdle { state.value = state.value.copy(isScanning = false) }
        compose.onNodeWithText("scanning · 30 files checked").assertDoesNotExist()
    }

    @Test fun displayChangesAndPopupCloseWorkWithSystemAnimationsDisabled() {
        val resolver = ApplicationProvider.getApplicationContext<Context>().contentResolver
        val original = Settings.Global.getFloat(resolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f)
        Settings.Global.putFloat(resolver, Settings.Global.ANIMATOR_DURATION_SCALE, 0f)
        try {
            val state = mutableStateOf(BooksUiState(isLoading = false))
            compose.setContent {
                DeftTheme {
                    BooksScreen(
                        state.value,
                        {},
                        {},
                        {},
                        { state.value = state.value.copy(gridView = it) },
                        {},
                        {},
                        { _, _ -> },
                    )
                }
            }
            compose.onNodeWithContentDescription("display options").performClick()
            compose.onNodeWithText("list").performClick()
            assertFalse(state.value.gridView)
            compose.onNodeWithContentDescription("close popup").performClick()
            compose.onNodeWithTag("books_sheet").assertDoesNotExist()
        } finally {
            Settings.Global.putFloat(resolver, Settings.Global.ANIMATOR_DURATION_SCALE, original)
        }
    }

    @Test fun completedScansAndErrorsDoNotOccupyTheBooksBody() {
        val previous =
            com.kmab.deft.library.scanner
                .ScanReport(2, 0, 0, 0, 0, 0, emptyList())
        val message = "Scan stopped by Android. Try rescan."
        compose.setContent {
            DeftTheme {
                BooksScreen(
                    BooksUiState(isLoading = false, hasFolders = true, scanReport = previous, scanMessage = message),
                    {},
                    {},
                    {},
                    {},
                    {},
                    {},
                    { _, _ -> },
                )
            }
        }
        compose.onNodeWithText(message).assertDoesNotExist()
        compose.onNodeWithText(previous.summary()).assertDoesNotExist()
    }

    @Test fun cancelledScanStillOffersItsSavedDetails() {
        var opens = 0
        compose.setContent {
            DeftTheme {
                BooksScreen(
                    BooksUiState(isLoading = false, hasScanDetails = true),
                    {},
                    {},
                    {},
                    {},
                    {},
                    {},
                    { _, _ -> },
                    onScanDetails = { opens++ },
                )
            }
        }
        compose.onNodeWithContentDescription("more options").performClick()
        compose.onNodeWithText("last scan").performClick()
        assertEquals(1, opens)
    }

    @Test fun lastScanOverflowOpensDedicatedDetailsWithoutNavigatingSettings() {
        var opens = 0
        val report =
            com.kmab.deft.library.scanner
                .ScanReport(0, 0, 0, 0, 0, 0, listOf("private-uri"))
        compose.setContent {
            DeftTheme {
                BooksScreen(
                    BooksUiState(
                        isLoading = false,
                        hasFolders = true,
                        scanReport = report,
                        scanMessage = "1 folder unavailable. Check library settings.",
                    ),
                    {},
                    {},
                    {},
                    {},
                    {},
                    {},
                    { _, _ -> },
                    onScanDetails = { opens++ },
                )
            }
        }
        compose.onNodeWithText("1 folder unavailable. Check library settings.").assertDoesNotExist()
        compose.onNodeWithContentDescription("more options").performClick()
        compose.onNodeWithText("last scan").performClick()
        assertEquals(1, opens)
        compose.onNodeWithText("private-uri").assertDoesNotExist()
    }

    @Test fun bookDetailsAndContinueUseTheirOwnBookIdentity() {
        val book =
            BookListItem(
                7,
                "Earthsea",
                "earthsea",
                "Ursula K. Le Guin",
                BookFormat.EPUB,
                null,
                Instant.EPOCH,
                false,
                false,
                totalProgression = 0.25,
            )
        var detailId = 0L
        var readId = 0L
        compose.setContent {
            DeftTheme {
                BooksScreen(
                    BooksUiState(books = listOf(book), isLoading = false, continueReading = book),
                    {},
                    {},
                    {},
                    {},
                    {},
                    {},
                    { _, _ -> },
                    onOpenBook = { detailId = it },
                    onContinueReading = { readId = it },
                )
            }
        }
        compose.onNodeWithTag("book_7").performClick()
        assertEquals(7L, detailId)
        assertEquals(0L, readId)
        compose.onNodeWithTag("continue_reading").performClick()
        assertEquals(7L, readId)
        compose.onNodeWithText("25%").assertExists()
        compose.onNodeWithTag("floating_continue").performClick()
        assertEquals(7L, readId)
    }

    @Test fun searchFindsAuthorsAndClearingRestoresTheLibrary() {
        val books =
            listOf(
                BookListItem(
                    1,
                    "Earthsea",
                    "earthsea",
                    "Ursula Le Guin",
                    BookFormat.EPUB,
                    null,
                    Instant.EPOCH,
                    false,
                    false,
                ),
                BookListItem(2, "Dune", "dune", "Frank Herbert", BookFormat.EPUB, null, Instant.EPOCH, false, false),
            )
        compose.setContent {
            DeftTheme {
                BooksScreen(
                    BooksUiState(books = books, isLoading = false, hasFolders = true),
                    {},
                    {},
                    {},
                    {},
                    {},
                    {},
                    { _, _ -> },
                )
            }
        }
        compose.onNodeWithContentDescription("search books").performClick()
        compose.onNodeWithTag("library_search").performTextInput("HERBERT")
        compose.onNodeWithTag("book_2").assertExists()
        compose.onNodeWithTag("book_1").assertDoesNotExist()
        compose.onNodeWithContentDescription("clear search").performClick()
        compose.onNodeWithTag("book_1").assertExists()
        compose.onNodeWithTag("book_2").assertExists()
        compose.onNodeWithTag("library_search").performTextInput("not in this library")
        compose.onNodeWithText("no matching books.").assertExists()
        compose.onNodeWithText("clear search").performClick()
        compose.onNodeWithTag("book_1").assertExists()
    }

    @Test fun scrollingHidesContinueHeaderAndUpwardMotionOffersReturnToTop() {
        val books =
            (1L..60L).map {
                BookListItem(it, "Book $it", "book $it", "Writer", BookFormat.EPUB, null, Instant.EPOCH, false, false)
            }
        compose.setContent {
            DeftTheme {
                BooksScreen(
                    BooksUiState(books = books, gridView = false, isLoading = false, continueReading = books.first()),
                    {},
                    {},
                    {},
                    {},
                    {},
                    {},
                    { _, _ -> },
                )
            }
        }
        compose.onNodeWithTag("continue_reading").assertIsDisplayed()
        compose.onNodeWithTag("book_list").performTouchInput { swipeUp() }
        compose.onNodeWithTag("book_list").performTouchInput { swipeUp() }
        compose.onNodeWithTag("continue_reading").assertDoesNotExist()
        compose.onNodeWithTag("floating_continue").assertIsDisplayed()
        compose.onNodeWithTag("book_list").performTouchInput { swipeDown(endY = centerY, durationMillis = 800) }
        compose.onNodeWithTag("continue_reading").assertIsDisplayed()
        compose.onNodeWithContentDescription("scroll to top").performClick()
        compose.onNodeWithTag("book_1").assertIsDisplayed()
        compose.onNodeWithContentDescription("scroll to top").assertDoesNotExist()
    }
}
