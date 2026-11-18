package com.kmab.deft.feature.library

import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.test.assertHasNoClickAction
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
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
        compose.onNodeWithText("A book").assertHasNoClickAction()
        compose.onNodeWithText("display").performClick()
        compose.onNodeWithText("list").performClick()
        compose.onNodeWithText("done").performClick()
        compose.onNodeWithTag("book_list").assertIsDisplayed()
        compose.onNodeWithTag("book_grid").assertDoesNotExist()
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
        compose.onNodeWithText("filters").performClick()
        compose.onNodeWithText("txt").performClick()
        compose
            .onNodeWithContentDescription("show missing")
            .performScrollTo()
            .assertIsDisplayed()
            .performClick()
        assertFalse(BookFormat.TXT in state.value.formatFilter)
        assertEquals(true, state.value.showMissing)
        compose.onNodeWithText("done").performClick()
        compose.onNodeWithText("sort").performClick()
        compose.onNodeWithText("author").performClick()
        assertEquals(BookSort.AUTHOR, state.value.sort)
        compose.onNodeWithText("done").performClick()
        compose.onNodeWithText("author · a to z").assertIsDisplayed()
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
        compose.onNodeWithText("display").performClick()
        compose.onNodeWithText("small").performClick()
        compose.onNodeWithText("list").performClick()
        compose.onNodeWithText("large").performClick()
        assertEquals(BookDisplaySize.SMALL, state.value.gridSize)
        assertEquals(BookDisplaySize.LARGE, state.value.listSize)
        compose.onNodeWithText("done").performClick()
        compose.onNodeWithText("sort").performClick()
        compose.onNodeWithText("last read").performClick()
        compose.onNodeWithText("oldest first").performClick()
        compose.onNodeWithText("Books with no reading history stay at the end.").assertIsDisplayed()
        compose.onNodeWithText("done").performClick()
        assertEquals(BookSort.LAST_READ, state.value.sort)
        assertEquals(true, state.value.reverseSort)
        compose.onNodeWithText("last read · oldest first").assertIsDisplayed()
    }

    @Test fun interruptedScanMessageTakesPrecedenceOverPreviousSuccessfulReport() {
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
        compose.onNodeWithText(message).assertIsDisplayed()
        compose.onNodeWithText(previous.summary()).assertDoesNotExist()
    }

    @Test fun unavailableFolderFeedbackLinksToLibrarySettings() {
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
                    { opens++ },
                    {},
                    {},
                    {},
                    {},
                    { _, _ -> },
                )
            }
        }
        compose.onNodeWithText("1 folder unavailable. Check library settings.").assertIsDisplayed()
        compose.onNodeWithText("library settings").performClick()
        assertEquals(1, opens)
        compose.onNodeWithText("private-uri").assertDoesNotExist()
    }
}
