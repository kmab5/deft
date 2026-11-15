package com.kmab.deft.library.scanner

import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.kmab.deft.core.common.TimeSource
import com.kmab.deft.core.database.AppDatabase
import com.kmab.deft.core.datastore.LibrarySettingsRepository
import kotlinx.coroutines.flow.first
import java.time.Duration
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ScanScheduler
    @Inject
    constructor(
        private val workManager: WorkManager,
        private val settings: LibrarySettingsRepository,
        private val time: TimeSource,
        private val db: AppDatabase,
    ) {
        /** Removal intent lives in Room, never in replaceable WorkManager input data. */
        fun scanNow() {
            enqueue(ExistingWorkPolicy.REPLACE)
        }

        suspend fun scanOnStartIfStale() {
            val last = settings.settings.first().lastScanAt
            if (db.libraryFolderDao().pendingRemovals().isNotEmpty() || last == null ||
                Duration.between(last, time.now()) >= STALE_AFTER
            ) {
                enqueue(ExistingWorkPolicy.KEEP)
            }
        }

        private fun enqueue(policy: ExistingWorkPolicy) {
            workManager.enqueueUniqueWork(WORK_NAME, policy, OneTimeWorkRequestBuilder<ScanWorker>().build())
        }

        companion object {
            const val WORK_NAME = "library-scan"
            private val STALE_AFTER = Duration.ofHours(6)
        }
    }
