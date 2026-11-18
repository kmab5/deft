package com.kmab.deft.library.scanner

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.cancel
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File
import java.io.IOException
import java.util.UUID

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ScanReportStoreTest {
    private val context = ApplicationProvider.getApplicationContext<Context>()

    private fun entry(
        index: Int,
        outcome: ScanDetailOutcome = ScanDetailOutcome.ADDED,
    ) = ScanDetailEntry("file$index", "Book $index.epub", "Books/Book $index.epub", outcome, "New book")

    @Test fun observationUpdatesWhenAnotherInstanceFinishesDelayedReportWrite() =
        runBlocking {
            val id = UUID.randomUUID().toString()
            val initiallyLoaded = CompletableDeferred<Unit>()
            val emissions = mutableListOf<ScanDetails?>()
            val observer =
                launch {
                    ScanReportStore(context).observe(id).take(2).collect {
                        emissions += it
                        initiallyLoaded.complete(Unit)
                    }
                }
            withTimeout(5000) { initiallyLoaded.await() }
            assertNull(emissions.single())
            ScanReportStore(context).append(id, ScanDetails(listOf(entry(0))))
            withTimeout(5000) { observer.join() }
            assertEquals(
                "Book 0.epub",
                emissions
                    .last()!!
                    .entries
                    .single()
                    .name,
            )
        }

    @Test fun namedImportsSurviveContinuationAndStoreRecreation() =
        runBlocking {
            val id = UUID.randomUUID().toString()
            val store = ScanReportStore(context)
            store.append(id, ScanDetails((0 until 500).map { entry(it) }))
            val secondPass =
                (0 until 500).map { entry(it, ScanDetailOutcome.UNCHANGED) } +
                    (500 until 1500).map { entry(it) }
            store.append(id, ScanDetails(secondPass))
            val saved = ScanReportStore(context).load(id)!!
            assertEquals(1500, saved.entries.size)
            assertTrue(saved.entries.all { it.outcome == ScanDetailOutcome.ADDED })
            assertEquals("Book 1499.epub", saved.entries.last().name)
            assertEquals(0, saved.omittedCount)
        }

    @Test fun recordLimitHasExplicitOmissionCountAndLatestReportsAreRetained() =
        runBlocking {
            val store = ScanReportStore(context)
            val ids = List(4) { UUID.randomUUID().toString() }
            store.append(ids.first(), ScanDetails((0 until 10003).map { entry(it) }))
            assertEquals(10000, store.load(ids.first())!!.entries.size)
            assertEquals(3, store.load(ids.first())!!.omittedCount)
            ids.drop(1).forEach { store.append(it, ScanDetails(listOf(entry(0)))) }
            assertNull(store.load(ids.first()))
            assertTrue(ids.drop(1).all { store.load(it) != null })
        }

    @Test fun invalidIdsCannotReadOutsideReportDirectory() =
        runBlocking {
            val store = ScanReportStore(context)
            assertNull(store.load("../../keystore.properties"))
            assertNull(store.load("1-1-1-1-1"))
        }

    @Test fun unicodePayloadIsByteBoundedAndReportsEveryOmittedRecord() =
        runBlocking {
            val id = UUID.randomUUID().toString()
            val longText = "界".repeat(500)
            val store = ScanReportStore(context)
            store.append(
                id,
                ScanDetails(
                    (0 until 10000).map {
                        entry(it).copy(name = longText, path = longText, reason = longText)
                    },
                ),
            )
            val saved = store.load(id)!!
            assertTrue(saved.omittedCount > 0)
            assertEquals(10000, saved.entries.size + saved.omittedCount)
            assertTrue(File(context.filesDir, "scan-reports/$id.json").length() <= 8 * 1024 * 1024)
            assertTrue(saved.entries.all { it.name.length <= 160 && it.path.length <= 240 && it.reason.length <= 200 })
        }

    @Test fun failedAtomicReplacementRemovesItsTemporaryFile() =
        runBlocking {
            val id = UUID.randomUUID().toString()
            val target = File(context.filesDir, "scan-reports/$id.json")
            target.mkdirs()
            try {
                try {
                    ScanReportStore(context).append(id, ScanDetails(listOf(entry(0))))
                    error("Expected replacement failure")
                } catch (_: IOException) {
                }
                assertTrue(target.isDirectory)
                assertTrue(!File(target.parentFile, "$id.tmp").exists())
            } finally {
                target.delete()
            }
        }

    @Test fun cancelledWriteLeavesThePreviousCompleteReportReadable() =
        runBlocking {
            val id = UUID.randomUUID().toString()
            val store = ScanReportStore(context)
            store.append(id, ScanDetails(listOf(entry(0))))
            val job =
                launch(start = CoroutineStart.UNDISPATCHED) {
                    currentCoroutineContext().cancel()
                    store.append(id, ScanDetails(listOf(entry(1))))
                }
            job.join()
            assertEquals(listOf("Book 0.epub"), store.load(id)!!.entries.map { it.name })
            assertTrue(!File(context.filesDir, "scan-reports/$id.tmp").exists())
        }
}
