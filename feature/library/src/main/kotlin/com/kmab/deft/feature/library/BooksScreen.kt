package com.kmab.deft.feature.library

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Switch
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
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import coil3.ImageLoader
import com.kmab.deft.core.designsystem.DeftButton
import com.kmab.deft.core.designsystem.DeftButtonStyle
import com.kmab.deft.core.designsystem.DeftShapes
import com.kmab.deft.core.designsystem.DeftSheet
import com.kmab.deft.core.designsystem.DeftSnackbar
import com.kmab.deft.core.designsystem.DeftTypography
import com.kmab.deft.core.designsystem.FilterChip
import com.kmab.deft.core.designsystem.LocalDeftColors
import com.kmab.deft.core.designsystem.icons.DeftIcons
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
                    DropdownMenuItem(text = { Text("settings", style = DeftTypography.label) }, onClick = {
                        menu = false
                        onSettings()
                    })
                }
            }
        }
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            val active =
                (if (state.formatFilter.size != BookFormat.entries.size) 1 else 0) +
                    (if (state.showMissing) 1 else 0)
            LibraryControl(
                if (active == 0) "filters" else "filters · $active",
                DeftIcons.Filter,
                Modifier.weight(1f),
            ) { sheet = "filters" }
            LibraryControl("sort", null, Modifier.weight(1f)) { sheet = "sort" }
            LibraryControl(
                "display",
                if (state.gridView) LibraryIcons.Grid else LibraryIcons.List,
                Modifier.weight(1f),
            ) {
                sheet = "display"
            }
        }
        Row(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 4.dp)) {
            Text(
                state.sort.label() + " · " + state.sort.orderLabel(state.reverseSort),
                style = DeftTypography.cardTitle,
                color = c.muted,
            )
        }
        ScanFeedback(state, onRescan, onAddFolder)
        PullToRefreshBox(isRefreshing = state.isScanning, onRefresh = onRescan, modifier = Modifier.weight(1f)) {
            BooksCollection(state, loader, onAddFolder, onRescan, onSettings) {
                BookFormat.entries.forEach { onFormatChange(it, true) }
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
private fun LibraryControl(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector?,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    val c = LocalDeftColors.current
    Row(
        modifier
            .background(c.surface, DeftShapes.small)
            .heightIn(min = 48.dp)
            .clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
    ) {
        if (icon != null) {
            Icon(icon, null, Modifier.size(16.dp), tint = c.muted)
            Spacer(Modifier.width(6.dp))
        }
        Text(label, style = DeftTypography.label, color = c.text)
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun BooksFilters(
    state: BooksUiState,
    onDismiss: () -> Unit,
    onShowMissingChange: (Boolean) -> Unit,
    onFormatChange: (BookFormat, Boolean) -> Unit,
) {
    val c = LocalDeftColors.current
    DeftSheet(onDismiss, Modifier.testTag("books_sheet")) {
        Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("filters", style = DeftTypography.headline, color = c.text)
            Text("file type", style = DeftTypography.label, color = c.muted)
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                BookFormat.entries.forEach { format ->
                    FilterChip(
                        format.displayName.lowercase(),
                        format in state.formatFilter,
                        { onFormatChange(format, format !in state.formatFilter) },
                    )
                }
            }
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text("show missing", style = DeftTypography.body, color = c.text, modifier = Modifier.weight(1f))
                Switch(
                    state.showMissing,
                    onShowMissingChange,
                    Modifier.semantics { contentDescription = "show missing" },
                )
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                DeftButton("reset", {
                    BookFormat.entries.forEach { onFormatChange(it, true) }
                    onShowMissingChange(false)
                }, Modifier.weight(1f), style = DeftButtonStyle.Secondary)
                DeftButton("done", onDismiss, Modifier.weight(1f))
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun BooksSort(
    state: BooksUiState,
    onDismiss: () -> Unit,
    onSortChange: (BookSort) -> Unit,
    onReverseSortChange: (Boolean) -> Unit,
) {
    val c = LocalDeftColors.current
    DeftSheet(onDismiss, Modifier.testTag("books_sheet")) {
        Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("sort books", style = DeftTypography.headline, color = c.text)
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                BookSort.entries.forEach { sort ->
                    FilterChip(sort.label(), state.sort == sort, { onSortChange(sort) })
                }
            }
            Text("order", style = DeftTypography.label, color = c.muted)
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                listOf(false, true).forEach { reverse ->
                    FilterChip(
                        state.sort.orderLabel(reverse),
                        state.reverseSort == reverse,
                        { onReverseSortChange(reverse) },
                    )
                }
            }
            if (state.sort == BookSort.LAST_READ) {
                Text("Books with no reading history stay at the end.", style = DeftTypography.body, color = c.muted)
            }
            DeftButton("done", onDismiss, Modifier.fillMaxWidth())
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun BooksDisplay(
    state: BooksUiState,
    onDismiss: () -> Unit,
    onGridViewChange: (Boolean) -> Unit,
    onGridSizeChange: (BookDisplaySize) -> Unit,
    onListSizeChange: (BookDisplaySize) -> Unit,
) {
    val c = LocalDeftColors.current
    DeftSheet(onDismiss, Modifier.testTag("books_sheet")) {
        Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("display", style = DeftTypography.headline, color = c.text)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip("grid", state.gridView, { onGridViewChange(true) }, Modifier.weight(1f))
                FilterChip("list", !state.gridView, { onGridViewChange(false) }, Modifier.weight(1f))
            }
            Text(if (state.gridView) "grid size" else "list size", style = DeftTypography.label, color = c.muted)
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                BookDisplaySize.entries.forEach { size ->
                    FilterChip(
                        size.name.lowercase(java.util.Locale.ROOT),
                        size == if (state.gridView) state.gridSize else state.listSize,
                        {
                            if (state.gridView) onGridSizeChange(size) else onListSizeChange(size)
                        },
                    )
                }
            }
            Text("Grid and list remember their own size.", style = DeftTypography.body, color = c.muted)
            DeftButton("done", onDismiss, Modifier.fillMaxWidth())
        }
    }
}

@Composable
private fun BooksCollection(
    state: BooksUiState,
    loader: ImageLoader,
    onAddFolder: () -> Unit,
    onRescan: () -> Unit,
    onSettings: () -> Unit,
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
                items(state.books, key = { it.id }) { book -> BookCard(book, loader, size = state.gridSize) }
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
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        BookRow(book, loader, size = state.listSize)
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

@Composable
private fun ScanFeedback(
    state: BooksUiState,
    onRescan: () -> Unit,
    onSettings: () -> Unit,
) {
    val c = LocalDeftColors.current
    if (state.error != null) DeftSnackbar(state.error, Modifier.padding(horizontal = 20.dp), "retry", onRescan)
    val summary =
        if (state.isScanning) {
            state.scanMessage ?: "Scanning folders."
        } else {
            state.scanMessage ?: state.scanReport?.summary()
        }
    if (summary != null) {
        Row(
            Modifier.padding(horizontal = 20.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            if (state.isScanning) {
                CircularProgressIndicator(Modifier.size(16.dp), color = c.accent, strokeWidth = 2.dp)
            }
            Text(summary, style = DeftTypography.cardTitle, color = c.muted)
        }
    }
    if (!state.isScanning && state.scanReport != null) {
        DeftButton(
            if (state.scanReport.unavailableRoots.isNotEmpty()) "library settings" else "scan details",
            onSettings,
            Modifier.padding(horizontal = 20.dp),
            style = DeftButtonStyle.Text,
        )
    }
}
