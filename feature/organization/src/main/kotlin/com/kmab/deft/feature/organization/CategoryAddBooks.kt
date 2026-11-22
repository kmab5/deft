package com.kmab.deft.feature.organization

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material3.Checkbox
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.kmab.deft.core.designsystem.DeftShapes
import com.kmab.deft.core.designsystem.DeftTypography
import com.kmab.deft.core.designsystem.LocalDeftColors
import com.kmab.deft.core.designsystem.icons.DeftActionIcons
import com.kmab.deft.core.model.BookFormat
import com.kmab.deft.core.model.BookState
import com.kmab.deft.library.organization.CategorySummary
import com.kmab.deft.library.organization.OrganizedBook

@Composable
internal fun CategoryAddBooks(
    category: CategorySummary,
    books: List<OrganizedBook>,
    model: OrganizationViewModel,
    close: () -> Unit,
) {
    var query by rememberSaveable { mutableStateOf("") }
    var selected by rememberSaveable { mutableStateOf(arrayListOf<Long>()) }
    var sort by rememberSaveable { mutableStateOf("title") }
    var reverse by rememberSaveable { mutableStateOf(false) }
    var state by rememberSaveable { mutableStateOf<BookState?>(null) }
    var format by rememberSaveable { mutableStateOf<BookFormat?>(null) }
    var availability by rememberSaveable { mutableStateOf("all files") }
    val busy by model.busy.collectAsStateWithLifecycle()
    val present = category.books.map { it.book.book.id }.toSet()
    val availableIds = books.map { it.book.id }.toSet() - present
    val selection = selected.toSet().intersect(availableIds)
    val matches =
        sortOrganizationBooks(
            books.filter { entry ->
                entry.book.id !in present && entry.matchesPickerFilters(query, state, format, availability)
            },
            sort,
            reverse,
        )
    OrganizationSheet("add books", close, model, scrollable = false) {
        OutlinedTextField(
            query,
            { query = it },
            Modifier.fillMaxWidth(),
            label = { Text("find books") },
            singleLine = true,
        )
        BrowseSort(sort, reverse, organizationBookSortOptions, { sort = it }, { reverse = !reverse })
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            BrowseState(state) { state = it }
            BrowseChoice(
                format?.name ?: "all formats",
                listOf("all formats") + BookFormat.entries.map { it.name },
                format?.name ?: "all formats",
                { value -> format = BookFormat.entries.find { it.name == value } },
            )
            BrowseChoice(availability, listOf("all files", "available", "missing"), availability, { availability = it })
        }
        Box(Modifier.fillMaxWidth().weight(1f, fill = false)) {
            LazyColumn(
                Modifier.testTag("category_add_books_list"),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(bottom = 88.dp),
            ) {
                if (matches.isEmpty()) item { OrganizationEmpty("No matching books.") }
                items(matches, key = { it.book.id }) { entry ->
                    val checked = entry.book.id in selection
                    Surface(
                        modifier =
                            Modifier.fillMaxWidth().toggleable(value = checked, enabled = !busy, role = Role.Checkbox) {
                                selected =
                                    ArrayList(if (checked) selected - entry.book.id else selected + entry.book.id)
                            },
                        shape = DeftShapes.card,
                        color = if (checked) LocalDeftColors.current.surface2 else LocalDeftColors.current.surface,
                        border =
                            BorderStroke(
                                1.dp,
                                if (checked) LocalDeftColors.current.accent else LocalDeftColors.current.line,
                            ),
                    ) {
                        Row(Modifier.padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
                            OrganizationBookLine(entry, Modifier.weight(1f), location = true)
                            Checkbox(checked, null, enabled = !busy)
                        }
                    }
                }
            }
            if (selection.isNotEmpty()) {
                FloatingActionButton(
                    onClick = { if (!busy) model.edit(close) { addBooks(category.category.id, selection) } },
                    modifier =
                        Modifier.align(Alignment.BottomEnd).padding(8.dp).semantics {
                            contentDescription =
                                "add ${selection.size} selected books"
                        },
                    shape = DeftShapes.card,
                    containerColor = LocalDeftColors.current.accent,
                    contentColor = LocalDeftColors.current.onAccent,
                ) {
                    Row(
                        Modifier.padding(horizontal = 16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Icon(DeftActionIcons.Check, null)
                        Text("add ${selection.size}", style = DeftTypography.label)
                    }
                }
            }
        }
    }
}

private fun OrganizedBook.matchesPickerFilters(
    query: String,
    selectedState: BookState?,
    selectedFormat: BookFormat?,
    availability: String,
): Boolean {
    val matchesState = selectedState == null || selectedState == state
    val matchesFormat = selectedFormat == null || selectedFormat == book.format
    val matchesAvailability = availability == "all files" || book.missing == (availability == "missing")
    val matchesQuery =
        listOf(book.displayTitle(), book.displayAuthors(), book.fileLocation).any { it.contains(query.trim(), true) }
    return matchesState && matchesFormat && matchesAvailability && matchesQuery
}
