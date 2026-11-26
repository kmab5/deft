package com.kmab.deft.feature.notes

import android.content.Context
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.StateRestorationTester
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextReplacement
import androidx.lifecycle.SavedStateHandle
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.kmab.deft.core.database.AppDatabase
import com.kmab.deft.core.database.entity.BookEntity
import com.kmab.deft.core.database.entity.BookFileEntity
import com.kmab.deft.core.database.entity.BookmarkEntity
import com.kmab.deft.core.designsystem.DeftNoticeHost
import com.kmab.deft.core.designsystem.DeftNoticeState
import com.kmab.deft.core.designsystem.DeftTheme
import com.kmab.deft.core.designsystem.LocalDeftNoticeState
import com.kmab.deft.core.model.BookFormat
import com.kmab.deft.core.model.HighlightColor
import com.kmab.deft.library.notes.HighlightDraft
import com.kmab.deft.library.notes.NoteFilter
import com.kmab.deft.library.notes.NotesRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.time.Instant

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36], qualifiers = "w393dp-h852dp-mdpi")
class NotesScreenTest {
    @get:Rule val compose = createComposeRule()
    private val context = ApplicationProvider.getApplicationContext<Context>()
    private val db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).build()
    private val repository = NotesRepository(db)
    private val model = NotesViewModel(repository, SavedStateHandle(), context)

    @After fun close() = db.close()

    @Test fun intersectingFiltersAndLiteralSearchSurviveRestorationAndResetTogether() {
        seed()
        val restoration = StateRestorationTester(compose)
        restoration.setContent { DeftTheme { NotesScreen(model, { _, _ -> }) } }
        awaitText("100%_ passage")
        compose.onNodeWithText("filters").performClick()
        chooseBook("First book")
        compose.onNodeWithText("purple").performScrollTo().performClick()
        compose.onNodeWithText("notes").performScrollTo().performClick()
        compose.onNodeWithText("done").performClick()
        compose.onNode(hasSetTextAction()).performTextReplacement("%_")
        awaitText("100%_ passage")
        compose.onNodeWithText("Other passage").assertDoesNotExist()
        compose.onNodeWithText("Bookmark label").assertDoesNotExist()
        restoration.emulateSavedInstanceStateRestore()
        awaitText("100%_ passage")
        compose.onNodeWithText("%_").assertExists()
        compose.onNodeWithText("clear filters").performClick()
        compose.onNodeWithText("%_").assertExists()
        compose.onNodeWithContentDescription("clear search").performClick()
        awaitText("Other passage")
        compose.onNodeWithText("Bookmark label").assertExists()
        compose.onNodeWithText("clear filters").assertDoesNotExist()
    }

    @Test fun missingBookEditsPersistAndAvailableBookOpensTheExactLocator() {
        val (first, second) = seed()
        val opened = mutableListOf<Pair<Long, String>>()
        val notices = DeftNoticeState()
        compose.setContent {
            DeftTheme {
                CompositionLocalProvider(LocalDeftNoticeState provides notices) {
                    Box {
                        NotesScreen(model, { book, locator -> opened += book to locator })
                        DeftNoticeHost(notices)
                    }
                }
            }
        }
        awaitText("100%_ passage")
        compose.onNodeWithText("100%_ passage").performClick()
        awaitText("Deft can't find this file. You can still edit or share its notes.")
        assertEquals(emptyList<Pair<Long, String>>(), opened)
        compose.onNodeWithContentDescription("dismiss notice").performClick()
        compose.onNodeWithText("Other passage").performClick()
        assertEquals(listOf(second to SECOND_LOCATOR), opened)

        compose.onNodeWithText("filters").performClick()
        chooseBook("First book")
        compose.onNodeWithText("done").performClick()
        awaitText("100%_ passage")
        compose.onNodeWithContentDescription("edit annotation").performClick()
        compose.onNode(hasSetTextAction() and hasText("note")).performTextReplacement("Revised note")
        compose.onNodeWithText("save").performClick()
        awaitText("Revised note")
        assertEquals(
            "Revised note",
            runBlocking {
                repository
                    .observe(
                        NoteFilter(bookId = first, type = com.kmab.deft.library.notes.NoteType.NOTE),
                    ).first()
                    .single()
                    .note
            },
        )

        compose.onNodeWithContentDescription("rename bookmark").performClick()
        compose.onNode(hasSetTextAction() and hasText("bookmark name")).performTextReplacement("Read this again")
        compose.onNodeWithText("save").performClick()
        awaitText("Read this again")
        assertEquals(
            "Read this again",
            runBlocking {
                db
                    .bookmarkDao()
                    .observeForBook(first)
                    .first()
                    .single()
                    .label
            },
        )
    }

    @Test fun bookmarksClearTheIncompatibleColorFilter() {
        seed()
        compose.setContent { DeftTheme { NotesScreen(model, { _, _ -> }) } }
        awaitText("100%_ passage")
        compose.onNodeWithText("filters").performClick()
        compose.onNodeWithText("green").performScrollTo().performClick()
        compose.onNodeWithText("bookmarks").performScrollTo().performClick()
        compose.onNodeWithText("green").assertDoesNotExist()
        compose.onNodeWithText("done").performClick()
        awaitText("Bookmark label")
        compose.onNodeWithText("filters · 1").assertExists()
        compose.onNodeWithText("bookmarks").assertExists()
    }

    @Test fun unavailableMarkdownReceiverReportsAnErrorAndDoesNotStartAnExport() {
        val (bookId, _) = seed()
        val notices = DeftNoticeState()
        compose.setContent {
            DeftTheme {
                CompositionLocalProvider(LocalDeftNoticeState provides notices) {
                    Box {
                        NotesScreen(model, { _, _ -> }, bookId)
                        DeftNoticeHost(notices)
                    }
                }
            }
        }
        awaitText("share Markdown")
        compose.onNodeWithText("share Markdown").performClick()
        awaitText("No app can share these notes.")
        assertNull(model.share.value)
    }

    private fun chooseBook(title: String) {
        compose.onNodeWithText("all books").performScrollTo().performClick()
        val search = title.substringBefore(' ')
        compose.onNode(hasSetTextAction() and hasText("search books")).performTextReplacement(search)
        compose.onNodeWithText(title).performClick()
    }

    private fun awaitText(text: String) {
        compose.waitUntil(5_000) { compose.onAllNodesWithText(text).fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithText(text).assertIsDisplayed()
    }

    private fun seed(): Pair<Long, Long> =
        runBlocking {
            val first =
                db.bookDao().insert(
                    BookEntity(
                        contentHash = "first",
                        format = BookFormat.EPUB,
                        title = "First book",
                        sortTitle = "First book",
                        addedAt = Instant.EPOCH,
                    ),
                )
            val second =
                db.bookDao().insert(
                    BookEntity(
                        contentHash = "second",
                        format = BookFormat.EPUB,
                        title = "Second book",
                        sortTitle = "Second book",
                        addedAt = Instant.EPOCH,
                    ),
                )
            db.bookFileDao().insert(
                BookFileEntity(
                    bookId = second,
                    rootUri = "root",
                    uri = "content://book/second",
                    displayPath = "Second.epub",
                    size = 25,
                    lastModified = 0,
                    missing = false,
                    lastSeenAt = Instant.EPOCH,
                ),
            )
            repository.saveHighlight(
                HighlightDraft(first, FIRST_LOCATOR, "100%_ passage", HighlightColor.PURPLE, "Original note"),
            )
            repository.saveHighlight(
                HighlightDraft(second, SECOND_LOCATOR, "Other passage", HighlightColor.GREEN, "Different note"),
            )
            db.bookmarkDao().insert(
                BookmarkEntity(
                    bookId = first,
                    locator = BOOKMARK_LOCATOR,
                    totalProgression = 0.75,
                    label = "Bookmark label",
                    createdAt = Instant.EPOCH,
                ),
            )
            first to second
        }

    private companion object {
        const val FIRST_LOCATOR = """{"href":"chapter.xhtml","title":"First chapter"}"""
        const val SECOND_LOCATOR = """{"href":"chapter.xhtml","title":"Second chapter"}"""
        const val BOOKMARK_LOCATOR = """{"href":"other.xhtml","title":"Last chapter"}"""
    }
}
