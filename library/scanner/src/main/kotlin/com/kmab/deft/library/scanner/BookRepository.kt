package com.kmab.deft.library.scanner

import androidx.room.withTransaction
import com.kmab.deft.core.common.SystemTimeSource
import com.kmab.deft.core.common.TimeSource
import com.kmab.deft.core.database.AppDatabase
import com.kmab.deft.core.database.entity.BookmarkEntity
import com.kmab.deft.core.database.model.BookDetails
import com.kmab.deft.core.model.BookFormat
import com.kmab.deft.core.model.BookSort
import com.kmab.deft.core.model.ReadingPosition
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.sync.withLock
import org.json.JSONException
import org.json.JSONObject
import javax.inject.Inject

// One book boundary keeps reading history and explicit forgetting consistent with scans.
@Suppress("TooManyFunctions")
class BookRepository
    @Inject
    constructor(
        private val db: AppDatabase,
        private val coordination: ScanCoordination = ScanCoordination(),
        private val time: TimeSource = SystemTimeSource,
    ) {
        fun observeBooks(
            includeMissing: Boolean,
            formats: Set<BookFormat>,
            sort: BookSort,
            reverseSort: Boolean = false,
        ) = db.bookDao().observeList(includeMissing, formats, sort, reverseSort)

        fun observeBook(id: Long) =
            combine(
                db.bookDao().observeById(id),
                db.bookDao().observeAuthorNames(id),
                db.bookFileDao().observeForBook(id),
            ) { book, names, files -> book?.let { BookDetails(it, names.joinToString(", "), files) } }

        suspend fun getBook(id: Long): BookDetails? =
            db.withTransaction {
                db.bookDao().getById(id)?.let { book ->
                    BookDetails(
                        book,
                        db.bookDao().getAuthorNames(id).joinToString(", "),
                        db.bookFileDao().getForBook(id),
                    )
                }
            }

        fun observeIgnoredBooks() = db.bookDao().observeIgnored()

        fun observeContinueReading() = db.bookDao().observeContinueReading()

        suspend fun savePosition(
            id: Long,
            position: ReadingPosition,
        ) {
            validatePosition(position)
            db.bookDao().savePosition(id, position.locatorJson, position.totalProgression, time.now())
        }

        suspend fun recordOpened(id: Long) = db.bookDao().recordOpened(id, time.now())

        suspend fun setIgnored(
            id: Long,
            ignored: Boolean,
        ) = db.bookDao().setIgnored(id, ignored)

        /** The caller confirms forgetting. Source files are deliberately never opened or removed here. */
        suspend fun forget(id: Long) =
            coordination.mutex.withLock {
                db.withTransaction { db.bookDao().forget(id) }
            }

        fun observeBookmarks(id: Long) = db.bookmarkDao().observeForBook(id)

        suspend fun addBookmark(
            id: Long,
            position: ReadingPosition,
            label: String,
        ): Long {
            validatePosition(position)
            return db.withTransaction {
                requireNotNull(db.bookDao().getById(id)) { "This book is no longer in Deft." }
                val result =
                    db.bookmarkDao().insert(
                        BookmarkEntity(
                            bookId = id,
                            locator = position.locatorJson,
                            totalProgression = position.totalProgression,
                            label = bookmarkLabel(label),
                            createdAt = time.now(),
                        ),
                    )
                if (result != -1L) result else requireNotNull(db.bookmarkDao().findId(id, position.locatorJson))
            }
        }

        suspend fun renameBookmark(
            id: Long,
            label: String,
        ) = db.bookmarkDao().rename(id, bookmarkLabel(label))

        suspend fun removeBookmark(id: Long) = db.bookmarkDao().remove(id)

        private fun bookmarkLabel(label: String) = label.trim().take(MAX_LABEL_LENGTH).ifBlank { "bookmark" }

        private fun validatePosition(position: ReadingPosition) {
            require(position.totalProgression.isFinite() && position.totalProgression in 0.0..1.0) {
                "Reading progress must be between 0 and 1."
            }
            require(position.locatorJson.length <= MAX_LOCATOR_LENGTH) { "Reading location is too large." }
            val valid =
                try {
                    JSONObject(position.locatorJson).optString("href").isNotBlank()
                } catch (
                    _: JSONException,
                ) {
                    false
                }
            require(valid) { "Reading location is invalid." }
        }

        private companion object {
            const val MAX_LABEL_LENGTH = 240
            const val MAX_LOCATOR_LENGTH = 65_536
        }
    }
