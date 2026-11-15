package com.kmab.deft.library.scanner

import androidx.work.WorkInfo
import androidx.work.WorkManager
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.runningFold
import javax.inject.Inject
import javax.inject.Singleton

/** Shared presentation of the actual unique scan, including retry and replacement states. */
data class ScanStatus(
    val isScanning: Boolean = false,
    val report: ScanReport? = null,
    val message: String? = null,
)

@Singleton
class ScanStatusObserver
    @Inject
    constructor(
        workManager: WorkManager,
    ) {
        val status: Flow<ScanStatus> =
            workManager
                .getWorkInfosForUniqueWorkFlow(ScanScheduler.WORK_NAME)
                .runningFold(ScanStatus()) { previous, work -> scanStatus(work, previous.report) }
    }

internal fun scanStatus(
    work: List<WorkInfo>,
    previous: ScanReport?,
): ScanStatus {
    val current = work.firstOrNull { !it.state.isFinished } ?: work.lastOrNull() ?: return ScanStatus(report = previous)
    val report = completedReport(current) ?: previous
    return ScanStatus(!current.state.isFinished, report, scanMessage(current, report))
}

private fun completedReport(current: WorkInfo): ScanReport? {
    return if (current.state == WorkInfo.State.SUCCEEDED && "added" in current.outputData.keyValueMap) {
            current.outputData.let { data ->
                ScanReport(
                    data.getInt("added", 0),
                    data.getInt("updated", 0),
                    data.getInt("unchanged", 0),
                    data.getInt("reattached", 0),
                    data.getInt("markedMissing", 0),
                    data.getInt("errors", 0),
                    data.getStringArray("unavailableRoots")?.toList().orEmpty(),
                )
            }
        } else {
            null
        }
}

private fun scanMessage(current: WorkInfo, report: ScanReport?): String? =
        when (current.state) {
            WorkInfo.State.RUNNING -> "Scanning folders."
            WorkInfo.State.ENQUEUED ->
                if (current.runAttemptCount >
                    0
                ) {
                    "Scan interrupted. Retrying."
                } else {
                    "Scan queued."
                }
            WorkInfo.State.BLOCKED -> "Scan is waiting."
            WorkInfo.State.FAILED -> "Scan failed. Try rescan."
            WorkInfo.State.CANCELLED -> "Scan cancelled. Try rescan."
            WorkInfo.State.SUCCEEDED ->
                report?.unavailableRoots?.size?.takeIf { it > 0 }?.let { count ->
                    "$count ${if (count == 1) "folder" else "folders"} unavailable. Check library settings."
                }
        }
