package com.kmab.deft.feature.library

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kmab.deft.core.database.model.BookListItem
import com.kmab.deft.core.datastore.LibrarySettings
import com.kmab.deft.core.datastore.LibrarySettingsRepository
import com.kmab.deft.core.model.BookFormat
import com.kmab.deft.core.model.BookSort
import com.kmab.deft.library.scanner.BookRepository
import com.kmab.deft.library.scanner.LibraryFolderRepository
import com.kmab.deft.library.scanner.ScanReport
import com.kmab.deft.library.scanner.ScanScheduler
import com.kmab.deft.library.scanner.ScanStatusObserver
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.io.IOException
import javax.inject.Inject

data class BooksUiState(
    val books: List<BookListItem> = emptyList(),
    val gridView: Boolean = true,
    val sort: BookSort = BookSort.TITLE,
    val showMissing: Boolean = false,
    val formatFilter: Set<BookFormat> = BookFormat.entries.toSet(),
    val isScanning: Boolean = false,
    val hasFolders: Boolean = false,
    val isLoading: Boolean = true,
    val scanReport: ScanReport? = null,
    val scanMessage: String? = null,
    val error: String? = null,
)

private data class BookQuery(
    val settings: LibrarySettings,
    val formats: Set<BookFormat>,
)

private data class BookResult(
    val query: BookQuery,
    val books: List<BookListItem>,
    val error: String? = null,
)

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class BooksViewModel
    @Inject
    constructor(
        repository: BookRepository,
        private val settings: LibrarySettingsRepository,
        folders: LibraryFolderRepository,
        private val scheduler: ScanScheduler,
        scans: ScanStatusObserver,
        private val savedState: SavedStateHandle,
    ) : ViewModel() {
        private val error = MutableStateFlow<String?>(null)
        private val formats = savedState.getStateFlow(FORMATS_KEY, BookFormat.entries.map { it.name })
        private val queries =
            combine(settings.settings, formats) { preferences, names ->
                BookQuery(preferences, names.mapNotNull { name -> BookFormat.entries.find { it.name == name } }.toSet())
            }
        private val books =
            queries
                .flatMapLatest { query ->
                    val list =
                        if (query.formats.isEmpty()) {
                            flowOf(
                                emptyList(),
                            )
                        } else {
                            repository.observeBooks(query.settings.showMissing, query.formats, query.settings.sort)
                        }
                    list.map { BookResult(query, it) }.catch { failure ->
                        if (failure is CancellationException) throw failure
                        emit(BookResult(query, emptyList(), "Couldn't load books. Try rescan."))
                    }
                }.catch { failure ->
                    if (failure is CancellationException) throw failure
                    emit(
                        BookResult(
                            BookQuery(LibrarySettings(), BookFormat.entries.toSet()),
                            emptyList(),
                            "Couldn't load library settings. Restart Deft to try again.",
                        ),
                    )
                }
        val uiState =
            combine(books, folders.folders, scans.status, error) { result, roots, scan, operationError ->
                BooksUiState(
                    result.books,
                    result.query.settings.gridView,
                    result.query.settings.sort,
                    result.query.settings.showMissing,
                    result.query.formats,
                    scan.isScanning,
                    roots.isNotEmpty(),
                    false,
                    scan.report,
                    scan.message,
                    operationError ?: result.error,
                )
            }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT), BooksUiState())

        fun setGridView(value: Boolean) = persist { settings.setGridView(value) }

        fun setSort(value: BookSort) = persist { settings.setSort(value) }

        fun setShowMissing(value: Boolean) = persist { settings.setShowMissing(value) }

        fun setFormat(
            format: BookFormat,
            enabled: Boolean,
        ) {
            val current = formats.value.toSet()
            savedState[FORMATS_KEY] = (if (enabled) current + format.name else current - format.name).toList()
        }

        fun rescan() {
            try {
                scheduler.scanNow()
            } catch (_: IllegalStateException) {
                error.value = "Couldn't start scan. Try again."
            }
        }

        fun dismissError() {
            error.value = null
        }

        private fun persist(write: suspend () -> Unit) {
            viewModelScope.launch {
                try {
                    write()
                } catch (
                    cancelled: CancellationException,
                ) {
                    throw cancelled
                } catch (_: IOException) {
                    error.value =
                        "Couldn't save library preferences. Try again."
                } catch (
                    _: IllegalStateException,
                ) {
                    error.value = "Couldn't save library preferences. Try again."
                }
            }
        }

        private companion object {
            const val FORMATS_KEY = "book_format_filter"
            const val STOP_TIMEOUT = 5_000L
        }
    }
