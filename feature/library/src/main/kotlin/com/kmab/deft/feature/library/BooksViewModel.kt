package com.kmab.deft.feature.library

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kmab.deft.core.database.model.BookListItem
import com.kmab.deft.core.datastore.LibrarySettings
import com.kmab.deft.core.datastore.LibrarySettingsRepository
import com.kmab.deft.core.model.BookDisplaySize
import com.kmab.deft.core.model.BookFormat
import com.kmab.deft.core.model.BookSort
import com.kmab.deft.core.model.BookState
import com.kmab.deft.core.model.BookTrackingSummary
import com.kmab.deft.library.organization.CategorySummary
import com.kmab.deft.library.organization.OrganizationRepository
import com.kmab.deft.library.scanner.BookRepository
import com.kmab.deft.library.scanner.LibraryFolderRepository
import com.kmab.deft.library.scanner.ScanReport
import com.kmab.deft.library.scanner.ScanScheduler
import com.kmab.deft.library.scanner.ScanStatusObserver
import com.kmab.deft.tracking.ReadThroughRepository
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
    val reverseSort: Boolean = false,
    val gridSize: BookDisplaySize = BookDisplaySize.MEDIUM,
    val listSize: BookDisplaySize = BookDisplaySize.MEDIUM,
    val hasScanDetails: Boolean = false,
    val continueReading: BookListItem? = null,
    val advancedSearch: Boolean = false,
    val stateFilter: Set<BookState> = BookState.entries.toSet(),
    val favoritesOnly: Boolean = false,
    val minimumRating: Double? = null,
    val tracking: Map<Long, BookTrackingSummary> = emptyMap(),
    val categories: List<CategorySummary> = emptyList(),
    val categoryFilter: Long? = null,
)

private data class BookQuery(
    val settings: LibrarySettings,
    val formats: Set<BookFormat>,
)

private data class BookResult(
    val query: BookQuery,
    val books: List<BookListItem>,
    val error: String? = null,
    val tracking: Map<Long, BookTrackingSummary> = emptyMap(),
    val categories: List<CategorySummary> = emptyList(),
    val categoryFilter: Long? = null,
)

@OptIn(ExperimentalCoroutinesApi::class)
@Suppress("TooManyFunctions") // One persistence action for each independent library preference.
@HiltViewModel
class BooksViewModel
    @Suppress("LongParameterList") // Hilt supplies independent library data and scan services.
    @Inject
    constructor(
        repository: BookRepository,
        private val settings: LibrarySettingsRepository,
        folders: LibraryFolderRepository,
        private val scheduler: ScanScheduler,
        scans: ScanStatusObserver,
        history: ReadThroughRepository,
        organization: OrganizationRepository,
    ) : ViewModel() {
        private val error = MutableStateFlow<String?>(null)
        private val queries = settings.settings.map { preferences -> BookQuery(preferences, preferences.formatFilter) }
        private val books =
            queries
                .flatMapLatest { query ->
                    val list =
                        if (query.formats.isEmpty()) {
                            flowOf(
                                emptyList(),
                            )
                        } else {
                            repository.observeBooks(
                                query.settings.showMissing,
                                query.formats,
                                if (query.settings.sort == BookSort.RATING) BookSort.TITLE else query.settings.sort,
                                query.settings.reverseSort,
                            )
                        }
                    combine(list, history.observeAll(), organization.observe()) { items, snapshots, organized ->
                        val category = organized.categories.find { it.category.id == query.settings.categoryFilter }
                        val memberIds = category?.books?.map { it.book.book.id }?.toSet()
                        BookResult(
                            query,
                            filterTrackedBooks(items, snapshots, query.settings).filter {
                                memberIds == null || it.id in memberIds
                            },
                            tracking = snapshots,
                            categories = organized.categories,
                            categoryFilter = category?.category?.id,
                        )
                    }.catch { failure ->
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
        private val continueReading =
            repository.observeContinueReading().catch { failure ->
                if (failure is CancellationException) throw failure
                error.value = "Couldn't load your last reading position. Open the book to try again."
                emit(null)
            }
        val uiState =
            combine(books, folders.folders, scans.status, error, continueReading) {
                result,
                roots,
                scan,
                operationError,
                recent,
                ->
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
                    result.query.settings.reverseSort,
                    result.query.settings.gridSize,
                    result.query.settings.listSize,
                    hasScanDetails = scan.report != null || (scan.isFinished && scan.workId != null),
                    continueReading = recent,
                    advancedSearch = result.query.settings.advancedSearch,
                    stateFilter = result.query.settings.stateFilter,
                    favoritesOnly = result.query.settings.favoritesOnly,
                    minimumRating = result.query.settings.minimumRating,
                    tracking = result.tracking,
                    categories = result.categories,
                    categoryFilter = result.categoryFilter,
                )
            }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT), BooksUiState())

        fun setStateFilter(
            value: BookState,
            enabled: Boolean,
        ) = persist { settings.setStateFilter(value, enabled) }

        fun setFavoritesOnly(value: Boolean) = persist { settings.setFavoritesOnly(value) }

        fun setMinimumRating(value: Double?) = persist { settings.setMinimumRating(value) }

        fun setCategoryFilter(value: Long?) = persist { settings.setCategoryFilter(value) }

        fun setGridView(value: Boolean) = persist { settings.setGridView(value) }

        fun setReverseSort(value: Boolean) = persist { settings.setReverseSort(value) }

        fun setGridSize(value: BookDisplaySize) = persist { settings.setGridSize(value) }

        fun setListSize(value: BookDisplaySize) = persist { settings.setListSize(value) }

        fun setSort(value: BookSort) = persist { settings.setSort(value) }

        fun setShowMissing(value: Boolean) = persist { settings.setShowMissing(value) }

        fun setFormat(
            format: BookFormat,
            enabled: Boolean,
        ) = persist { settings.setFormatFilter(format, enabled) }

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
            const val STOP_TIMEOUT = 5_000L
        }
    }
