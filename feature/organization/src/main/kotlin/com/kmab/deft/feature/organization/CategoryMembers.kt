package com.kmab.deft.feature.organization

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.FilterChip
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.kmab.deft.core.designsystem.DeftButton
import com.kmab.deft.core.designsystem.DeftDialog
import com.kmab.deft.core.designsystem.DeftTypography
import com.kmab.deft.core.model.CategoryOrder
import com.kmab.deft.library.organization.CategoryBook
import com.kmab.deft.library.organization.CategorySummary
import com.kmab.deft.library.organization.OrganizedBook
import kotlin.math.abs

@Composable
internal fun CategoryMemberRow(
    category: CategorySummary,
    member: CategoryBook,
    model: OrganizationViewModel,
    open: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var action by rememberSaveable { mutableStateOf<String?>(null) }
    val id = category.category.id
    val book = member.book.book
    OrganizationPanel(modifier) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            androidx.compose.foundation.layout.Box(Modifier.weight(1f)) {
                val prefix = if (category.isSeries) "#${positionLabel(member.membership.position)} · " else ""
                val status =
                    when {
                        book.missing -> " · file missing"
                        book.drmProtected -> " · protected"
                        else -> ""
                    }
                val review = if (member.membership.suggested) " · suggested" else ""
                OrganizationRow(book.displayTitle(), prefix + book.displayAuthors() + status + review, onClick = open)
            }
            OrganizationMenu(
                buildList {
                    if (category.isSeries) add("edit position" to { action = "position" })
                    if (member.membership.suggested) {
                        add(
                            "confirm suggestion" to {
                                model.edit { confirmMembers(id, setOf(book.id)) }
                            },
                        )
                    }
                    add("remove from category" to { action = "remove" })
                },
            )
        }
    }
    when (action) {
        "position" -> CategoryPositionSheet(member, model) { action = null }
        "remove" ->
            DeftDialog("remove book?", "The book and its reading history will be kept.", "remove", {
                action = null
                model.edit { removeMember(id, book.id) }
            }, { action = null })
    }
}

@Composable
private fun CategoryPositionSheet(
    member: CategoryBook,
    model: OrganizationViewModel,
    close: () -> Unit,
) {
    var text by rememberSaveable { mutableStateOf(positionLabel(member.membership.position)) }
    val value =
        text
            .trim()
            .replace(',', '.')
            .toDoubleOrNull()
            ?.takeIf { it.isFinite() && abs(it) <= MAX_SERIES_POSITION }
    OrganizationSheet("series position", close, model) {
        OutlinedTextField(
            text,
            { text = it },
            Modifier.fillMaxWidth(),
            singleLine = true,
            label = { Text("position") },
            isError = text.isNotBlank() && value == null,
            supportingText = { Text("Use a number like −1, 0 or 2.5.") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text),
        )
        DeftButton("save position", {
            if (value != null) {
                model.edit(close) {
                    setPosition(member.membership.categoryId, member.membership.bookId, value)
                }
            }
        }, enabled = value != null)
    }
}

@Composable
internal fun CategoryOrderSheet(
    category: CategorySummary,
    model: OrganizationViewModel,
    close: () -> Unit,
) {
    OrganizationSheet("sort books", close, model) {
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            CategoryOrder.entries.forEach { order ->
                FilterChip(category.category.order == order, {
                    val current = category.category
                    model.edit(close) {
                        saveCategory(
                            current.id,
                            current.name,
                            current.description,
                            category.types.map { it.id }.toSet(),
                            order,
                            current.coverPath,
                        )
                    }
                }, label = { Text(order.name.lowercase().replace('_', ' ')) })
            }
        }
    }
}

@Composable
internal fun CategoryAddBooks(
    category: CategorySummary,
    books: List<OrganizedBook>,
    model: OrganizationViewModel,
    close: () -> Unit,
) {
    var query by rememberSaveable { mutableStateOf("") }
    var selected by rememberSaveable { mutableStateOf(arrayListOf<Long>()) }
    val present = category.books.map { it.book.book.id }.toSet()
    val search = query.trim()
    val matches =
        books.filter { entry ->
            val book = entry.book
            book.id !in present &&
                (book.displayTitle().contains(search, true) || book.displayAuthors().contains(search, true))
        }
    OrganizationSheet("add books", close, model) {
        OutlinedTextField(
            query,
            { query = it },
            Modifier.fillMaxWidth(),
            label = { Text("find books") },
            singleLine = true,
        )
        DeftButton("add ${selected.size} books", {
            model.edit(close) { addBooks(category.category.id, selected.toSet() - present) }
        }, enabled = selected.isNotEmpty())
        if (matches.isEmpty()) Text("No matching books.", style = DeftTypography.body)
        matches.forEach { item ->
            val book = item.book
            FilterChip(book.id in selected, {
                selected = ArrayList(if (book.id in selected) selected - book.id else selected + book.id)
            }, label = {
                Column {
                    Text(book.displayTitle())
                    Text(book.displayAuthors(), style = DeftTypography.mono)
                }
            }, modifier = Modifier.fillMaxWidth())
        }
    }
}

@Composable
internal fun CategoryMergeSheet(
    source: CategorySummary,
    categories: List<CategorySummary>,
    model: OrganizationViewModel,
    close: () -> Unit,
    merged: () -> Unit,
) {
    var targetId by rememberSaveable { mutableStateOf<Long?>(null) }
    val target = categories.firstOrNull { it.category.id == targetId && it.category.id != source.category.id }
    OrganizationSheet("merge into", close, model) {
        val options = categories.filter { it.category.id != source.category.id }
        if (options.isEmpty()) Text("Create another category to merge into.", style = DeftTypography.body)
        options.forEach { candidate ->
            OrganizationRow(candidate.category.name, candidate.choiceDetails()) { targetId = candidate.category.id }
        }
    }
    if (target != null) {
        DeftDialog(
            "merge categories?",
            "Move the members and types from ${source.category.name} " +
                "into ${target.category.name}. The target's details and existing book positions will be kept.",
            "merge",
            {
                targetId = null
                model.edit(merged) {
                    mergeCategories(source.category.id, target.category.id)
                    model.covers.remove(source.category.coverPath)
                }
            },
            { targetId = null },
        )
    }
}

private const val MAX_SERIES_POSITION = 1e9
