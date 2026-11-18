package com.kmab.deft.library.scanner

import android.content.Context
import android.os.SystemClock
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import java.io.IOException

@HiltWorker
class ScanWorker
    @AssistedInject
    constructor(
        @Assisted context: Context,
        @Assisted parameters: WorkerParameters,
        private val runner: ScanRunner,
    ) : CoroutineWorker(context, parameters) {
        private val continuation =
            ScanContinuationStore(context.getSharedPreferences("scan-continuation", Context.MODE_PRIVATE))
        private val notification by lazy {
            ScanNotification(
                applicationContext,
                WorkManager.getInstance(applicationContext).createCancelPendingIntent(id),
            )
        }

        override suspend fun doWork(): Result =
            try {
                val started = SystemClock.elapsedRealtime()
                var lastProgress = 0L
                var foreground =
                    promoteScan(inputData.getBoolean(ScanScheduler.USER_INITIATED, false)) {
                        setForeground(notification.foregroundInfo(0))
                    }
                val report =
                    continuation.combine(
                        id.toString(),
                        runner.run(
                            onProgress = { progress ->
                                val now = SystemClock.elapsedRealtime()
                                if (now - lastProgress >= PROGRESS_INTERVAL_MS || progress.processedFiles == 1) {
                                    setProgress(
                                        workDataOf(
                                            "processedFiles" to progress.processedFiles,
                                            "skippedFiles" to progress.skippedFiles,
                                        ),
                                    )
                                    if (foreground) {
                                        foreground =
                                            promoteScan(
                                                true,
                                            ) { setForeground(notification.foregroundInfo(progress.processedFiles)) }
                                    }
                                    lastProgress = now
                                }
                            },
                            shouldYield = { shouldYieldScan(foreground, SystemClock.elapsedRealtime() - started) },
                        ),
                    )
                if (report.needsContinuation) {
                    continuation.save(id.toString(), report)
                    if (canContinueScan(runAttemptCount)) {
                        Result.retry()
                    } else {
                        Result.failure(
                            workDataOf(
                                "failure" to
                                    "Scan paused by background limits. Open Deft and rescan; imported books are saved.",
                            ),
                        )
                    }
                } else {
                    continuation.clear()
                    Result.success(report.toWorkData())
                }
            } catch (_: IOException) {
                Result.failure(
                    workDataOf("failure" to "Scan could not read the selected folders. Check access and rescan."),
                )
            } catch (_: SecurityException) {
                Result.failure(workDataOf("failure" to "Folder access was revoked. Select the folder again."))
            }
    }

internal fun ScanReport.toWorkData() =
    workDataOf(
        "added" to added,
        "updated" to updated,
        "unchanged" to unchanged,
        "reattached" to reattached,
        "markedMissing" to markedMissing,
        "errors" to errors,
        "unavailableRoots" to
            unavailableRoots.take(MAX_OUTPUT_ROOTS).map { it.take(MAX_OUTPUT_ROOT_CHARS) }.toTypedArray(),
        "processedFiles" to processedFiles,
        "skippedFiles" to skippedFiles,
        "duplicateFiles" to duplicateFiles,
        "metadataFallbacks" to metadataFallbacks,
        "failedFiles" to failedFiles,
        "incompleteRoots" to
            incompleteRoots.take(MAX_OUTPUT_ROOTS).map { it.take(MAX_OUTPUT_ROOT_CHARS) }.toTypedArray(),
        "errorDetails" to errorDetails.take(MAX_OUTPUT_ERRORS).map { it.take(MAX_OUTPUT_ERROR_CHARS) }.toTypedArray(),
        "unavailableRootCount" to maxOf(unavailableRootCount, unavailableRoots.size),
        "incompleteRootCount" to maxOf(incompleteRootCount, incompleteRoots.size),
    )

private const val PROGRESS_INTERVAL_MS = 1000L
private const val MAX_OUTPUT_ROOTS = 8
private const val MAX_OUTPUT_ROOT_CHARS = 64
private const val MAX_OUTPUT_ERRORS = 4
private const val MAX_OUTPUT_ERROR_CHARS = 240
