package com.kmab.deft

import com.kmab.deft.library.scanner.ScanStatus

private const val FRESH_COMPLETION_MILLIS = 6_000L

internal data class ScanNoticeMemory(
    val initialized: Boolean = false,
    val handledId: String? = null,
    val activeId: String? = null,
) {
    fun endForegroundSession(): ScanNoticeMemory = copy(activeId = null)
}

internal data class ScanNoticeDecision(
    val memory: ScanNoticeMemory,
    val show: Boolean = false,
)

/** Historical work establishes a baseline; only fresh subsequent completions deserve an interruption. */
internal fun scanNoticeDecision(
    memory: ScanNoticeMemory,
    scan: ScanStatus,
    now: Long,
): ScanNoticeDecision {
    if (!scan.initialized) return ScanNoticeDecision(memory)
    val active = if (scan.isScanning) scan.workId else memory.activeId
    return when {
        !memory.initialized ->
            ScanNoticeDecision(ScanNoticeMemory(true, if (scan.isFinished) scan.workId else null, active))
        !scan.isFinished || scan.workId == null || scan.workId == memory.handledId ->
            ScanNoticeDecision(memory.copy(activeId = active))
        else -> {
            val fresh =
                scan.completedAtMillis?.let { now - it in 0..FRESH_COMPLETION_MILLIS }
                    ?: (memory.activeId == scan.workId)
            ScanNoticeDecision(memory.copy(handledId = scan.workId, activeId = null), fresh)
        }
    }
}
