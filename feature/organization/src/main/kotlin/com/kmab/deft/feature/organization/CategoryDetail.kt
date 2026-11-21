package com.kmab.deft.feature.organization

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.kmab.deft.core.designsystem.DeftButton
import com.kmab.deft.core.designsystem.DeftButtonStyle
import com.kmab.deft.core.designsystem.DeftDialog
import com.kmab.deft.core.designsystem.DeftTypography
import com.kmab.deft.core.designsystem.LocalDeftColors
import com.kmab.deft.core.designsystem.RatingBar
import com.kmab.deft.core.model.CategoryOrder
import com.kmab.deft.library.organization.CategorySummary
import com.kmab.deft.library.organization.OrganizationSnapshot

@Composable
internal fun CategoryDetail(
    category: CategorySummary,
    snapshot: OrganizationSnapshot,
    model: OrganizationViewModel,
    close: () -> Unit,
    openBook: (Long) -> Unit,
) {
    val details = category.category
    var action by rememberSaveable(details.id) { mutableStateOf<String?>(null) }
    val closeSheet = { action = null }
    LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item {
            CategoryOverview(category, model) { action = it }
        }
        item { Text("books", style = DeftTypography.title) }
        if (category.books.isEmpty()) item { OrganizationEmpty("No books in this category.") }
        items(category.books, key = { it.book.book.id }) { member ->
            CategoryMemberRow(category, member, model, { openBook(member.book.book.id) }, Modifier.animateItem())
        }
    }
    when (action) {
        "edit" -> CategoryEditor(category, snapshot.types, model, closeSheet)
        "add" -> CategoryAddBooks(category, snapshot.books, model, closeSheet)
        "order" -> CategoryOrderSheet(category, model, closeSheet)
        "reorder" -> CategoryReorderSheet(category, model, closeSheet)
        "merge" -> CategoryMergeSheet(category, snapshot.categories, model, closeSheet, close)
        "delete" ->
            DeftDialog(
                "delete category?",
                if (details.autoCreated) {
                    "Books and reading history will be kept. " +
                        "Series auto-fill may recreate this category on a future scan."
                } else {
                    "Books and reading history will be kept."
                },
                "delete category",
                {
                    action = null
                    model.edit(close) {
                        deleteCategory(details.id)
                        model.covers.remove(details.coverPath)
                    }
                },
                closeSheet,
            )
    }
}

@Composable
private fun CategoryOverview(
    category: CategorySummary,
    model: OrganizationViewModel,
    action: (String) -> Unit,
) {
    val details = category.category
    OrganizationPanel {
        CategoryCover(category, Modifier.fillMaxWidth().height(148.dp))
        Text(details.name, style = DeftTypography.headline)
        Text(category.choiceDetails(), style = DeftTypography.mono, color = LocalDeftColors.current.muted)
        details.description?.let { Text(it, style = DeftTypography.body) }
        OrganizationProgress(category.stats)
        CategoryRating(category, model)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            DeftButton("add books", { action("add") })
            DeftButton("edit", { action("edit") }, style = DeftButtonStyle.Secondary)
            OrganizationMenu(
                buildList {
                    if (!category.isSeries) add("sort books" to { action("order") })
                    if (!category.isSeries && details.order == CategoryOrder.MANUAL && category.books.size > 1) {
                        add("reorder books" to { action("reorder") })
                    }
                    add("merge category" to { action("merge") })
                    add("delete category" to { action("delete") })
                },
            )
        }
        if (category.needsReview) {
            Text("Suggested from series metadata.", style = DeftTypography.body)
            DeftButton("confirm all suggestions", {
                model.edit {
                    confirmMembers(
                        details.id,
                        category.books
                            .filter { it.membership.suggested }
                            .map { it.book.book.id }
                            .toSet(),
                    )
                }
            }, style = DeftButtonStyle.Secondary)
        }
    }
}

@Composable
private fun CategoryRating(
    category: CategorySummary,
    model: OrganizationViewModel,
) {
    val id = category.category.id
    val override = category.category.ratingOverride
    Text(
        if (override == null) {
            "average rating · ${ratingLabel(category.rating)}"
        } else {
            "your rating · ${ratingLabel(override)} · average ${ratingLabel(category.stats.averageRating)}"
        },
        style = DeftTypography.mono,
    )
    RatingBar((category.rating ?: 0.0).toFloat(), { value ->
        model.edit { setRating(id, value.toDouble().takeIf { it > 0.0 }) }
    }, showValue = false)
    if (override != null) {
        DeftButton("use average", { model.edit { setRating(id, null) } }, style = DeftButtonStyle.Text)
    }
}
