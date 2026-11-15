package com.kmab.deft.library.scanner

import kotlinx.coroutines.sync.Mutex
import javax.inject.Inject
import javax.inject.Singleton

/** Folder edits, scan snapshots and outbox acknowledgement share one process-local lock. */
@Singleton
class ScanCoordination
    @Inject
    constructor() {
        internal val mutex = Mutex()
    }
