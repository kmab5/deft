package com.kmab.deft.feature.organization

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
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
    val book = member.book.book
    OrganizationPanel(modifier) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            OrganizationBookCover(book, Modifier.size(44.dp, 64.dp).clickable(onClick = open))
            Box(Modifier.weight(1f)) {
                val prefix = if (category.isSeries) "#${positionLabel(member.membership.position)} · " else ""
                val status =
                    if (book.missing) {
                        " · file missing"
                    } else if (book.drmProtected) {
                        " · protected"
                    } else {
                        ""
                    }
                val review = if (member.membership.suggested) " · suggested" else ""
                OrganizationRow(book.displayTitle(), prefix + book.displayAuthors() + status + review, onClick = open)
            }
            CategoryMemberActions(category, member, model, showPosition = false)
        }
    }
}

@Composable
internal fun CategoryMemberActions(
    category: CategorySummary,
    member: CategoryBook,
    model: OrganizationViewModel,
    showPosition: Boolean = true,
) {
    var action by rememberSaveable { mutableStateOf<String?>(null) }
    val id = category.category.id
    val book = member.book.book
    Row(verticalAlignment = Alignment.CenterVertically) {
        if (showPosition) {
            Text(
                if (category.isSeries) {
                    "#${positionLabel(
                        member.membership.position,
                    )}"
                } else {
                    member.book.state.readingLabel()
                },
                Modifier.weight(1f),
                style = DeftTypography.mono,
            )
        }
        OrganizationMenu(
            buildList {
                if (category.isSeries) add("edit position" to { action = "position" })
                if (member.membership.suggested) {
                    add("confirm suggestion" to { model.edit { confirmMembers(id, setOf(book.id)) } })
                }
                add("remove from category" to { action = "remove" })
            },
        )
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

private const val MAX_SERIES_POSITION = 1e9
