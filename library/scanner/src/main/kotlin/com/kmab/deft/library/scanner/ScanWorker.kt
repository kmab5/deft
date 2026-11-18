package com.kmab.deft.library.scanner

import android.content.Context
import android.os.SystemClock
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.Data
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.withContext
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
        private val reportStore = ScanReportStore(context)
        private var namedDetails = ScanDetailBuffer()
        private var latestSummary = ScanReport(0, 0, 0, 0, 0, 0, emptyList(), interrupted = true)
        private var checkpointSaved = false
        private val notification by lazy {
            ScanNotification(
                applicationContext,
                WorkManager.getInstance(applicationContext).createCancelPendingIntent(id),
            )
        }

        override suspend fun doWork(): Result =
            try {
                finish(continuation.combine(id.toString(), scan()))
            } catch (cancelled: CancellationException) {
                withContext(NonCancellable) {
                    try {
                        if (!checkpointSaved) checkpoint(continuation.combine(id.toString(), latestSummary))
                    } catch (error: IOException) {
                        cancelled.addSuppressed(error)
                    }
                    persistDetails()
                }
                throw cancelled
            } catch (_: IOException) {
                failed("Scan could not read the selected folders. Check access and rescan.")
            } catch (_: SecurityException) {
                failed("Folder access was revoked. Select the folder again.")
            }

        private suspend fun scan(): ScanReport {
            val started = SystemClock.elapsedRealtime()
            var lastProgress = 0L
            var foreground =
                promoteScan(inputData.getBoolean(ScanScheduler.USER_INITIATED, false)) {
                    setForeground(notification.foregroundInfo(0))
                }
            return runner.run(
                onProgress = { progress ->
                    namedDetails.add(progress.scanDetails)
                    progress.snapshot?.let { latestSummary = it }
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
            )
        }

        private suspend fun finish(report: ScanReport): Result {
            checkpoint(report)
            val storedId = persistDetails()
            if (!report.needsContinuation) {
                return Result.success(report.copy(reportId = storedId).toWorkData(System.currentTimeMillis()))
            }
            return if (canContinueScan(runAttemptCount)) {
                Result.retry()
            } else {
                failed(
                    "Scan paused by background limits. Open Deft and rescan; imported books are saved.",
                    report,
                )
            }
        }

        private fun checkpoint(report: ScanReport) {
            continuation.save(id.toString(), report)
            checkpointSaved = true
        }

        private suspend fun persistDetails(): String? =
            try {
                reportStore.append(id.toString(), namedDetails.snapshot())
                namedDetails = ScanDetailBuffer()
                id.toString()
            } catch (_: IOException) {
                null
            }

        private suspend fun failed(
            message: String,
            summary: ScanReport = continuation.combine(id.toString(), latestSummary),
        ): Result {
            namedDetails.add(ScanDetailEntry(id.toString(), "Library scan", "", ScanDetailOutcome.ERROR, message))
            val report = summary.copy(reportId = persistDetails(), interrupted = true, errors = summary.errors + 1)
            return Result.failure(
                Data
                    .Builder()
                    .putAll(
                        report.toWorkData(System.currentTimeMillis()),
                    ).putString("failure", message)
                    .build(),
            )
        }
    }

internal fun ScanReport.toWorkData(completedAtMillis: Long? = null) =
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
        "reportId" to reportId,
        "completedAtMillis" to (completedAtMillis ?: 0L),
        "interrupted" to interrupted,
    )

private const val PROGRESS_INTERVAL_MS = 1000L
private const val MAX_OUTPUT_ROOTS = 8
private const val MAX_OUTPUT_ROOT_CHARS = 64
private const val MAX_OUTPUT_ERRORS = 4
private const val MAX_OUTPUT_ERROR_CHARS = 240
