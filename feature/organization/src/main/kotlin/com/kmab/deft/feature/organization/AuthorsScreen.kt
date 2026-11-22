package com.kmab.deft.feature.organization

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.kmab.deft.core.designsystem.DeftButton
import com.kmab.deft.core.designsystem.DeftButtonStyle
import com.kmab.deft.core.designsystem.DeftDialog
import com.kmab.deft.core.designsystem.DeftTypography
import com.kmab.deft.core.designsystem.LocalDeftColors
import com.kmab.deft.core.model.BookState
import com.kmab.deft.library.organization.AuthorSummary
import com.kmab.deft.library.organization.OrganizedBook

@Composable
internal fun AuthorsScreen(
    state: OrganizationUiState,
    model: OrganizationViewModel? = null,
    open: (Long) -> Unit,
) {
    val authors =
        state.snapshot?.authors ?: return OrganizationEmpty(
            if (state.loadFailed) "Couldn't load authors." else "loading",
            !state.loadFailed,
        )
    var query by rememberSaveable { mutableStateOf("") }
    var filters by rememberSaveable { mutableStateOf(false) }
    val prefs = state.preferences
    val matches = browseAuthors(authors, prefs, query)
    LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        item { BrowseSearch(query, { query = it }, "find authors") }
        item {
            BrowseSort(
                prefs.authorSort,
                prefs.authorReverse,
                authorSortOptions,
                { model?.preference { setAuthorSort(it) } },
                { model?.preference { toggleAuthorReverse() } },
                listOf(prefs.authorState != null, prefs.authorRating != "all").count { it },
                { filters = true },
            )
        }
        if (matches.isEmpty()) item { OrganizationEmpty("No matching authors.") }
        items(matches, key = { it.author.id }) { author ->
            OrganizationPanel(Modifier.animateItem()) {
                OrganizationRow(author.author.name, "${author.stats.total} books · ${author.stats.finished} finished") {
                    open(author.author.id)
                }
                OrganizationProgress(author.stats, legend = false)
            }
        }
    }
    if (filters && model != null) AuthorFilters(prefs, model) { filters = false }
}

@Composable
internal fun AuthorDetail(
    author: AuthorSummary,
    authors: List<AuthorSummary>,
    model: OrganizationViewModel,
    close: () -> Unit,
    openBook: (Long) -> Unit,
) {
    var action by rememberSaveable(author.author.id) { mutableStateOf<String?>(null) }
    val closeSheet = { action = null }
    var sort by rememberSaveable(author.author.id) { mutableStateOf("title") }
    var reverse by rememberSaveable(author.author.id) { mutableStateOf(false) }
    val byState = sortOrganizationBooks(author.books, sort, reverse).groupBy { it.state }
    LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item {
            OrganizationPanel {
                Text(author.author.name, style = DeftTypography.headline)
                OrganizationProgress(author.stats)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    Text("${timeLabel(author.stats.activeSeconds)} read", style = DeftTypography.mono)
                    Text("average rating · ${ratingLabel(author.stats.averageRating)}", style = DeftTypography.mono)
                }
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    DeftButton("rename", { action = "rename" }, style = DeftButtonStyle.Secondary)
                    DeftButton("merge author", { action = "merge" }, style = DeftButtonStyle.Text)
                }
            }
        }
        item { BrowseSort(sort, reverse, organizationBookSortOptions, { sort = it }, { reverse = !reverse }) }
        BookState.entries.forEach { state ->
            val books = byState[state].orEmpty()
            if (books.isNotEmpty()) {
                item { Text(state.readingLabel(), style = DeftTypography.title, color = LocalDeftColors.current.muted) }
                item { AuthorBooks(books, openBook) }
            }
        }
    }
    when (action) {
        "rename" -> AuthorRename(author, model, closeSheet)
        "merge" -> AuthorMerge(author, authors, model, closeSheet, close)
    }
}

@Composable
private fun AuthorBooks(
    books: List<OrganizedBook>,
    openBook: (Long) -> Unit,
) {
    LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        items(books, key = { it.book.id }) { entry ->
            OrganizationBookTile(entry, Modifier.width(156.dp), { openBook(entry.book.id) }) {
                Text(timeLabel(entry.activeSeconds), style = DeftTypography.mono)
            }
        }
    }
}

@Composable
private fun AuthorRename(
    author: AuthorSummary,
    model: OrganizationViewModel,
    close: () -> Unit,
) {
    var name by rememberSaveable { mutableStateOf(author.author.name) }
    val busy by model.busy.collectAsStateWithLifecycle()
    OrganizationSheet("rename author", close, model) {
        OutlinedTextField(name, { name = it }, Modifier.fillMaxWidth(), singleLine = true, label = { Text("name") })
        DeftButton(
            "save name",
            { model.edit(close) { renameAuthor(author.author.id, name) } },
            enabled = !busy && name.isNotBlank() && name.length <= 240,
        )
    }
}

@Composable
private fun AuthorMerge(
    source: AuthorSummary,
    authors: List<AuthorSummary>,
    model: OrganizationViewModel,
    close: () -> Unit,
    merged: () -> Unit,
) {
    var query by rememberSaveable { mutableStateOf("") }
    var targetId by rememberSaveable { mutableStateOf<Long?>(null) }
    val target = authors.firstOrNull { it.author.id == targetId && it.author.id != source.author.id }
    OrganizationSheet("merge into", close, model) {
        OutlinedTextField(
            query,
            { query = it },
            Modifier.fillMaxWidth(),
            singleLine = true,
            label = { Text("find an author") },
        )
        val matches = authors.filter { it.author.id != source.author.id && it.author.name.contains(query.trim(), true) }
        if (matches.isEmpty()) Text("No matching authors.", style = DeftTypography.body)
        matches.forEach { author ->
            OrganizationRow(author.author.name, "${author.stats.total} books") { targetId = author.author.id }
        }
    }
    if (target != null) {
        DeftDialog(
            "merge authors?",
            "Use ${target.author.name} for books by ${source.author.name}. " +
                "Reading history will be kept.",
            "merge",
            {
                targetId = null
                model.edit(merged) { mergeAuthors(source.author.id, target.author.id) }
            },
            { targetId = null },
        )
    }
}
