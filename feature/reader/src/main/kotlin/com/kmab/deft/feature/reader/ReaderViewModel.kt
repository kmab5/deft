@file:OptIn(org.readium.r2.shared.ExperimentalReadiumApi::class)

package com.kmab.deft.feature.reader

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kmab.deft.core.database.entity.BookmarkEntity
import com.kmab.deft.core.datastore.ReaderSettingsRepository
import com.kmab.deft.core.model.ReaderFont
import com.kmab.deft.core.model.ReaderPreferences
import com.kmab.deft.core.model.ReadingPosition
import com.kmab.deft.feature.readersettings.ReaderFontRepository
import com.kmab.deft.format.readium.OpenedEpub
import com.kmab.deft.format.readium.ReaderOpenException
import com.kmab.deft.library.scanner.BookRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.json.JSONObject
import org.readium.r2.shared.publication.Locator
import org.readium.r2.shared.publication.services.locateProgression
import javax.inject.Inject

internal data class ReaderSession(
    val bookId: Long,
    val title: String,
    val opened: OpenedEpub,
    val fonts: List<ReaderFont>,
    val positions: List<Locator>,
    val initialLocator: Locator?,
)

internal data class ReaderState(
    val loading: Boolean = true,
    val error: String? = null,
    val session: ReaderSession? = null,
    val locator: Locator? = null,
    val rendered: Boolean = false,
    val canReturn: Boolean = false,
    val bookmarks: List<BookmarkEntity> = emptyList(),
)

@HiltViewModel
internal class ReaderViewModel
    @Inject
    constructor(
        @ApplicationContext private val context: Context,
        private val books: BookRepository,
        settings: ReaderSettingsRepository,
        private val fonts: ReaderFontRepository,
        private val writer: ReaderPositionWriter,
        private val events: ReaderEvents,
    ) : ViewModel() {
        private val mutableState = MutableStateFlow(ReaderState())
        val state = mutableState.asStateFlow()
        val preferences = settings.preferences.stateIn(viewModelScope, SharingStarted.Eagerly, ReaderPreferences())
        val themes = settings.customThemes.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())
        private val messages = MutableSharedFlow<String>(extraBufferCapacity = 8)
        val notices = messages.asSharedFlow()
        val interaction = ReaderInteraction(mutableState, preferences, writer, events)
        private var started = false

        init {
            viewModelScope.launch { writer.failures.collect { notice(it) } }
        }

        // Untrusted format/provider failures become actionable UI errors; cancellation remains cancellation.
        @Suppress("TooGenericExceptionCaught")
        fun open(
            bookId: Long,
            restored: String?,
        ) {
            if (started) return
            started = true
            viewModelScope.launch {
                try {
                    val session = ReaderSessionLoader(context, books, fonts).load(bookId, restored)
                    mutableState.update {
                        it.copy(
                            loading = false,
                            session = session,
                            locator = session.initialLocator,
                        )
                    }
                    books.recordOpened(bookId)
                    books.observeBookmarks(bookId).collect { bookmarks ->
                        mutableState.update { it.copy(bookmarks = bookmarks) }
                    }
                } catch (cancel: CancellationException) {
                    throw cancel
                } catch (error: Exception) {
                    mutableState.update { it.copy(loading = false, error = error.readableMessage()) }
                }
            }
        }

        suspend fun progressionLocator(progress: Float): Locator? =
            state.value.session
                ?.opened
                ?.publication
                ?.locateProgression(progress.toDouble())

        fun toggleBookmark() {
            val current = state.value
            val locator = current.locator ?: return
            val session = current.session ?: return
            val existing = current.bookmarks.firstOrNull { bookmarkMatches(it, locator) }
            action {
                if (existing != null) {
                    books.removeBookmark(existing.id)
                    notice("Bookmark removed.")
                } else {
                    books.addBookmark(session.bookId, locator.toPosition(), locator.title ?: "Bookmark")
                    notice("Bookmark added. Name it in Contents → Bookmarks.")
                }
            }
        }

        fun renameBookmark(
            id: Long,
            label: String,
        ) = action { books.renameBookmark(id, label) }

        fun removeBookmark(id: Long) = action { books.removeBookmark(id) }

        fun notice(message: String) {
            messages.tryEmit(message)
        }

        private fun action(block: suspend () -> Unit) {
            viewModelScope.launch {
                try {
                    block()
                } catch (
                    cancel: CancellationException,
                ) {
                    throw cancel
                } catch (_: Exception) {
                    notice("That change could not be saved. Please try again.")
                }
            }
        }

        override fun onCleared() {
            state.value.session
                ?.opened
                ?.close()
        }
    }

internal fun parseLocator(json: String?): Locator? =
    json?.let {
        runCatching { Locator.fromJSON(JSONObject(it)) }.getOrNull()
    }

internal fun Locator.toPosition() =
    ReadingPosition(toJSON().toString(), (locations.totalProgression ?: 0.0).coerceIn(0.0, 1.0))

internal fun bookmarkMatches(
    bookmark: BookmarkEntity,
    locator: Locator,
): Boolean =
    bookmark.locator == locator.toJSON().toString() || parseLocator(bookmark.locator)?.let {
        it.href == locator.href &&
            kotlin.math.abs((it.locations.progression ?: 0.0) - (locator.locations.progression ?: 0.0)) <
            BOOKMARK_PROGRESSION_TOLERANCE
    } == true

private fun Exception.readableMessage(): String =
    when (this) {
        is ReaderOpenException -> message.orEmpty()
        is SecurityException -> "Deft no longer has access to this folder. Reconnect it in Library settings."
        else -> "This book could not be read. Check the file and available storage, then try again."
    }

private const val BOOKMARK_PROGRESSION_TOLERANCE = 0.001
