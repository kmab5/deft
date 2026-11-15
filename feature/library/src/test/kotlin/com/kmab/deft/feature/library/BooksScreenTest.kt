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
        compose.onNodeWithContentDescription("switch to list view").performClick()
        compose.onNodeWithTag("book_list").assertIsDisplayed()
        compose.onNodeWithTag("book_grid").assertDoesNotExist()
    }

    @Test fun oneSheetAppliesFormatsSortAndMissingLive() {
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
        compose.onNodeWithText("all books").performClick()
        compose.onNodeWithText("txt").performClick()
        compose.onNodeWithText("author").performClick()
        compose
            .onNodeWithText("show missing")
            .performScrollTo()
            .assertIsDisplayed()
            .performClick()
        assertFalse(BookFormat.TXT in state.value.formatFilter)
        assertEquals(BookSort.AUTHOR, state.value.sort)
        assertEquals(true, state.value.showMissing)
        compose.onNodeWithText("done").performClick()
        compose.onNodeWithText("author ▾").assertIsDisplayed()
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
