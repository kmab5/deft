package com.kmab.deft.feature.library

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.heading
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
) {
    val c = LocalDeftColors.current
    var menu by remember { mutableStateOf(false) }
    var filters by rememberSaveable { mutableStateOf(false) }
    val context = LocalContext.current.applicationContext
    val loader = remember(context) { ImageLoader.Builder(context).build() }
    DisposableEffect(loader) { onDispose { loader.shutdown() } }
    Column(Modifier.fillMaxSize().background(c.bg).testTag("books_screen")) {
        Row(Modifier.fillMaxWidth().heightIn(min = 56.dp).padding(start = 20.dp, end = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            Text("books", style = DeftTypography.headline, color = c.text, modifier = Modifier.semantics { heading() })
            Spacer(Modifier.width(10.dp))
            Text(state.books.size.toString(), style = DeftTypography.mono, color = c.muted)
            Spacer(Modifier.weight(1f))
            Box {
                IconButton(onClick = { menu = true }) { Icon(LibraryIcons.More, "more options", tint = c.muted) }
                DropdownMenu(menu, { menu = false }, shape = DeftShapes.card, containerColor = c.surface,
                    tonalElevation = 0.dp, shadowElevation = 0.dp, border = BorderStroke(1.dp, c.line)) {
                    DropdownMenuItem(text = { Text("settings", style = DeftTypography.label) }, onClick = { menu = false; onSettings() })
                }
            }
        }
        Row(Modifier.fillMaxWidth().padding(horizontal = 20.dp), verticalAlignment = Alignment.CenterVertically) {
            val active = if (state.formatFilter.size != BookFormat.entries.size) 1 else 0
            Row(Modifier.weight(1f).heightIn(min = 48.dp).clickable(role = Role.Button) { filters = true },
                verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Icon(DeftIcons.Filter, null, Modifier.size(16.dp), tint = c.muted)
                Text(if (active == 0) "all books" else "all books · $active", style = DeftTypography.mono, color = c.text)
            }
            Box(Modifier.heightIn(min = 48.dp).clickable(role = Role.Button) { filters = true }.padding(horizontal = 8.dp), contentAlignment = Alignment.Center) {
                Text(state.sort.label() + " ▾", style = DeftTypography.mono, color = c.muted)
            }
            IconButton(onClick = { onGridViewChange(!state.gridView) }) {
                Icon(if (state.gridView) LibraryIcons.List else LibraryIcons.Grid,
                    if (state.gridView) "switch to list view" else "switch to grid view", tint = c.muted)
            }
        }
        if (state.error != null) DeftSnackbar(state.error, Modifier.padding(horizontal = 20.dp), "retry", onRescan)
        if (state.isScanning || state.scanMessage != null) {
            Row(Modifier.padding(horizontal = 20.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (state.isScanning) CircularProgressIndicator(Modifier.size(16.dp), color = c.accent, strokeWidth = 2.dp)
                Text(state.scanMessage ?: "Scanning folders.", style = DeftTypography.mono, color = c.muted)
            }
        } else state.scanReport?.let { report ->
            Text("added ${report.added} · missing ${report.markedMissing} · ${report.errors} errors",
                Modifier.padding(horizontal = 20.dp, vertical = 8.dp), style = DeftTypography.mono, color = c.muted)
        }
        if (!state.isScanning && state.scanReport?.unavailableRoots?.isNotEmpty() == true) {
            DeftButton("library settings", onAddFolder, Modifier.padding(horizontal = 20.dp), style = DeftButtonStyle.Text)
        }
        PullToRefreshBox(isRefreshing = state.isScanning, onRefresh = onRescan, modifier = Modifier.weight(1f)) {
            when {
                state.isLoading -> Column(Modifier.fillMaxSize().padding(20.dp), verticalArrangement = Arrangement.Center) {
                    CircularProgressIndicator(color = c.accent)
                    Text("loading books…", style = DeftTypography.mono, color = c.muted)
                }
                state.books.isEmpty() -> EmptyBooks(state, onAddFolder, onRescan, onSettings, {
                    BookFormat.entries.forEach { onFormatChange(it, true) }
                })
                state.gridView -> LazyVerticalGrid(GridCells.Adaptive(110.dp), Modifier.fillMaxSize().testTag("book_grid"),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(start = 20.dp, end = 20.dp, top = 12.dp, bottom = 24.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp), verticalArrangement = Arrangement.spacedBy(22.dp)) {
                    items(state.books, key = { it.id }) { book -> BookCard(book, loader) }
                }
                else -> LazyColumn(Modifier.fillMaxSize().testTag("book_list"),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 20.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    items(state.books, key = { it.id }) { book -> BookRow(book, loader) }
                }
            }
        }
    }
    if (filters) DeftSheet({ filters = false }) {
        Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("filter & sort", style = DeftTypography.headline, color = c.text)
            Text("format", style = DeftTypography.monoSmall, color = c.muted)
            BookFormat.entries.chunked(3).forEach { row ->
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    row.forEach { format -> FilterChip(format.displayName.lowercase(), format in state.formatFilter,
                        { onFormatChange(format, format !in state.formatFilter) }, Modifier.weight(1f)) }
                }
            }
            Text("sort", style = DeftTypography.monoSmall, color = c.muted)
            BookSort.entries.forEach { sort -> FilterChip(sort.label(), state.sort == sort, { onSortChange(sort) }) }
            FilterChip("show missing", state.showMissing, { onShowMissingChange(!state.showMissing) })
            DeftButton("done", { filters = false })
        }
    }
}

@Composable
private fun EmptyBooks(state: BooksUiState, onAdd: () -> Unit, onRescan: () -> Unit, onSettings: () -> Unit, onClear: () -> Unit) {
    val c = LocalDeftColors.current
    LazyColumn(Modifier.fillMaxSize()) {
        item {
            Column(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 64.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                val filtered = state.hasFolders && state.formatFilter.size != BookFormat.entries.size
                Text(if (!state.hasFolders) "no books yet." else if (filtered) "no matching books." else "nothing here.",
                    style = DeftTypography.headline, color = c.text)
                Text(if (!state.hasFolders) "Point Deft at a folder and it will find them." else if (filtered)
                    "No books match these filters." else "No supported books in these folders. Check the file types in settings.",
                    style = DeftTypography.body, color = c.muted)
                when {
                    !state.hasFolders -> DeftButton("add a folder", onAdd)
                    filtered -> DeftButton("clear filters", onClear)
                    else -> Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        DeftButton("rescan", onRescan)
                        DeftButton("settings", onSettings, style = DeftButtonStyle.Secondary)
                    }
                }
            }
        }
    }
}
internal fun BookSort.label(): String = when (this) { BookSort.TITLE -> "title"; BookSort.AUTHOR -> "author"; BookSort.DATE_ADDED -> "date added" }
