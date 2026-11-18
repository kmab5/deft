package com.kmab.deft.feature.settings

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.kmab.deft.core.database.model.BookListItem
import com.kmab.deft.core.designsystem.DeftTheme
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
class SettingsNavigationContentTest {
    @get:Rule val compose = createComposeRule()

    @Test fun chooserOpensEachRealSectionAndReturns() {
        var selected = ""
        compose.setContent {
            DeftTheme {
                SettingsHomeScreen({ selected = "back" }, { selected = "library" }, { selected = "reader" })
            }
        }
        compose.onNodeWithText("library").performClick()
        assertEquals("library", selected)
        compose.onNodeWithText("reader").performClick()
        assertEquals("reader", selected)
        compose.onNodeWithContentDescription("back").performClick()
        assertEquals("back", selected)
    }

    @Test fun ignoredBookCanBeInspectedOrRestoredIndependently() {
        var opened = 0L
        var restored = 0L
        val book =
            BookListItem(
                7L,
                "Earthsea",
                "earthsea",
                "Ursula K. Le Guin",
                BookFormat.EPUB,
                null,
                Instant.EPOCH,
                missing = true,
                drmProtected = false,
            )
        compose.setContent {
            DeftTheme { IgnoredBooksContent(listOf(book), {}, { opened = it }, { restored = it }) }
        }
        compose.onNodeWithText("file missing").assertExists()
        compose.onNodeWithText("Earthsea").performClick()
        assertEquals(7L, opened)
        assertEquals(0L, restored)
        compose.onNodeWithContentDescription("un-ignore Earthsea").performClick()
        assertEquals(7L, restored)
    }
}
