package com.kmab.deft.library.organization

import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class OrganizationObservationTest : OrganizationFixture() {
    @Test fun observedSnapshotsRefreshAfterAtomicMembershipAndAuthorEdits() =
        runBlocking {
            val a = book("A")
            val b = book("B")
            db.authorDao().setBookAuthors(a, listOf("Original Author"))
            val author = db.authorDao().getOrCreate("Original Author")
            val snapshots = Channel<OrganizationSnapshot>(Channel.UNLIMITED)
            val observer = launch { repo.observe().collect { snapshots.send(it) } }
            try {
                assertEquals(2, withTimeout(5000) { snapshots.receive() }.books.size)
                val category = repo.saveCategory(name = "Observed")
                repo.addBooks(category, setOf(a, b))
                val added =
                    withTimeout(5000) {
                        var value = snapshots.receive()
                        while (value.categories
                                .singleOrNull()
                                ?.books
                                ?.size != 2
                        ) {
                            value = snapshots.receive()
                        }
                        value
                    }
                assertEquals(
                    setOf(a, b),
                    added.categories
                        .single()
                        .books
                        .map { it.book.book.id }
                        .toSet(),
                )
                repo.renameAuthor(author, "Renamed Author")
                val renamed =
                    withTimeout(5000) {
                        var value = snapshots.receive()
                        while (value.authors
                                .singleOrNull()
                                ?.author
                                ?.name != "Renamed Author"
                        ) {
                            value = snapshots.receive()
                        }
                        value
                    }
                assertEquals(
                    "Renamed Author",
                    renamed.books
                        .single { it.book.id == a }
                        .book.authorsDisplay,
                )
            } finally {
                observer.cancelAndJoin()
                snapshots.close()
            }
        }
}
