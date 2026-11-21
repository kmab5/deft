package com.kmab.deft.feature.book

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kmab.deft.core.database.model.BookDetails
import com.kmab.deft.core.datastore.TrackingSettingsRepository
import com.kmab.deft.core.model.BookState
import com.kmab.deft.core.model.BookTrackingSnapshot
import com.kmab.deft.library.scanner.BookRepository
import com.kmab.deft.tracking.RatingRepository
import com.kmab.deft.tracking.ReadThroughRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import javax.inject.Inject

data class BookDetailState(
    val details: BookDetails? = null,
    val loading: Boolean = true,
    val error: String? = null,
    val busy: Boolean = false,
    val forgotten: Boolean = false,
    val tracking: BookTrackingSnapshot? = null,
    val celebrations: Boolean = true,
    val celebrate: Boolean = false,
)

@HiltViewModel
class BookDetailViewModel
    @Inject
    constructor(
        private val savedState: SavedStateHandle,
        private val books: BookRepository,
        private val history: ReadThroughRepository,
        private val ratings: RatingRepository,
        preferences: TrackingSettingsRepository,
    ) : ViewModel() {
        private val bookId = checkNotNull(savedState.get<Long>("bookId"))
        private val error = MutableStateFlow<String?>(null)
        private val busy = MutableStateFlow(false)
        private val edits = Mutex()
        private var pendingEdits = 0
        private val forgotten = MutableStateFlow(false)
        private val celebrate = savedState.getStateFlow("celebrate", false)
        private val content =
            combine(
                books.observeBook(bookId),
                history.observe(bookId),
                preferences.preferences,
            ) { book, snapshot, prefs ->
                BookDetailState(book, loading = false, tracking = snapshot, celebrations = prefs.celebrations)
            }
        val state =
            combine(content, error, busy, forgotten, celebrate) { value, message, working, removed, finish ->
                value.copy(error = message, busy = working, forgotten = removed, celebrate = finish)
            }.catch { failure ->
                if (failure is CancellationException) throw failure
                emit(BookDetailState(loading = false, error = "Couldn't load this book. Go back and try again."))
            }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(BOOK_STOP_TIMEOUT), BookDetailState())

        val actions =
            BookTrackingActions(
                setState = { target, at, confirm ->
                    act {
                        history.setState(bookId, target, at, confirm)
                        if (target == BookState.FINISHED) savedState["celebrate"] = true
                    }
                },
                editDates = { id, start, end -> act { history.editDates(id, start, end) } },
                delete = { id -> act { history.delete(id) } },
                rating = { value -> act { ratings.update(bookId) { it.copy(rating = value) } } },
                favorite = { value -> act { ratings.update(bookId) { it.copy(favorite = value) } } },
                review = { value -> act { ratings.update(bookId) { it.copy(review = value) } } },
            )

        fun dismissCelebration() {
            savedState["celebrate"] = false
        }

        fun setIgnored(ignored: Boolean) = act { books.setIgnored(bookId, ignored) }

        fun forget() =
            act {
                books.forget(bookId)
                forgotten.value = true
            }

        fun dismissError() {
            error.value = null
        }

        private fun act(action: suspend () -> Unit) {
            pendingEdits++
            busy.value = true
            viewModelScope.launch {
                try {
                    edits.withLock { action() }
                } catch (
                    cancelled: CancellationException,
                ) {
                    throw cancelled
                } catch (_: IllegalArgumentException) {
                    error.value =
                        "Couldn't save these dates. The end must be on or after the start."
                } catch (
                    _: java.io.IOException,
                ) {
                    error.value = "Couldn't save this change. Try again."
                } catch (
                    _: android.database.sqlite.SQLiteException,
                ) {
                    error.value = "Couldn't save this change. Try again."
                } finally {
                    pendingEdits--
                    busy.value = pendingEdits > 0
                }
            }
        }
    }

private const val BOOK_STOP_TIMEOUT = 5_000L
