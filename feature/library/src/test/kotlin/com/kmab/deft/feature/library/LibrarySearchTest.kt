package com.kmab.deft.feature.library

import com.kmab.deft.core.database.model.BookListItem
import com.kmab.deft.core.model.BookFormat
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Test
import java.time.Instant

class LibrarySearchTest {
    private val books =
        listOf(
            book(8, "The Left Hand of Darkness", "Ursula K. Le Guin"),
            book(3, "A Wizard of Earthsea", "Ursula K. Le Guin"),
            book(2, "Dune", "Frank Herbert"),
        )

    @Test fun whitespaceQueryPreservesTheSelectedSortOrder() {
        assertSame(books, books.searchBooks(" \t "))
        assertEquals(listOf(8L, 3L), books.searchBooks("guin").map { it.id })
    }

    @Test fun wordsMatchAcrossTitleAndAuthorIgnoringCase() {
        assertEquals(listOf(3L), books.searchBooks("  EARTHSEA  ursula ").map { it.id })
        assertEquals(emptyList<Long>(), books.searchBooks("earthsea herbert").map { it.id })
    }

    @Test fun advancedOptionsApplyToBothTitlesAndAuthors() {
        val options =
            com.kmab.deft.core.model
                .SearchOptions(regex = true)
        assertEquals(listOf(8L, 3L), books.searchBooks("Guin|Earthsea", options).map { it.id })
        assertEquals(emptyList<Long>(), books.searchBooks("guin", options.copy(matchCase = true)).map { it.id })
        assertEquals(emptyList<Long>(), books.searchBooks("Dark", options.copy(wholeWord = true)).map { it.id })
        assertEquals(listOf(8L), books.searchBooks("Darkness", options.copy(wholeWord = true)).map { it.id })
    }

    private fun book(
        id: Long,
        title: String,
        author: String,
    ) = BookListItem(id, title, title, author, BookFormat.EPUB, null, Instant.EPOCH, false, false)
}
