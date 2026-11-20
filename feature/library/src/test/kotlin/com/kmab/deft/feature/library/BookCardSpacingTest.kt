package com.kmab.deft.feature.library

import android.content.Context
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.width
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.hasParent
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.unit.dp
import androidx.test.core.app.ApplicationProvider
import coil3.ImageLoader
import com.kmab.deft.core.database.model.BookListItem
import com.kmab.deft.core.designsystem.DeftTheme
import com.kmab.deft.core.model.BookFormat
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.time.Instant

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36], qualifiers = "w393dp-h852dp-mdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class BookCardSpacingTest {
    @get:Rule val compose = createComposeRule()

    @Test fun singleLineTitleUsesItsOwnHeightBesideAWrappedTitle() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val loader = ImageLoader.Builder(context).build()
        val short = book(1L, "Piranesi", "Susanna Clarke")
        val wrapped = book(2L, "The Long Journey Through the Stars", "Another Writer")
        try {
            compose.setContent {
                DeftTheme {
                    Row {
                        BookCard(short, loader, Modifier.width(170.dp))
                        BookCard(wrapped, loader, Modifier.width(170.dp))
                    }
                }
            }
            val title = metadataBounds(short.id, short.title)
            val longTitle = metadataBounds(wrapped.id, wrapped.title)
            val author = metadataBounds(short.id, short.authorsDisplay)
            assertTrue("One-line titles do not reserve a blank second line", title.height < longTitle.height)
            assertEquals(8f, author.top - title.bottom, 1f)
            val first = compose.onNodeWithTag("book_1").fetchSemanticsNode().boundsInRoot
            val second = compose.onNodeWithTag("book_2").fetchSemanticsNode().boundsInRoot
            assertTrue("Mixed title lengths keep naturally sized cards", first.height < second.height)
        } finally {
            loader.shutdown()
        }
    }

    private fun metadataBounds(
        bookId: Long,
        text: String,
    ) = compose
        .onNode(
            hasText(text) and hasParent(hasTestTag("book_$bookId")),
            useUnmergedTree = true,
        ).fetchSemanticsNode()
        .boundsInRoot

    private fun book(
        id: Long,
        title: String,
        author: String,
    ) = BookListItem(id, title, title, author, BookFormat.EPUB, null, Instant.EPOCH, false, false)
}
