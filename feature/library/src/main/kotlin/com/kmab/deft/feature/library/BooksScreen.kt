package com.kmab.deft.feature.library

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
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
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
import com.kmab.deft.core.model.BookDisplaySize
import com.kmab.deft.core.model.BookFormat
import com.kmab.deft.core.model.BookSort

@OptIn(ExperimentalMaterial3Api::class)
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
) {
    val c = LocalDeftColors.current
    var menu by remember { mutableStateOf(false) }
    var sheet by rememberSaveable { mutableStateOf<String?>(null) }
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
            Text(state.books.size.toString(), style = DeftTypography.cardTitle, color = c.muted)
            Spacer(Modifier.weight(1f))
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
        AnimatedVisibility(state.continueReading != null) {
            state.continueReading?.let { book -> ContinueReadingCard(book, loader) { onContinueReading(book.id) } }
        }
        BooksToolbar(state, onFilters = { sheet = "filters" }, onSort = { sheet = "sort" }, onDisplay = {
            sheet =
                "display"
        })
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
        PullToRefreshBox(isRefreshing = state.isScanning, onRefresh = onRescan, modifier = Modifier.weight(1f)) {
            Crossfade(
                targetState = BookLayout(state.gridView, state.gridSize, state.listSize),
                animationSpec = tween(DeftMotion.SHORT),
                label = "book layout",
            ) { layout ->
                BooksCollection(
                    state.copy(gridView = layout.grid, gridSize = layout.gridSize, listSize = layout.listSize),
                    loader,
                    onAddFolder,
                    onRescan,
                    onSettings,
                    onOpenBook,
                ) {
                    BookFormat.entries.forEach { onFormatChange(it, true) }
                }
            }
        }
    }
    when (sheet) {
        "filters" -> BooksFilters(state, { sheet = null }, onShowMissingChange, onFormatChange)
        "sort" -> BooksSort(state, { sheet = null }, onSortChange, onReverseSortChange)
        "display" -> BooksDisplay(state, { sheet = null }, onGridViewChange, onGridSizeChange, onListSizeChange)
    }
}

@Composable
private fun BooksCollection(
    state: BooksUiState,
    loader: ImageLoader,
    onAddFolder: () -> Unit,
    onRescan: () -> Unit,
    onSettings: () -> Unit,
    onOpenBook: (Long) -> Unit,
    onClear: () -> Unit,
) {
    val c = LocalDeftColors.current
    when {
        state.isLoading ->
            Column(Modifier.fillMaxSize().padding(20.dp), verticalArrangement = Arrangement.Center) {
                CircularProgressIndicator(color = c.accent)
                Text("loading books…", style = DeftTypography.cardTitle, color = c.muted)
            }
        state.books.isEmpty() -> EmptyBooks(state, onAddFolder, onRescan, onSettings, onClear)
        state.gridView ->
            LazyVerticalGrid(
                GridCells.Adaptive(state.gridSize.gridMinimumWidth()),
                Modifier.fillMaxSize().testTag("book_grid"),
                contentPadding =
                    androidx.compose.foundation.layout.PaddingValues(
                        start = 20.dp,
                        end = 20.dp,
                        top = 12.dp,
                        bottom = 24.dp,
                    ),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp),
            ) {
                items(state.books, key = {
                    it.id
                }) { book ->
                    BookCard(
                        book,
                        loader,
                        Modifier.animateItem(fadeInSpec = null, placementSpec = tween(DeftMotion.MEDIUM)),
                        size = state.gridSize,
                        onClick = { onOpenBook(book.id) },
                    )
                }
            }
        else ->
            LazyColumn(
                Modifier.fillMaxSize().testTag("book_list"),
                contentPadding =
                    androidx.compose.foundation.layout.PaddingValues(
                        horizontal = 20.dp,
                        vertical = 12.dp,
                    ),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                items(state.books, key = { it.id }) { book ->
                    Column(
                        Modifier.animateItem(fadeInSpec = null, placementSpec = tween(DeftMotion.MEDIUM)),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        BookRow(book, loader, size = state.listSize, onClick = { onOpenBook(book.id) })
                        HorizontalDivider(color = c.line)
                    }
                }
            }
    }
}

@Composable
private fun EmptyBooks(
    state: BooksUiState,
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
                val filtered = state.hasFolders && state.formatFilter.size != BookFormat.entries.size
                Text(
                    if (!state.hasFolders) {
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
                    if (!state.hasFolders) {
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
    }

internal fun BookSort.orderLabel(reverse: Boolean): String =
    when (this) {
        BookSort.TITLE, BookSort.AUTHOR -> if (reverse) "z to a" else "a to z"
        BookSort.DATE_ADDED, BookSort.LAST_READ -> if (reverse) "oldest first" else "newest first"
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
