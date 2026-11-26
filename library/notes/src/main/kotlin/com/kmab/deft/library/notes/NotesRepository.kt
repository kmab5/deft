package com.kmab.deft.library.notes

import androidx.room.withTransaction
import com.kmab.deft.core.database.AppDatabase
import com.kmab.deft.core.database.dao.NoteRow
import com.kmab.deft.core.database.entity.HighlightEntity
import com.kmab.deft.core.model.HighlightColor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import org.json.JSONException
import org.json.JSONObject
import java.time.Instant
import javax.inject.Inject

class NotesRepository
    @Inject
    constructor(
        private val db: AppDatabase,
    ) {
        private val highlights = db.highlightDao()

        fun observe(filter: NoteFilter = NoteFilter()): Flow<List<NoteEntry>> =
            highlights
                .observeNotes(filter.bookId, filter.color?.name, filter.type?.name)
                .map { rows -> rows.filter { it.matches(filter.query) }.map(NoteRow::toEntry) }
                .flowOn(Dispatchers.Default)

        fun observeHighlights(bookId: Long): Flow<List<HighlightEntity>> = highlights.observeForBook(bookId)

        suspend fun saveHighlight(draft: HighlightDraft): Long? {
            validate(draft)
            return db.withTransaction {
                require(db.bookDao().getById(draft.bookId)?.ignored == false) { "This book is unavailable in Notes." }
                val existing = findExisting(draft)
                val note = draft.note?.takeUnless(String::isBlank)
                if (draft.color == HighlightColor.NONE && note == null) {
                    existing?.let { highlights.remove(it.id) }
                    null
                } else {
                    save(draft, existing, note)
                }
            }
        }

        suspend fun deleteHighlight(id: Long) = highlights.remove(id)

        suspend fun renameBookmark(
            id: Long,
            label: String,
        ) = db.bookmarkDao().rename(id, label.trim().take(MAX_BOOKMARK_LABEL_LENGTH).ifBlank { "bookmark" })

        suspend fun deleteBookmark(id: Long) = db.bookmarkDao().remove(id)

        suspend fun exportMarkdown(bookId: Long): String {
            val (title, authors, notes) =
                db.withTransaction {
                    val book = requireNotNull(db.bookDao().getById(bookId)) { "This book is no longer in Deft." }
                    require(!book.ignored) { "This book is unavailable in Notes." }
                    Triple(
                        book.titleOverride ?: book.title,
                        db.bookDao().getAuthorNames(bookId).joinToString(", "),
                        highlights.getNotesForBook(bookId).map(NoteRow::toEntry),
                    )
                }
            return notesMarkdown(title, authors, notes)
        }

        private suspend fun findExisting(draft: HighlightDraft): HighlightEntity? {
            val id = draft.id ?: return highlights.findAt(draft.bookId, draft.locator)
            val existing = requireNotNull(highlights.findById(id)) { "This annotation was removed." }
            require(existing.bookId == draft.bookId && existing.locator == draft.locator) {
                "This annotation belongs to a different reading location."
            }
            return existing
        }

        private suspend fun save(
            draft: HighlightDraft,
            existing: HighlightEntity?,
            note: String?,
        ): Long {
            val now = Instant.ofEpochMilli(System.currentTimeMillis())
            val updated =
                HighlightEntity(
                    id = existing?.id ?: 0,
                    bookId = draft.bookId,
                    locator = draft.locator,
                    text = draft.text,
                    color = draft.color,
                    underline = draft.underline,
                    note = note,
                    totalProgression = draft.totalProgression,
                    createdAt = existing?.createdAt ?: now,
                    updatedAt = existing?.updatedAt ?: now,
                )
            if (existing == null) return highlights.insert(updated)
            if (updated != existing) {
                highlights.update(updated.copy(updatedAt = maxOf(now, existing.updatedAt)))
            }
            return existing.id
        }

        private fun validate(draft: HighlightDraft) {
            require(draft.text.isNotBlank()) { "Select a passage to annotate." }
            require(draft.totalProgression in 0.0..1.0) { "Reading progress must be between 0 and 1." }
            require(draft.locator.length <= MAX_LOCATOR_LENGTH) { "Reading location is too large." }
            val href =
                try {
                    JSONObject(draft.locator).opt("href") as? String
                } catch (_: JSONException) {
                    null
                }
            require(!href.isNullOrBlank()) { "Reading location is invalid." }
        }

        private companion object {
            const val MAX_BOOKMARK_LABEL_LENGTH = 240
            const val MAX_LOCATOR_LENGTH = 65_536
        }
    }

private fun NoteRow.matches(query: String): Boolean =
    query.isEmpty() ||
        sequenceOf(text, note, label, bookTitle, authors).any { it?.contains(query, ignoreCase = true) == true }

private fun NoteRow.toEntry() =
    NoteEntry(
        id = id,
        bookId = bookId,
        bookTitle = bookTitle,
        authors = authors,
        bookFormat = bookFormat,
        missing = missing,
        drmProtected = drmProtected,
        locator = locator,
        totalProgression = totalProgression,
        text = text,
        note = note,
        color = color,
        underline = underline,
        label = label,
        createdAt = createdAt,
        updatedAt = updatedAt,
        type =
            when (color) {
                null -> NoteType.BOOKMARK
                HighlightColor.NONE -> NoteType.NOTE
                else -> NoteType.HIGHLIGHT
            },
    )
