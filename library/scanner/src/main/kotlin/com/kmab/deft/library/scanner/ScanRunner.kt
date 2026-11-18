package com.kmab.deft.library.scanner

import android.net.Uri
import com.kmab.deft.core.common.TimeSource
import com.kmab.deft.core.database.AppDatabase
import com.kmab.deft.core.datastore.LibrarySettingsRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.sync.withLock
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ScanRunner
    @Inject
    constructor(
        private val db: AppDatabase,
        private val scanner: LibraryScanner,
        private val settings: LibrarySettingsRepository,
        private val time: TimeSource,
        private val coordination: ScanCoordination,
        private val access: FolderAccess,
    ) {
        suspend fun run(
            onProgress: suspend (ScanProgress) -> Unit = {},
            shouldYield: () -> Boolean = { false },
        ): ScanReport =
            coordination.mutex.withLock {
                val dao = db.libraryFolderDao()
                val selected = dao.getAll().map { it.treeUri }
                val removed = dao.pendingRemovals()
                for (root in removed.filterNot { it in selected }) {
                    try {
                        access.releaseRead(Uri.parse(root))
                    } catch (_: SecurityException) {
                        // Already revoked by the provider; reconciliation only touches app-owned rows.
                    }
                }
                val report =
                    scanner.scan(
                        selected,
                        settings.settings.first().enabledFormats,
                        removed,
                        onProgress,
                        shouldYield,
                    )
                if (!report.needsContinuation) {
                    settings.setLastScanAt(time.now())
                    dao.acknowledgeRemovals(removed)
                }
                report
            }
    }
