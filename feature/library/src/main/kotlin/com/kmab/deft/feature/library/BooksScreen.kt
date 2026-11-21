package com.kmab.deft.feature.library

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import coil3.ImageLoader
import com.kmab.deft.core.designsystem.DeftButton
import com.kmab.deft.core.designsystem.DeftButtonStyle
import com.kmab.deft.core.designsystem.DeftMotion
import com.kmab.deft.core.designsystem.DeftShapes
import com.kmab.deft.core.designsystem.DeftTypography
import com.kmab.deft.core.designsystem.LocalDeftColors
import com.kmab.deft.core.designsystem.icons.DeftIcons
import com.kmab.deft.core.model.BookDisplaySize
import com.kmab.deft.core.model.BookFormat
import com.kmab.deft.core.model.BookSort

@OptIn(ExperimentalMaterial3Api::class)
@Suppress("CyclomaticComplexMethod") // Navigation/search/layout choices are independent screen controls.
@Composable
fun BooksScreen(
    state: BooksUiState,
    onSettings: () -> Unit,
    onAddFolder: () -> Unit,
    onRescan: () -> Unit,
    onGridViewChange: (Boolean) -> Unit,
    onSortChange: (BookSort) -> Unit,
    onShowMissingChange: (Boolean) -> Unit,
    onFormatChange: (BookFormat, Boolean) -> Unit,
    onReverseSortChange: (Boolean) -> Unit = {},
    onGridSizeChange: (BookDisplaySize) -> Unit = {},
    onListSizeChange: (BookDisplaySize) -> Unit = {},
    onScanDetails: () -> Unit = {},
    onOpenBook: (Long) -> Unit = {},
    onContinueReading: (Long) -> Unit = {},
    onStateFilter: (com.kmab.deft.core.model.BookState, Boolean) -> Unit = { _, _ -> },
    onFavoritesOnly: (Boolean) -> Unit = {},
    onMinimumRating: (Double?) -> Unit = {},
    onCategoryFilter: (Long?) -> Unit = {},
    onAddToCategory: (Set<Long>) -> Unit = {},
    selectionResetKey: Int = 0,
) {
    val c = LocalDeftColors.current
    var menu by remember { mutableStateOf(false) }
    var sheet by rememberSaveable { mutableStateOf<String?>(null) }
    var searching by rememberSaveable { mutableStateOf(false) }
    var query by rememberSaveable { mutableStateOf("") }
    var previousQuery by remember { mutableStateOf(query) }
    var searchOptions by remember {
        mutableStateOf(
            com.kmab.deft.core.model
                .SearchOptions(),
        )
    }
    val activeOptions = searchOptions.enabledWhen(state.advancedSearch)
    val searchResult =
        remember(state.books, query, activeOptions) { runCatching { state.books.searchBooks(query, activeOptions) } }
    val visibleBooks = searchResult.getOrDefault(emptyList())
    var selected by rememberSaveable { mutableStateOf(emptyList<Long>()) }
    var handledReset by rememberSaveable { mutableStateOf(selectionResetKey) }
    LaunchedEffect(selectionResetKey) {
        if (selectionResetKey != handledReset) {
            selected = emptyList()
            handledReset = selectionResetKey
        }
    }
    val visibleIds = remember(visibleBooks) { visibleBooks.map { it.id }.toSet() }
    LaunchedEffect(visibleIds, state.isLoading, state.error, searchResult.isSuccess) {
        if (!state.isLoading && state.error == null && searchResult.isSuccess) {
            selected = selected.filter { it in visibleIds }
        }
    }
    BackHandler(selected.isNotEmpty()) { selected = emptyList() }
    val toggle: (Long) -> Unit = { id -> selected = if (id in selected) selected - id else selected + id }
    val open: (Long) -> Unit = { id -> if (selected.isEmpty()) onOpenBook(id) else toggle(id) }
    val scroll = rememberLibraryScrollState(state.gridView)
    LaunchedEffect(query) {
        if (query != previousQuery) {
            previousQuery = query
            scroll.scrollToTop()
        }
    }
    val context = LocalContext.current.applicationContext
    val loader = remember(context) { ImageLoader.Builder(context).diskCache(null).build() }
    DisposableEffect(loader) { onDispose { loader.shutdown() } }
    Column(Modifier.fillMaxSize().background(c.bg).testTag("books_screen")) {
        Row(
            Modifier.fillMaxWidth().heightIn(min = 56.dp).padding(start = 20.dp, end = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("books", style = DeftTypography.headline, color = c.text, modifier = Modifier.semantics { heading() })
            Spacer(Modifier.width(10.dp))
            Text(visibleBooks.size.toString(), style = DeftTypography.cardTitle, color = c.muted)
            Spacer(Modifier.weight(1f))
            IconButton(onClick = {
                searching = !searching
                if (!searching) query = ""
            }) {
                Icon(DeftIcons.Search, "search books", tint = if (searching) c.accentText else c.muted)
            }
            Box {
                IconButton(onClick = { menu = true }) { Icon(LibraryIcons.More, "more options", tint = c.muted) }
                DropdownMenu(
                    menu,
                    { menu = false },
                    shape = DeftShapes.card,
                    containerColor = c.surface,
                    tonalElevation = 0.dp,
                    shadowElevation = 0.dp,
                    border = BorderStroke(1.dp, c.line),
                ) {
                    if (state.scanReport != null || state.hasScanDetails) {
                        DropdownMenuItem(
                            text = { Text("last scan", style = DeftTypography.label) },
                            leadingIcon = { Icon(LibraryIcons.History, null) },
                            onClick = {
                                menu = false
                                onScanDetails()
                            },
                        )
                    }
                    DropdownMenuItem(leadingIcon = {
                        Icon(LibraryIcons.Settings, null)
                    }, text = { Text("settings", style = DeftTypography.label) }, onClick = {
                        menu = false
                        onSettings()
                    })
                }
            }
        }
        AnimatedVisibility(searching) {
            Column {
                LibrarySearch(query, { query = it }, {
                    searching = false
                    query = ""
                })
                LibrarySearchFilters(state.advancedSearch, searchOptions, { searchOptions = it }, searchResult)
            }
        }
        AnimatedVisibility(state.continueReading != null && scroll.showHeader && selected.isEmpty()) {
            state.continueReading?.let { book -> ContinueReadingCard(book, loader) { onContinueReading(book.id) } }
        }
        BooksToolbar(state, onFilters = { sheet = "filters" }, onSort = { sheet = "sort" }, onDisplay = {
            sheet =
                "display"
        })
        AnimatedVisibility(selected.isNotEmpty()) {
            BooksSelectionBar(selected.size, { selected = emptyList() }, { selected = visibleIds.toList() }) {
                onAddToCategory(selected.toSet())
            }
        }
        AnimatedVisibility(
            visible = state.isScanning,
            enter = fadeIn(tween(DeftMotion.SHORT)) + expandVertically(tween(DeftMotion.SHORT)),
            exit = fadeOut(tween(DeftMotion.SHORT)) + shrinkVertically(tween(DeftMotion.SHORT)),
        ) {
            Row(
                Modifier.padding(horizontal = 20.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                CircularProgressIndicator(Modifier.size(14.dp), color = c.accent, strokeWidth = 2.dp)
                Text(state.scanMessage ?: "scanning folders…", style = DeftTypography.cardTitle, color = c.muted)
            }
        }
        PullToRefreshBox(
            isRefreshing = state.isScanning,
            onRefresh = onRescan,
            modifier = Modifier.weight(1f).nestedScroll(scroll.connection),
        ) {
            Crossfade(
                targetState = BookLayout(state.gridView, state.gridSize, state.listSize),
                animationSpec = tween(DeftMotion.SHORT),
                label = "book layout",
            ) { layout ->
                BooksCollection(
                    state.copy(
                        books = visibleBooks,
                        gridView = layout.grid,
                        gridSize = layout.gridSize,
                        listSize = layout.listSize,
                    ),
                    loader,
                    scroll.list,
                    scroll.grid,
                    query.isNotBlank(),
                    onAddFolder,
                    onRescan,
                    onSettings,
                    open,
                    toggle,
                    selected.toSet(),
                ) {
                    if (query.isNotBlank()) {
                        query = ""
                    } else {
                        BookFormat.entries.forEach { onFormatChange(it, true) }
                        com.kmab.deft.core.model.BookState.entries
                            .forEach { onStateFilter(it, true) }
                        onFavoritesOnly(false)
                        onMinimumRating(null)
                        onCategoryFilter(null)
                    }
                }
            }
            LibraryScrollOverlay(scroll, state.continueReading.takeIf { selected.isEmpty() }, onContinueReading)
        }
    }
    when (sheet) {
        "filters" ->
            BooksFilters(state, {
                sheet = null
            }, onShowMissingChange, onFormatChange, onStateFilter, onFavoritesOnly, onMinimumRating, onCategoryFilter)
        "sort" -> BooksSort(state, { sheet = null }, onSortChange, onReverseSortChange)
        "display" -> BooksDisplay(state, { sheet = null }, onGridViewChange, onGridSizeChange, onListSizeChange)
    }
}

@Composable
private fun BooksCollection(
    state: BooksUiState,
    loader: ImageLoader,
    listState: LazyListState,
    gridState: LazyGridState,
    searching: Boolean,
    onAddFolder: () -> Unit,
    onRescan: () -> Unit,
    onSettings: () -> Unit,
    onOpenBook: (Long) -> Unit,
    onSelectBook: (Long) -> Unit,
    selected: Set<Long>,
    onClear: () -> Unit,
) {
    val c = LocalDeftColors.current
    when {
        state.isLoading ->
            Column(Modifier.fillMaxSize().padding(20.dp), verticalArrangement = Arrangement.Center) {
                CircularProgressIndicator(color = c.accent)
                Text("loading books…", style = DeftTypography.cardTitle, color = c.muted)
            }
        state.books.isEmpty() -> EmptyBooks(state, searching, onAddFolder, onRescan, onSettings, onClear)
        state.gridView ->
            LazyVerticalGrid(
                GridCells.Adaptive(
                    state.gridSize.gridMinimumWidth() * LocalDensity.current.fontScale.coerceAtLeast(1f),
                ),
                Modifier.fillMaxSize().testTag("book_grid"),
                state = gridState,
                contentPadding =
                    androidx.compose.foundation.layout.PaddingValues(
                        start = 16.dp,
                        end = 16.dp,
                        top = 12.dp,
                        bottom = 144.dp,
                    ),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                items(state.books, key = {
                    it.id
                }) { book ->
                    BookCard(
                        book,
                        loader,
                        Modifier.animateItem(fadeInSpec = null, placementSpec = tween(DeftMotion.MEDIUM)),
                        size = state.gridSize,
                        tracking = state.tracking[book.id],
                        onClick = { onOpenBook(book.id) },
                        onLongClick = { onSelectBook(book.id) },
                        selected = book.id in selected,
                    )
                }
            }
        else ->
            LazyColumn(
                Modifier.fillMaxSize().testTag("book_list"),
                state = listState,
                contentPadding =
                    androidx.compose.foundation.layout.PaddingValues(
                        start = 16.dp,
                        end = 16.dp,
                        top = 12.dp,
                        bottom = 144.dp,
                    ),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                items(state.books, key = { it.id }) { book ->
                    BookRow(
                        book,
                        loader,
                        Modifier.animateItem(fadeInSpec = null, placementSpec = tween(DeftMotion.MEDIUM)),
                        size = state.listSize,
                        tracking = state.tracking[book.id],
                        onClick = { onOpenBook(book.id) },
                        onLongClick = { onSelectBook(book.id) },
                        selected = book.id in selected,
                    )
                }
            }
    }
}

@Composable
private fun EmptyBooks(
    state: BooksUiState,
    searching: Boolean,
    onAdd: () -> Unit,
    onRescan: () -> Unit,
    onSettings: () -> Unit,
    onClear: () -> Unit,
) {
    val c = LocalDeftColors.current
    LazyColumn(Modifier.fillMaxSize()) {
        item {
            Column(
                Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 64.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                val filtered = state.hasReadingFilters()
                Text(
                    if (searching) {
                        "no matching books."
                    } else if (!state.hasFolders) {
                        "no books yet."
                    } else if (filtered) {
                        "no matching books."
                    } else {
                        "nothing here."
                    },
                    style = DeftTypography.headline,
                    color = c.text,
                )
                Text(
                    if (searching) {
                        "Try another title or author, or clear your search."
                    } else if (!state.hasFolders) {
                        "Point Deft at a folder and it will find them."
                    } else if (filtered) {
                        "No books match these filters."
                    } else {
                        "No supported books in these folders. Check the file types in settings."
                    },
                    style = DeftTypography.body,
                    color = c.muted,
                )
                when {
                    searching -> DeftButton("clear search", onClear)
                    !state.hasFolders -> DeftButton("add a folder", onAdd)
                    filtered -> DeftButton("clear filters", onClear)
                    else ->
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            DeftButton("rescan", onRescan)
                            DeftButton("settings", onSettings, style = DeftButtonStyle.Secondary)
                        }
                }
            }
        }
    }
}

internal fun BookSort.label(): String =
    when (this) {
        BookSort.TITLE -> "title"
        BookSort.AUTHOR -> "author"
        BookSort.DATE_ADDED -> "date added"
        BookSort.LAST_READ -> "last read"
        BookSort.RATING -> "rating"
    }

internal fun BookSort.orderLabel(reverse: Boolean): String =
    when (this) {
        BookSort.TITLE, BookSort.AUTHOR -> if (reverse) "z to a" else "a to z"
        BookSort.DATE_ADDED, BookSort.LAST_READ -> if (reverse) "oldest first" else "newest first"
        BookSort.RATING -> if (reverse) "lowest first" else "highest first"
    }

internal fun BookDisplaySize.gridMinimumWidth() =
    when (this) {
        BookDisplaySize.SMALL -> 72.dp
        BookDisplaySize.MEDIUM -> 108.dp
        BookDisplaySize.LARGE -> 152.dp
    }

private data class BookLayout(
    val grid: Boolean,
    val gridSize: BookDisplaySize,
    val listSize: BookDisplaySize,
)

private fun com.kmab.deft.core.model.SearchOptions.enabledWhen(enabled: Boolean) =
    if (enabled) {
        this
    } else {
        com.kmab.deft.core.model
            .SearchOptions()
    }

private fun BooksUiState.hasReadingFilters(): Boolean =
    hasFolders && (
        formatFilter.size != BookFormat.entries.size ||
            stateFilter.size != com.kmab.deft.core.model.BookState.entries.size || favoritesOnly ||
            minimumRating != null || categoryFilter != null
    )
