package com.kmab.deft.library.scanner

import android.net.Uri
import com.kmab.deft.core.common.TimeSource
import com.kmab.deft.core.database.AppDatabase
import com.kmab.deft.core.database.entity.ExcludedFolderEntity
import com.kmab.deft.core.database.entity.LibraryFolderEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LibraryFolderRepository
    @Inject
    constructor(
        private val db: AppDatabase,
        private val tree: DocumentTree,
        private val access: FolderAccess,
        private val scheduler: ScanScheduler,
        private val time: TimeSource,
        private val coordination: ScanCoordination,
    ) {
        private val dao get() = db.libraryFolderDao()
        val folders = dao.observeAll()

        fun exclusions(treeUri: String) = dao.observeExclusions(treeUri)

        suspend fun add(treeUri: Uri) =
            withContext(Dispatchers.IO) {
                coordination.mutex.withLock {
                    val root = treeUri.toString()
                    for (selected in dao.getAll()) {
                        require(root != selected.treeUri) {
                            "${selected.displayName} is already selected. Its subfolders are included automatically."
                        }
                        require(!tree.isChild(selected.treeUri, root)) {
                            "This folder is already included inside ${selected.displayName}. " +
                                "Manage its excluded subfolders below."
                        }
                        require(!tree.isChild(root, selected.treeUri)) {
                            "This parent folder contains the selected folder ${selected.displayName}. " +
                                "Remove ${selected.displayName} and any other selected subfolders from Deft, " +
                                "then add the parent folder again. Files and reading history stay safe. " +
                                "Set up any subfolder exclusions again on the parent."
                        }
                    }
                    val existingGrant = access.hasRead(treeUri)
                    var inserted = false
                    try {
                        access.takeRead(treeUri)
                        dao.insert(LibraryFolderEntity(root, access.displayName(treeUri), time.now()))
                        inserted = true
                    } finally {
                        if (!inserted && !existingGrant) access.releaseRead(treeUri)
                    }
                    scheduler.scanNow()
                }
            }

        suspend fun remove(treeUri: String) =
            withContext(Dispatchers.IO) {
                coordination.mutex.withLock {
                    dao.removeForReconciliation(treeUri)
                    // A failed/revoked grant cannot discard durable removal intent or prevent scheduling.
                    try {
                        access.releaseRead(Uri.parse(treeUri))
                    } catch (_: SecurityException) {
                        // Provider may already have revoked it; scan also retries release after restart.
                    } finally {
                        scheduler.scanNow()
                    }
                }
            }

        suspend fun exclude(
            treeUri: String,
            subfolderUri: Uri,
        ) = withContext(Dispatchers.IO) {
            coordination.mutex.withLock {
                require(dao.getAll().any { it.treeUri == treeUri }) { "Select the library folder first" }
                require(
                    tree.isChild(treeUri, subfolderUri.toString()) && !tree.isChild(subfolderUri.toString(), treeUri),
                ) {
                    "Choose a subfolder inside this library folder"
                }
                val documentId = SafDocumentTree.documentId(subfolderUri)
                val document =
                    android.provider.DocumentsContract.buildDocumentUriUsingTree(
                        Uri.parse(treeUri),
                        documentId,
                    )
                dao.addExclusion(ExcludedFolderEntity(treeUri, documentId, access.displayName(document)))
                scheduler.scanNow()
            }
        }

        suspend fun unexclude(
            treeUri: String,
            documentId: String,
        ) = withContext(Dispatchers.IO) {
            coordination.mutex.withLock {
                dao.removeExclusion(treeUri, documentId)
                scheduler.scanNow()
            }
        }
    }
