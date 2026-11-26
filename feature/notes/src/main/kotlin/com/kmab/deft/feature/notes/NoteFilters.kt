package com.kmab.deft.feature.notes

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.kmab.deft.core.designsystem.DeftButton
import com.kmab.deft.core.designsystem.DeftButtonStyle
import com.kmab.deft.core.designsystem.DeftShapes
import com.kmab.deft.core.designsystem.DeftSheet
import com.kmab.deft.core.designsystem.DeftTypography
import com.kmab.deft.core.designsystem.LocalDeftColors
import com.kmab.deft.core.designsystem.icons.DeftActionIcons
import com.kmab.deft.core.model.HighlightColor
import com.kmab.deft.library.notes.NoteEntry
import com.kmab.deft.library.notes.NoteFilter
import com.kmab.deft.library.notes.NoteType

@Composable
internal fun NoteFilters(
    filter: NoteFilter,
    books: List<NoteEntry>,
    showBookFilter: Boolean,
    onFilter: (NoteFilter) -> Unit,
    onExport: (() -> Unit)?,
    busy: Boolean,
) {
    var open by rememberSaveable { mutableStateOf(false) }
    val colors = LocalDeftColors.current
    val labels = filter.activeLabels(books, showBookFilter)
    val reset = { onFilter(NoteFilter(bookId = filter.bookId.takeUnless { showBookFilter }, query = filter.query)) }
    OutlinedTextField(
        filter.query,
        { onFilter(filter.copy(query = it)) },
        Modifier.fillMaxWidth(),
        label = { Text("search notes") },
        trailingIcon = {
            if (filter.query.isNotEmpty()) {
                IconButton({ onFilter(filter.copy(query = "")) }) { Icon(DeftActionIcons.Close, "clear search") }
            }
        },
        singleLine = true,
    )
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        DeftButton(
            if (labels.isEmpty()) "filters" else "filters · ${labels.size}",
            { open = true },
            style = DeftButtonStyle.Secondary,
            icon = DeftActionIcons.Settings,
        )
        if (labels.isNotEmpty()) DeftButton("clear filters", reset, style = DeftButtonStyle.Text)
        if (onExport != null) DeftButton("share Markdown", onExport, style = DeftButtonStyle.Text, enabled = !busy)
    }
    if (labels.isNotEmpty()) {
        Text(labels.joinToString(" · "), style = DeftTypography.label, color = colors.accentText)
    }
    if (open) {
        NotesFilterSheet(filter, books, showBookFilter, onFilter) { open = false }
    }
}

private fun NoteFilter.activeLabels(
    books: List<NoteEntry>,
    showBookFilter: Boolean,
): List<String> =
    listOfNotNull(
        bookId?.takeIf { showBookFilter }?.let { bookTitle(books) },
        color?.filterLabel,
        type?.filterLabel,
    )

private fun NoteFilter.bookTitle(books: List<NoteEntry>): String =
    if (bookId == null) "all books" else books.firstOrNull { it.bookId == bookId }?.bookTitle ?: "unavailable book"

@Composable
private fun NotesFilterSheet(
    filter: NoteFilter,
    books: List<NoteEntry>,
    showBookFilter: Boolean,
    onFilter: (NoteFilter) -> Unit,
    onDismiss: () -> Unit,
) {
    var choosingBook by rememberSaveable { mutableStateOf(false) }
    DeftSheet(onDismiss) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            if (choosingBook) {
                Text("choose a book", style = DeftTypography.headline)
                Column(Modifier.weight(1f, fill = false)) {
                    NotesBookChoices(books, filter.bookId) {
                        onFilter(filter.copy(bookId = it))
                        choosingBook = false
                    }
                }
                DeftButton("back to filters", { choosingBook = false }, style = DeftButtonStyle.Text)
            } else {
                Text("filter notes", style = DeftTypography.headline)
                Column(
                    Modifier.weight(1f, fill = false).verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    if (showBookFilter) {
                        Text("book", style = DeftTypography.label)
                        DeftButton(filter.bookTitle(books), { choosingBook = true }, style = DeftButtonStyle.Secondary)
                    }
                    NoteTypeChoices(filter, onFilter)
                    NoteColorChoices(filter, onFilter)
                }
                DeftButton("done", onDismiss, Modifier.align(Alignment.End))
            }
        }
    }
}

@Composable
private fun NoteTypeChoices(
    filter: NoteFilter,
    onFilter: (NoteFilter) -> Unit,
) {
    Text("type", style = DeftTypography.label)
    FlowRow(Modifier.selectableGroup(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        (listOf(null) + NoteType.entries).forEach { option ->
            FilterChip(
                selected = filter.type == option,
                onClick = {
                    val color = filter.color.takeIf { it.canFilter(option) }
                    onFilter(filter.copy(type = option, color = color))
                },
                label = { Text(option?.filterLabel ?: "all types") },
                colors = notesChipColors(),
            )
        }
    }
}

@Composable
private fun NoteColorChoices(
    filter: NoteFilter,
    onFilter: (NoteFilter) -> Unit,
) {
    Text("color", style = DeftTypography.label)
    if (filter.type == NoteType.BOOKMARK) {
        Text("Bookmarks have no highlight color.", style = DeftTypography.body, color = LocalDeftColors.current.muted)
    } else {
        FlowRow(Modifier.selectableGroup(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            (listOf(null) + HighlightColor.entries).filter { it.canFilter(filter.type) }.forEach { option ->
                FilterChip(
                    selected = filter.color == option,
                    onClick = { onFilter(filter.copy(color = option)) },
                    label = { Text(option?.filterLabel ?: "all colors") },
                    leadingIcon =
                        if (option == null || option == HighlightColor.NONE) {
                            null
                        } else {
                            { Box(Modifier.size(10.dp).background(option.ruleColor(), DeftShapes.small)) }
                        },
                    colors = notesChipColors(),
                )
            }
        }
    }
}

@Composable
private fun NotesBookChoices(
    books: List<NoteEntry>,
    selected: Long?,
    onPick: (Long?) -> Unit,
) {
    var query by rememberSaveable { mutableStateOf("") }
    val matches =
        books.filter {
            it.bookTitle.contains(query, ignoreCase = true) || it.authors.contains(query, ignoreCase = true)
        }
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        OutlinedTextField(
            query,
            { query = it },
            Modifier.fillMaxWidth(),
            label = { Text("search books") },
            singleLine = true,
        )
        LazyColumn(Modifier.weight(1f, fill = false).fillMaxWidth().selectableGroup()) {
            item("all") { NotesBookChoice("all books", "", selected == null) { onPick(null) } }
            if (matches.isEmpty()) item("empty") { Text("No books match.", style = DeftTypography.body) }
            items(matches, key = NoteEntry::bookId) { book ->
                NotesBookChoice(book.bookTitle, book.authors, book.bookId == selected) { onPick(book.bookId) }
            }
        }
    }
}

@Composable
private fun NotesBookChoice(
    title: String,
    author: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val colors = LocalDeftColors.current
    Row(
        Modifier.fillMaxWidth().heightIn(min = 48.dp).selectable(selected, role = Role.RadioButton, onClick = onClick),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, style = DeftTypography.body, maxLines = 2, overflow = TextOverflow.Ellipsis)
            if (author.isNotBlank()) Text(author, style = DeftTypography.monoSmall, color = colors.muted)
        }
        if (selected) Icon(DeftActionIcons.Check, null, Modifier.size(20.dp), tint = colors.accentText)
    }
}

private val HighlightColor.filterLabel: String get() = if (this == HighlightColor.NONE) "colorless" else label

private fun HighlightColor?.canFilter(type: NoteType?): Boolean =
    when (type) {
        NoteType.BOOKMARK -> this == null
        NoteType.HIGHLIGHT -> this != HighlightColor.NONE
        else -> true
    }

private val NoteType.filterLabel: String
    get() =
        when (this) {
            NoteType.HIGHLIGHT -> "highlights"
            NoteType.NOTE -> "notes"
            NoteType.BOOKMARK -> "bookmarks"
        }
