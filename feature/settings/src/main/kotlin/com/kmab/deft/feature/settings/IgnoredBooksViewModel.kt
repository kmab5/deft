package com.kmab.deft.feature.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kmab.deft.core.database.model.BookListItem
import com.kmab.deft.library.scanner.BookRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class IgnoredBooksViewModel
    @Inject
    constructor(
        private val repository: BookRepository,
    ) : ViewModel() {
        val error = MutableStateFlow<String?>(null)
        val books =
            repository
                .observeIgnoredBooks()
                .map<List<BookListItem>, List<BookListItem>?> { it }
                .catch { failure ->
                    if (failure is CancellationException) throw failure
                    error.value = "Couldn't load ignored books. Go back and try again."
                    emit(emptyList())
                }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT), null)

        fun restore(bookId: Long) {
            viewModelScope.launch {
                try {
                    repository.setIgnored(bookId, false)
                } catch (cancelled: CancellationException) {
                    throw cancelled
                } catch (_: android.database.sqlite.SQLiteException) {
                    error.value = "Couldn't restore this book. Try again."
                }
            }
        }

        private companion object {
            const val STOP_TIMEOUT = 5_000L
        }
    }
