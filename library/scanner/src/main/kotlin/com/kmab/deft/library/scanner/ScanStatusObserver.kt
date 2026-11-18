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
    val progress: ScanProgress? = null,
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
    val report = completedReport(current)
    val progress =
        if (current.state == WorkInfo.State.RUNNING) {
            ScanProgress(current.progress.getInt("processedFiles", 0), current.progress.getInt("skippedFiles", 0))
        } else {
            null
        }
    return ScanStatus(
        !current.state.isFinished,
        report.takeIf { current.state.isFinished },
        scanMessage(current, report),
        progress,
    )
}

private fun completedReport(current: WorkInfo): ScanReport? =
    if (current.state == WorkInfo.State.SUCCEEDED && "added" in current.outputData.keyValueMap) {
        current.outputData.let { data ->
            ScanReport(
                data.getInt("added", 0),
                data.getInt("updated", 0),
                data.getInt("unchanged", 0),
                data.getInt("reattached", 0),
                data.getInt("markedMissing", 0),
                data.getInt("errors", 0),
                data.getStringArray("unavailableRoots")?.toList().orEmpty(),
                processedFiles = data.getInt("processedFiles", 0),
                skippedFiles = data.getInt("skippedFiles", 0),
                duplicateFiles = data.getInt("duplicateFiles", 0),
                metadataFallbacks = data.getInt("metadataFallbacks", 0),
                failedFiles = data.getInt("failedFiles", 0),
                incompleteRoots = data.getStringArray("incompleteRoots")?.toList().orEmpty(),
                errorDetails = data.getStringArray("errorDetails")?.toList().orEmpty(),
                unavailableRootCount = data.getInt("unavailableRootCount", 0),
                incompleteRootCount = data.getInt("incompleteRootCount", 0),
            )
        }
    } else {
        null
    }

private fun scanMessage(
    current: WorkInfo,
    report: ScanReport?,
): String? =
    when (current.state) {
        WorkInfo.State.RUNNING -> "scanning · ${current.progress.getInt("processedFiles", 0)} files checked"
        WorkInfo.State.ENQUEUED ->
            if (current.runAttemptCount >
                0
            ) {
                "scan continuing shortly · imported books are saved"
            } else {
                "scan queued"
            }
        WorkInfo.State.BLOCKED -> "scan is waiting"
        WorkInfo.State.FAILED -> current.outputData.getString("failure") ?: "scan failed · try rescan"
        WorkInfo.State.CANCELLED -> "scan cancelled · imported books are saved"
        WorkInfo.State.SUCCEEDED -> report?.takeIf { it.isPartial }?.summary()
    }
