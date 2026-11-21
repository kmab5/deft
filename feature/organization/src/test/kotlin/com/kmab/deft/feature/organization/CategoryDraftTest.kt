package com.kmab.deft.feature.organization

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import org.junit.Assert.assertEquals
import org.junit.Test

class CategoryDraftTest {
    @Test fun cancellationAtCommitDispatcherReturnKeepsTheReferencedCover() =
        runBlocking {
            val commitReachedDisk = CompletableDeferred<Unit>()
            val allowCommitReturn = CompletableDeferred<Unit>()
            val files = mutableSetOf("old.jpg")
            var reference: String? = "old.jpg"
            val save =
                launch {
                    persistCategoryCover(
                        previous = reference,
                        removePrevious = false,
                        importCover = { "new.jpg".also { files.add(it) } },
                        removeCover = { if (it != null) files.remove(it) },
                    ) { path ->
                        withContext(Dispatchers.IO) {
                            reference = path
                            commitReachedDisk.complete(Unit)
                            allowCommitReturn.await()
                        }
                    }
                }
            commitReachedDisk.await()
            save.cancel()
            allowCommitReturn.complete(Unit)
            save.join()
            assertEquals("new.jpg", reference)
            assertEquals(setOf("new.jpg"), files)
        }
}
