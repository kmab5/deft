package com.kmab.deft.library.scanner

import androidx.room.withTransaction
import com.kmab.deft.core.common.ContentHasher
import com.kmab.deft.core.common.TimeSource
import com.kmab.deft.core.database.AppDatabase
import com.kmab.deft.core.database.entity.BookEntity
import com.kmab.deft.core.database.entity.BookFileEntity
import com.kmab.deft.core.model.BookFormat
import com.kmab.deft.core.model.ContentHash
import com.kmab.deft.core.model.sortTitle
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.IOException

/** Serializes scans on this instance. Files are only ever opened for reading. */
class LibraryScanner(
    private val tree: DocumentTree,
    private val hasher: ContentHasher,
    private val registry: MetadataExtractorRegistry,
    private val coverStore: CoverStore,
    private val db: AppDatabase,
    private val time: TimeSource,
) {
    private val mutex = Mutex()
    private val files get() = db.bookFileDao()
    private val books get() = db.bookDao()

    suspend fun scan(
        roots: List<String>,
        formats: Set<BookFormat>,
        removedRoots: List<String> = emptyList(),
        onProgress: suspend (ScanProgress) -> Unit = {},
        shouldYield: () -> Boolean = { false },
    ): ScanReport =
        mutex.withLock {
            withContext(Dispatchers.IO) {
                val selected = roots.distinct()
                val accessible = selected.filter { isAccessible(it) }
                validateRoots(accessible)
                val session =
                    ScanSession(files.getAll(), formats, selected - accessible.toSet(), onProgress, shouldYield)
                try {
                    accessible.forEach { session.scanRoot(it) }
                } catch (_: ScanBudgetReached) {
                    session.needsContinuation = true
                }
                session.finish(removedRoots.distinct().filterNot { it in selected })
            }
        }

    private suspend fun isAccessible(root: String): Boolean =
        try {
            tree.isAccessible(root)
        } catch (_: IOException) {
            false
        } catch (_: SecurityException) {
            false
        }

    private suspend fun validateRoots(roots: List<String>) {
        // Provider relationship failures propagate before any database writes.
        for ((index, root) in roots.withIndex()) {
            for (other in roots.drop(index + 1)) {
                require(!tree.isChild(root, other) && !tree.isChild(other, root)) {
                    "Subfolders are already included; select non-overlapping folders"
                }
            }
        }
    }

    private inner class ScanSession(
        before: List<BookFileEntity>,
        private val formats: Set<BookFormat>,
        unavailableRoots: List<String>,
        private val onProgress: suspend (ScanProgress) -> Unit,
        private val shouldYield: () -> Boolean,
    ) {
        private val existingFiles = before.associateBy { it.uri }
        private val presentBefore = before.filterNot { it.missing }.groupBy { it.bookId }
        private val candidates = mutableMapOf<Long, Int>()
        private val created = mutableSetOf<Long>()
        private var added = 0
        private var updated = 0
        private var unchanged = 0
        private var errors = 0
        private val unavailable = unavailableRoots.toMutableList()
        private val incomplete = mutableListOf<String>()
        private val details = linkedSetOf<String>()
        private val observedBooks = mutableSetOf<Long>()
        private val namedDetails = ScanDetailRecorder()
        private val candidateEntries = mutableMapOf<Long, MutableList<DocumentEntry>>()
        private var processed = 0
        private var skipped = 0
        private var duplicates = 0
        private var fallbacks = 0
        private var failed = 0
        var needsContinuation = false

        private fun recordError(
            path: String,
            reason: String,
        ) {
            errors++
            if (details.size < MAX_ERROR_DETAILS) {
                details += "${path.take(MAX_ERROR_PATH_CHARS)}: ${reason.take(MAX_ERROR_REASON_CHARS)}"
            }
        }

        suspend fun scanRoot(root: String) {
            currentCoroutineContext().ensureActive()
            if (!isAccessible(root)) {
                unavailable += root
                return
            }
            val exclusions =
                db
                    .libraryFolderDao()
                    .observeExclusions(root)
                    .first()
                    .map { it.documentId }
                    .toSet()
            val seen = mutableSetOf<String>()
            val enumerated = mutableSetOf<String>()
            var complete = true
            try {
                tree.walk(root, exclusions).collect { entry ->
                    if (shouldYield()) throw ScanBudgetReached()
                    require(entry.rootUri == root) { "Document entry belongs to another root" }
                    if (!enumerated.add(entry.uri)) return@collect
                    val format = BookFormat.fromFileName(entry.name)
                    if (format != null && format in formats) {
                        if (scanEntry(entry, format)) seen += entry.uri
                        processed++
                    } else {
                        skipped++
                        val reason =
                            if (format ==
                                null
                            ) {
                                "Unsupported file type"
                            } else {
                                "${format.name} is disabled in library settings"
                            }
                        namedDetails.file(entry, ScanDetailOutcome.SKIPPED, reason)
                    }
                    onProgress(ScanProgress(processed, skipped, namedDetails.drain(), snapshotReport()))
                }
            } catch (_: IOException) {
                recordError("Library folder", "Folder listing incomplete. Check access and rescan.")
                incomplete += root
                complete = false
            } catch (_: SecurityException) {
                recordError("Library folder", "Folder access was revoked. Select the folder again.")
                incomplete += root
                complete = false
            }
            currentCoroutineContext().ensureActive()
            if (complete && hasUnseenFiles(root, seen)) {
                files.markMissingUnderRootsExcept(listOf(root), seen)
            }
        }

        private fun hasUnseenFiles(
            root: String,
            seen: Set<String>,
        ): Boolean = existingFiles.values.any { it.rootUri == root && !it.missing && it.uri !in seen }

        private suspend fun scanEntry(
            entry: DocumentEntry,
            format: BookFormat,
        ): Boolean {
            currentCoroutineContext().ensureActive()
            val existing = existingFiles[entry.uri]
            if (existing != null && fingerprintMatches(existing, entry)) {
                useUnchanged(existing, entry)
                return true
            }
            return try {
                indexFile(entry, format, existing)
                true
            } catch (_: IOException) {
                failed++
                recordError(entry.displayPath, "File could not be read. Check access and rescan.")
                namedDetails.file(entry, ScanDetailOutcome.ERROR, "File could not be read. Check access and rescan.")
                existing?.missing == false
            } catch (_: SecurityException) {
                failed++
                recordError(entry.displayPath, "Permission to read this file was revoked.")
                namedDetails.file(entry, ScanDetailOutcome.ERROR, "Permission to read this file was revoked.")
                existing?.missing == false
            }
        }

        private suspend fun useUnchanged(
            existing: BookFileEntity,
            entry: DocumentEntry,
        ) {
            if (existing.missing || existing.rootUri != entry.rootUri || existing.displayPath != entry.displayPath) {
                db.withTransaction { saveFile(existing, entry, existing.bookId) }
            }
            if (existing.missing) candidates.merge(existing.bookId, 1, Int::plus) else unchanged++
            val duplicate = !observedBooks.add(existing.bookId)
            if (duplicate) duplicates++
            val outcome =
                when {
                    existing.missing -> ScanDetailOutcome.RESTORED
                    duplicate -> ScanDetailOutcome.DUPLICATE
                    else -> ScanDetailOutcome.UNCHANGED
                }
            val reason =
                when (outcome) {
                    ScanDetailOutcome.RESTORED -> "Existing book restored with its history"
                    ScanDetailOutcome.DUPLICATE -> "Same content as another book file"
                    else -> "Already indexed; content unchanged"
                }
            namedDetails.file(entry, outcome, reason)
        }

        private suspend fun indexFile(
            entry: DocumentEntry,
            format: BookFormat,
            existing: BookFileEntity?,
        ) {
            val hash = tree.open(entry.uri).use(hasher::hash)
            currentCoroutineContext().ensureActive()
            val known = books.findByHash(hash.hex)
            val bookId =
                if (known == null) {
                    createBook(entry, format, hash, existing)
                } else {
                    db.withTransaction { saveFile(existing, entry, known.id) }
                    known.id
                }
            when {
                existing != null -> updated++
                known == null -> added++
                bookId in created -> unchanged++
                else -> candidates.merge(bookId, 1, Int::plus)
            }
            if (!observedBooks.add(bookId)) duplicates++
            namedDetails.indexed(entry, changed = existing != null, newBook = known == null)
            if (known != null && existing == null) candidateEntries.getOrPut(bookId) { mutableListOf() }.add(entry)
        }

        private suspend fun createBook(
            entry: DocumentEntry,
            format: BookFormat,
            hash: ContentHash,
            existing: BookFileEntity?,
        ): Long {
            val (metadata, metadataError) = tree.open(entry.uri).use { registry.extract(format, it, entry.name) }
            if (metadataError != null) {
                fallbacks++
                recordError(entry.displayPath, "Using filename details: $metadataError")
                namedDetails.file(entry, ScanDetailOutcome.FALLBACK, "Using filename details: $metadataError")
            }
            val cover =
                try {
                    metadata.coverImage?.let { coverStore.save(hash, it) }
                } catch (_: IOException) {
                    recordError(entry.displayPath, "Cover could not be saved; book is included.")
                    namedDetails.file(entry, ScanDetailOutcome.ERROR, "Cover could not be saved; book is included.")
                    null
                }
            val title = metadata.title ?: entry.name
            val entity =
                BookEntity(
                    contentHash = hash.hex,
                    format = format,
                    title = title,
                    sortTitle = sortTitle(title),
                    language = metadata.language,
                    publisher = metadata.publisher,
                    publishedDate = metadata.publishedDate,
                    description = metadata.description,
                    isbn10 = metadata.isbn10,
                    isbn13 = metadata.isbn13,
                    pageCount = metadata.pageCount,
                    coverPath = cover,
                    extractedSeriesName = metadata.seriesName,
                    extractedSeriesIndex = metadata.seriesIndex,
                    drmProtected = metadata.drmProtected,
                    metadataError = metadataError,
                    addedAt = time.now(),
                )
            val id =
                db.withTransaction {
                    val id = books.insert(entity)
                    db.authorDao().setBookAuthors(id, metadata.authors)
                    saveFile(existing, entry, id)
                    id
                }
            created += id
            return id
        }

        suspend fun finish(removedRoots: List<String>): ScanReport {
            currentCoroutineContext().ensureActive()
            if (!needsContinuation) {
                db.withTransaction {
                    removedRoots.forEach { files.markMissingForRoot(it) }
                    files.pruneMissingWherePresentCopyExists()
                }
            }
            val after = files.getAll().filterNot { it.missing }.groupBy { it.bookId }
            var reattached = 0
            for ((id, count) in candidates) {
                val oldUris = presentBefore[id].orEmpty().map { it.uri }.toSet()
                val movedOrRestored = after[id].orEmpty().none { it.uri in oldUris }
                if (movedOrRestored) {
                    reattached++
                    unchanged += count - 1
                    candidateEntries[id].orEmpty().forEach {
                        namedDetails.file(it, ScanDetailOutcome.RESTORED, "Existing book restored with its history")
                    }
                } else {
                    unchanged += count
                }
            }
            val folderNames = db.libraryFolderDao().getAll().associate { it.treeUri to it.displayName }
            (unavailable + incomplete).distinct().forEach { root ->
                val reason =
                    if (root in
                        unavailable
                    ) {
                        "Folder unavailable. Grant access again."
                    } else {
                        "Folder listing incomplete; unseen books kept present."
                    }
                namedDetails.folder(root, folderNames[root] ?: "Library folder", reason)
            }
            val report = snapshotReport(reattached, presentBefore.keys.count { it !in after }, complete = true)
            onProgress(ScanProgress(processed, skipped, namedDetails.drain(), report))
            return report
        }

        private fun snapshotReport(
            reattached: Int = 0,
            markedMissing: Int = 0,
            complete: Boolean = false,
        ) = ScanReport(
            added,
            updated,
            unchanged,
            reattached,
            markedMissing,
            errors,
            unavailable,
            processed,
            skipped,
            duplicates,
            fallbacks,
            failed,
            incomplete,
            details.toList(),
            needsContinuation,
            scanDetails = if (complete) namedDetails.snapshot() else ScanDetails(),
            interrupted = !complete,
        )
    }

    private class ScanBudgetReached : RuntimeException()

    private companion object {
        const val MAX_ERROR_DETAILS = 8
        const val MAX_ERROR_PATH_CHARS = 120
        const val MAX_ERROR_REASON_CHARS = 160
    }

    private fun fingerprintMatches(
        file: BookFileEntity,
        entry: DocumentEntry,
    ): Boolean {
        val usable = entry.size >= 0 && entry.lastModified > 0
        return usable && file.size == entry.size && file.lastModified == entry.lastModified
    }

    private suspend fun saveFile(
        existing: BookFileEntity?,
        entry: DocumentEntry,
        bookId: Long,
    ) {
        val file =
            BookFileEntity(
                id = existing?.id ?: 0,
                bookId = bookId,
                rootUri = entry.rootUri,
                uri = entry.uri,
                displayPath = entry.displayPath,
                size = entry.size,
                lastModified = entry.lastModified,
                missing = false,
                lastSeenAt = time.now(),
            )
        if (existing == null) files.insert(file) else files.update(file)
    }
}
