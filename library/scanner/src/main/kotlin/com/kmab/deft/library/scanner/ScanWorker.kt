package com.kmab.deft.library.scanner

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
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
        override suspend fun doWork(): Result =
            try {
                val report = runner.run()
                Result.success(
                    workDataOf(
                        "added" to report.added,
                        "updated" to report.updated,
                        "unchanged" to report.unchanged,
                        "reattached" to report.reattached,
                        "markedMissing" to report.markedMissing,
                        "errors" to report.errors,
                        "unavailableRoots" to report.unavailableRoots.toTypedArray(),
                    ),
                )
            } catch (_: IOException) {
                Result.retry()
            } catch (_: SecurityException) {
                Result.retry()
            }
    }
