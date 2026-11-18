package com.kmab.deft

import com.kmab.deft.library.scanner.ScanStatus
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ScanNoticePolicyTest {
    private val running = ScanStatus(initialized = true, isScanning = true, workId = "scan")
    private val finished =
        ScanStatus(initialized = true, isFinished = true, workId = "scan", completedAtMillis = 10_000)

    @Test fun historicalResultDoesNotReplayOnLaunch() {
        assertFalse(scanNoticeDecision(ScanNoticeMemory(), finished, 10_000).show)
    }

    @Test fun freshCompletionAppearsOnlyOnce() {
        val started = scanNoticeDecision(ScanNoticeMemory(), running, 9_000).memory
        val completed = scanNoticeDecision(started, finished, 10_000)
        assertTrue(completed.show)
        assertFalse(scanNoticeDecision(completed.memory, finished, 10_001).show)
    }

    @Test fun returningAfterExpiryDoesNotReplayCompletion() {
        val started = scanNoticeDecision(ScanNoticeMemory(), running, 9_000).memory
        assertFalse(scanNoticeDecision(started, finished, 20_000).show)
    }

    @Test fun initialPlaceholderDoesNotTurnHistoricalResultIntoNewEvent() {
        val initial = scanNoticeDecision(ScanNoticeMemory(), ScanStatus(), 10_000).memory
        assertFalse(scanNoticeDecision(initial, finished, 10_000).show)
    }

    @Test fun observedCancellationWithoutTimestampIsReported() {
        val started = scanNoticeDecision(ScanNoticeMemory(), running, 9_000).memory
        assertTrue(scanNoticeDecision(started, finished.copy(completedAtMillis = null), 10_000).show)
        assertFalse(scanNoticeDecision(ScanNoticeMemory(), finished.copy(completedAtMillis = null), 10_000).show)
    }

    @Test fun cancellationDuringBackgroundDoesNotReplayOnReturn() {
        val started = scanNoticeDecision(ScanNoticeMemory(), running, 9_000).memory
        val backgrounded = started.endForegroundSession()
        assertFalse(scanNoticeDecision(backgrounded, finished.copy(completedAtMillis = null), 90_000).show)
    }

    @Test fun returningToAnActiveScanCanStillReportItsLaterCompletion() {
        val started = scanNoticeDecision(ScanNoticeMemory(), running, 9_000).memory
        val resumed = scanNoticeDecision(started.endForegroundSession(), running, 10_000).memory
        assertTrue(scanNoticeDecision(resumed, finished.copy(completedAtMillis = null), 11_000).show)
    }
}
