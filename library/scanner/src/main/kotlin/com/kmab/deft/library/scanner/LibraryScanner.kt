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
    ): ScanReport =
        mutex.withLock {
            withContext(Dispatchers.IO) {
                val selected = roots.distinct()
                val accessible = selected.filter { isAccessible(it) }
                validateRoots(accessible)
                val session = ScanSession(files.getAll(), formats, selected - accessible.toSet())
                accessible.forEach { session.scanRoot(it) }
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
    ) {
        private val presentBefore = before.filterNot { it.missing }.groupBy { it.bookId }
        private val candidates = mutableMapOf<Long, Int>()
        private val created = mutableSetOf<Long>()
        private var added = 0
        private var updated = 0
        private var unchanged = 0
        private var errors = 0
        private val unavailable = unavailableRoots.toMutableList()

        suspend fun scanRoot(root: String) {
            currentCoroutineContext().ensureActive()
            val accessible =
                try {
                    tree.isAccessible(root)
                } catch (_: IOException) {
                    false
                } catch (_: SecurityException) {
                    false
                }
            if (!accessible) {
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
            var complete = true
            try {
                tree.walk(root, exclusions).collect { entry ->
                    require(entry.rootUri == root) { "Document entry belongs to another root" }
                    val format = BookFormat.fromFileName(entry.name)
                    if (format != null && format in formats) {
                        if (scanEntry(entry, format)) seen += entry.uri
                    }
                }
            } catch (_: IOException) {
                errors++
                complete = false
            } catch (_: SecurityException) {
                errors++
                complete = false
            }
            currentCoroutineContext().ensureActive()
            if (complete) files.markMissingUnderRootsExcept(listOf(root), seen)
        }

        private suspend fun scanEntry(
            entry: DocumentEntry,
            format: BookFormat,
        ): Boolean {
            currentCoroutineContext().ensureActive()
            val existing = files.findByUri(entry.uri)
            if (existing != null && fingerprintMatches(existing, entry)) {
                db.withTransaction { saveFile(existing, entry, existing.bookId) }
                if (existing.missing) candidates.merge(existing.bookId, 1, Int::plus) else unchanged++
                return true
            }
            return try {
                indexFile(entry, format, existing)
                true
            } catch (_: IOException) {
                errors++
                existing?.missing == false
            } catch (_: SecurityException) {
                errors++
                existing?.missing == false
            }
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
        }

        private suspend fun createBook(
            entry: DocumentEntry,
            format: BookFormat,
            hash: ContentHash,
            existing: BookFileEntity?,
        ): Long {
            val (metadata, metadataError) = tree.open(entry.uri).use { registry.extract(format, it, entry.name) }
            if (metadataError != null) errors++
            val cover =
                try {
                    metadata.coverImage?.let { coverStore.save(hash, it) }
                } catch (_: IOException) {
                    errors++
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
            db.withTransaction {
                removedRoots.forEach { files.markMissingForRoot(it) }
                files.pruneMissingWherePresentCopyExists()
            }
            val after = files.getAll().filterNot { it.missing }.groupBy { it.bookId }
            var reattached = 0
            for ((id, count) in candidates) {
                val oldUris = presentBefore[id].orEmpty().map { it.uri }.toSet()
                val movedOrRestored = after[id].orEmpty().none { it.uri in oldUris }
                if (movedOrRestored) {
                    reattached++
                    unchanged += count - 1
                } else {
                    unchanged += count
                }
            }
            return ScanReport(
                added,
                updated,
                unchanged,
                reattached,
                presentBefore.keys.count { it !in after },
                errors,
                unavailable,
            )
        }
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
