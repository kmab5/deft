package com.kmab.deft.library.scanner

import android.app.Notification
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import androidx.test.core.app.ApplicationProvider
import androidx.work.WorkInfo
import androidx.work.workDataOf
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.util.UUID

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ScanFeedbackTest {
    @Test fun oneFileSummaryUsesSingularNouns() {
        val summary = report().copy(added = 1, processedFiles = 1).summary()
        assertEquals("scan complete · 1 file checked · 1 book added", summary)
    }

    @Test fun observerIdentifiesInitializationWorkAndFreshTerminalResults() {
        assertFalse(ScanStatus().initialized)
        assertTrue(scanStatus(emptyList(), null).initialized)
        val id = UUID.randomUUID()
        val current =
            WorkInfo(
                id = id,
                state = WorkInfo.State.SUCCEEDED,
                tags = emptySet(),
                outputData = report().copy(reportId = id.toString()).toWorkData(completedAtMillis = 123456L),
            )
        val status = scanStatus(listOf(current), null)
        assertTrue(status.initialized)
        assertTrue(status.isFinished)
        assertEquals(id.toString(), status.workId)
        assertEquals(123456L, status.completedAtMillis)
        assertEquals(id.toString(), status.report!!.reportId)
    }

    @Test fun failedOrCancelledScanDoesNotPresentAnOldSuccessfulReport() {
        for (state in listOf(WorkInfo.State.FAILED, WorkInfo.State.CANCELLED)) {
            val work =
                WorkInfo(
                    id = UUID.randomUUID(),
                    state = state,
                    tags = emptySet(),
                    outputData = workDataOf("failure" to "Check folder access"),
                )
            val status = scanStatus(listOf(work), report())
            assertNull(status.report)
            assertFalse(status.isScanning)
            assertTrue(status.message != null)
        }
    }

    @Test fun cancelledForegroundPromotionNeverStartsFallbackWork() =
        runBlocking {
            val cancelled = CancellationException("user cancelled")
            try {
                promoteScan(true) { throw cancelled }
                error("Cancellation must propagate")
            } catch (actual: CancellationException) {
                assertTrue(actual === cancelled)
            }
        }

    @Test fun progressNotificationHasDataSyncTypeAndUserCancellationAction() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val cancel = PendingIntent.getBroadcast(context, 0, Intent("cancel-test"), PendingIntent.FLAG_IMMUTABLE)
        val info = ScanNotification(context, cancel).foregroundInfo(501)
        assertEquals(ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC, info.foregroundServiceType)
        assertEquals(
            "501 files checked · you can leave Deft",
            info.notification.extras.getString(Notification.EXTRA_TEXT),
        )
        assertEquals(
            cancel,
            info.notification.actions
                .single()
                .actionIntent,
        )
        assertTrue(info.notification.flags and Notification.FLAG_ONGOING_EVENT != 0)
    }

    @Test fun userScanPromotesButAutomaticScanDoesNotStartForegroundService() =
        runBlocking {
            var promotions = 0
            assertTrue(promoteScan(true) { promotions++ })
            assertFalse(promoteScan(false) { promotions++ })
            assertEquals(1, promotions)
        }

    @Test fun foregroundStartRestrictionsFallBackToResumableBackgroundWork() =
        runBlocking {
            assertFalse(promoteScan(true) { throw IllegalStateException("App moved to background") })
            assertFalse(promoteScan(true) { throw SecurityException("Foreground service not allowed") })
        }

    @Test fun foregroundScanDoesNotRepeatFourMinutePrefixButBackgroundRetriesAreFinite() {
        assertFalse(shouldYieldScan(foreground = true, elapsedMillis = 5 * 60 * 1000L))
        assertTrue(shouldYieldScan(foreground = false, elapsedMillis = 5 * 60 * 1000L))
        assertTrue(canContinueScan(0))
        assertFalse(canContinueScan(5))
    }

    private fun report() = ScanReport(500, 0, 0, 0, 0, 2, emptyList(), processedFiles = 500, metadataFallbacks = 2)

    @Test fun continuationRetainsImportedTotalsAfterRecreationWithoutAddingRepeatedProcessedCounts() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val preferences = context.getSharedPreferences("continuation-test", Context.MODE_PRIVATE)
        preferences.edit().clear().commit()
        ScanContinuationStore(preferences).save("same-work", report())
        val resumed =
            ScanContinuationStore(
                preferences,
            ).combine("same-work", report().copy(added = 1000, processedFiles = 1500))
        assertEquals(1500, resumed.added)
        assertEquals(1500, resumed.processedFiles)
        assertEquals(4, resumed.errors)
        assertEquals(4, resumed.metadataFallbacks)
        assertEquals(500, ScanContinuationStore(preferences).combine("new-work", report()).added)
    }

    @Test fun activeScanShowsCurrentProgressAndHidesOldCompletedReport() {
        val work =
            WorkInfo(
                id = UUID.randomUUID(),
                state = WorkInfo.State.RUNNING,
                tags = emptySet(),
                progress = workDataOf("processedFiles" to 700, "skippedFiles" to 12),
            )
        val status = scanStatus(listOf(work), report())
        assertTrue(status.isScanning)
        assertNull(status.report)
        assertEquals(700, status.progress!!.processedFiles)
        assertTrue(status.message!!.contains("700 files checked"))
    }

    @Test fun completedPartialReportKeepsActionableDetails() {
        val report =
            report().copy(
                failedFiles = 1,
                incompleteRoots = listOf("root"),
                incompleteRootCount = 1,
                errorDetails = listOf("Book.epub: cannot read"),
            )
        val work =
            WorkInfo(
                id = UUID.randomUUID(),
                state = WorkInfo.State.SUCCEEDED,
                tags = emptySet(),
                outputData = report.toWorkData(),
            )
        val status = scanStatus(listOf(work), null)
        assertFalse(status.isScanning)
        assertEquals(report, status.report)
        assertTrue(status.message!!.startsWith("scan incomplete"))
        assertTrue(status.report!!.details().any { "Book.epub" in it })
    }

    @Test fun diagnosticsWithLongUnicodePathsFitWorkManagerDataLimit() {
        val longText = "界".repeat(2000)
        val data =
            report()
                .copy(
                    unavailableRoots = List(100) { longText },
                    incompleteRoots = List(100) { longText },
                    errorDetails = List(100) { longText },
                ).toWorkData()
        assertEquals(8, data.getStringArray("unavailableRoots")!!.size)
        assertEquals(4, data.getStringArray("errorDetails")!!.size)
        assertEquals(100, data.getInt("unavailableRootCount", 0))
    }
}
