package com.kmab.deft.feature.organization

import android.database.sqlite.SQLiteException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.IOException

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class CategoryCoverCleanupTest {
    @Test fun emptyPathsSkipRetainedCoverLookup() =
        runBlocking {
            removeUnusedCovers(
                paths = emptyList(),
                retainedPaths = { error("Empty cleanup must not query the database") },
                removeCover = { error("Empty cleanup must not delete a cover") },
            )
        }

    @Test fun lookupFailuresDoNotEscapeOrDeletePotentiallySharedCovers() =
        runBlocking {
            listOf(
                SQLiteException("Unavailable database"),
                IOException("Unavailable storage"),
                SecurityException(),
            ).forEach { failure ->
                removeUnusedCovers(
                    paths = listOf("possibly-shared.jpg"),
                    retainedPaths = { throw failure },
                    removeCover = { error("Retention lookup failed; no covers can be safely removed") },
                )
            }
        }

    @Test fun onlyDistinctUnretainedCoversAreRemoved() =
        runBlocking {
            val removed = mutableListOf<String>()
            removeUnusedCovers(
                paths = listOf("shared.jpg", "unused.jpg", "unused.jpg", "other.jpg"),
                retainedPaths = { setOf("shared.jpg", "unrelated.jpg") },
                removeCover = { removed += it },
            )
            assertEquals(listOf("unused.jpg", "other.jpg"), removed)
        }

    @Test fun optionalDeletionFailureDoesNotPreventOtherUnusedCoverCleanup() =
        runBlocking {
            listOf(
                SQLiteException("Unavailable database"),
                IOException("Unavailable storage"),
                SecurityException(),
            ).forEach { failure ->
                val removed = mutableListOf<String>()
                removeUnusedCovers(
                    paths = listOf("failed.jpg", "unused.jpg"),
                    retainedPaths = { emptySet() },
                    removeCover = {
                        if (it == "failed.jpg") throw failure
                        removed += it
                    },
                )
                assertEquals(listOf("unused.jpg"), removed)
            }
        }

    @Test fun cancellationFromLookupIsPreserved() =
        runBlocking {
            val cancellation = CancellationException("Leave screen")
            val failure =
                runCatching {
                    removeUnusedCovers(
                        paths = listOf("cover.jpg"),
                        retainedPaths = { throw cancellation },
                        removeCover = { error("Cancelled lookup must not delete covers") },
                    )
                }.exceptionOrNull()
            assertSame(cancellation, failure)
        }

    @Test fun cancellationFromRemovalIsPreservedAndStopsCleanup() =
        runBlocking {
            val cancellation = CancellationException("Leave screen")
            val failure =
                runCatching {
                    removeUnusedCovers(
                        paths = listOf("first.jpg", "next.jpg"),
                        retainedPaths = { emptySet() },
                        removeCover = {
                            if (it == "first.jpg") throw cancellation
                            error("Cancellation must stop later deletions")
                        },
                    )
                }.exceptionOrNull()
            assertSame(cancellation, failure)
        }
}
