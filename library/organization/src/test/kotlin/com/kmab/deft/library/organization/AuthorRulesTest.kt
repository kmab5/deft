package com.kmab.deft.library.organization

import com.kmab.deft.core.model.AuthorNames
import com.kmab.deft.core.model.ReadThroughOutcome
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class AuthorRulesTest : OrganizationFixture() {
    @Test fun bookCreditsKeepTheirPositionWhenAuthorListSortsAlphabetically() =
        runBlocking {
            val bookId = book("Book")
            db.authorDao().setBookAuthors(bookId, listOf("Zora Writer", "Ada Writer"))
            val snapshot = repo.get()
            assertEquals(listOf("Ada Writer", "Zora Writer"), snapshot.authors.map { it.author.name })
            val authors = snapshot.authors.associateBy { it.author.id }
            val book = snapshot.books.single()
            assertEquals(listOf("Zora Writer", "Ada Writer"), book.authorIds.map { authors.getValue(it).author.name })
        }

    @Test fun renamedAuthorsRetainAliasesForFutureScans() =
        runBlocking {
            val book = book("Book")
            db.authorDao().setBookAuthors(book, listOf("Le Guin, Ursula"))
            val author = db.authorDao().getOrCreate("Ursula Le Guin")
            repo.renameAuthor(author, "Ursula K. Le Guin")
            assertEquals(author, db.authorDao().getOrCreate("Le Guin, Ursula"))
            assertEquals(
                "Ursula K. Le Guin",
                repo
                    .get()
                    .authors
                    .single()
                    .author.name,
            )
            val other = db.authorDao().getOrCreate("Other Writer")
            invalid { repo.renameAuthor(other, "Ursula Le Guin") }
            invalid { repo.renameAuthor(other, "Ursula K. Le Guin") }
            invalid { repo.renameAuthor(author, " ") }
            invalid { repo.renameAuthor(999, "Nobody") }
        }

    @Test fun mergePreservesEffectiveOverridesAndDeduplicatesLinks() =
        runBlocking {
            val book = book("Book")
            db.authorDao().setBookAuthors(book, listOf("Source Writer", "Target Writer", "Other Writer"))
            val source = db.authorDao().getOrCreate("Source Writer")
            val target = db.authorDao().getOrCreate("Target Writer")
            db.openHelper.writableDatabase.execSQL(
                "UPDATE book_authors SET overridden = 1 WHERE bookId = ? AND authorId = ?",
                arrayOf(book, source),
            )
            repo.renameAuthor(source, "Source Renamed")
            repo.mergeAuthors(source, target)
            assertEquals(listOf("Target Writer"), db.bookDao().getAuthorNames(book))
            assertEquals(target, db.authorDao().getOrCreate("Source Writer"))
            assertEquals(target, db.authorDao().getOrCreate("Source Renamed"))
            assertEquals(
                target,
                repo
                    .get()
                    .authors
                    .single()
                    .author.id,
            )
            db.authorDao().setBookAuthors(book, listOf("Source Writer", "Other Writer"))
            assertEquals(listOf("Target Writer"), db.bookDao().getAuthorNames(book))
            assertEquals(
                AuthorNames.normalizedKey("Target Writer"),
                repo
                    .get()
                    .authors
                    .single()
                    .author.normalizedName,
            )
            invalid { repo.mergeAuthors(target, target) }
            invalid { repo.mergeAuthors(source, target) }
        }

    @Test fun coAuthorsReceiveFullTimeAndIgnoredBooksDisappearWithoutLosingRows() =
        runBlocking {
            val visible = book("Visible", rating = 4.5)
            val ignored = book("Ignored", rating = 1.0, ignored = true)
            db.authorDao().setBookAuthors(visible, listOf("First Author", "Second Author"))
            db.authorDao().setBookAuthors(ignored, listOf("First Author", "Hidden Author"))
            val firstRead = read(visible, ReadThroughOutcome.FINISHED)
            session(visible, firstRead, 90)
            session(visible, firstRead, 120, browsing = true)
            session(visible, firstRead, 20)
            val hiddenRead = read(ignored, ReadThroughOutcome.FINISHED)
            session(ignored, hiddenRead, 180)
            val authors = repo.get().authors
            assertEquals(2, authors.size)
            authors.forEach {
                assertEquals(90L, it.stats.activeSeconds)
                assertEquals(4.5, it.stats.averageRating!!, 0.0)
                assertEquals(1, it.stats.finished)
                assertTrue(
                    it.books
                        .single()
                        .book.missing,
                )
            }
            db.bookDao().setIgnored(ignored, false)
            assertEquals(3, repo.get().authors.size)
        }
}
