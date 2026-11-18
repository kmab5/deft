package com.kmab.deft.feature.book

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kmab.deft.core.database.model.BookDetails
import com.kmab.deft.library.scanner.BookRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class BookDetailState(
    val details: BookDetails? = null,
    val loading: Boolean = true,
    val error: String? = null,
    val busy: Boolean = false,
    val forgotten: Boolean = false,
)

@HiltViewModel
class BookDetailViewModel
    @Inject
    constructor(
        savedState: SavedStateHandle,
        private val books: BookRepository,
    ) : ViewModel() {
        private val bookId = checkNotNull(savedState.get<Long>("bookId"))
        private val error = MutableStateFlow<String?>(null)
        private val busy = MutableStateFlow(false)
        private val forgotten = MutableStateFlow(false)
        val state =
            combine(books.observeBook(bookId), error, busy, forgotten) { book, message, working, removed ->
                BookDetailState(book, loading = false, error = message, busy = working, forgotten = removed)
            }.catch { failure ->
                if (failure is CancellationException) throw failure
                emit(BookDetailState(loading = false, error = "Couldn't load this book. Go back and try again."))
            }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT), BookDetailState())

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
            if (busy.value) return
            busy.value = true
            viewModelScope.launch {
                try {
                    action()
                } catch (cancelled: CancellationException) {
                    throw cancelled
                } catch (_: java.io.IOException) {
                    error.value = "Couldn't save this change. Try again."
                } catch (_: android.database.sqlite.SQLiteException) {
                    error.value = "Couldn't save this change. Try again."
                } finally {
                    busy.value = false
                }
            }
        }

        private companion object {
            const val STOP_TIMEOUT = 5_000L
        }
    }
