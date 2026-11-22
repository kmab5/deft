package com.kmab.deft.feature.organization

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import com.kmab.deft.core.designsystem.DeftButton
import com.kmab.deft.core.designsystem.DeftButtonStyle
import com.kmab.deft.core.designsystem.DeftDialog
import com.kmab.deft.core.designsystem.DeftTypography
import com.kmab.deft.core.designsystem.LocalDeftColors
import com.kmab.deft.core.designsystem.RatingBar
import com.kmab.deft.core.designsystem.icons.DeftActionIcons
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
    var grid by rememberSaveable(details.id) { mutableStateOf(false) }
    val availableWidth = LocalConfiguration.current.screenWidthDp / LocalDensity.current.fontScale
    val columns = if (availableWidth < MEMBER_GRID_MIN_WIDTH) 1 else 2
    LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item {
            CategoryOverview(category, model) { action = it }
        }
        if (category.needsReview) item { CategoryReview(category, model) }
        item {
            CategoryBooksHeader(category, grid, { grid = it }, { action = it })
        }
        categoryMemberItems(category, model, grid, columns, openBook)
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
private fun CategoryBooksHeader(
    category: CategorySummary,
    grid: Boolean,
    onGrid: (Boolean) -> Unit,
    action: (String) -> Unit,
) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text("books · ${category.books.size}", Modifier.weight(1f), style = DeftTypography.title)
        IconButton({
            onGrid(!grid)
        }) {
            Icon(
                if (grid) DeftActionIcons.List else DeftActionIcons.Grid,
                if (grid) "show book list" else "show book grid",
            )
        }
    }
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        if (!category.isSeries) {
            DeftButton(
                category.category.order.name
                    .lowercase()
                    .replace('_', ' '),
                {
                    action("order")
                },
                style = DeftButtonStyle.Secondary,
                icon = DeftActionIcons.Settings,
            )
        }
        if (category.books.size > 1 && (category.isSeries || category.category.order == CategoryOrder.MANUAL)) {
            DeftButton(
                "reorder books",
                { action("reorder") },
                style = DeftButtonStyle.Secondary,
                icon = DeftActionIcons.List,
            )
        }
    }
}

private fun LazyListScope.categoryMemberItems(
    category: CategorySummary,
    model: OrganizationViewModel,
    grid: Boolean,
    columns: Int,
    openBook: (Long) -> Unit,
) {
    if (category.books.isEmpty()) item { OrganizationEmpty("No books in this category.") }
    if (grid) {
        items(category.books.chunked(columns), key = {
            it
                .first()
                .book.book.id
        }) { row ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                row.forEach { member ->
                    OrganizationBookTile(member.book, Modifier.weight(1f), { openBook(member.book.book.id) }) {
                        CategoryMemberActions(category, member, model)
                    }
                }
                repeat(columns - row.size) { Box(Modifier.weight(1f)) }
            }
        }
    } else {
        items(category.books, key = { it.book.book.id }) { member ->
            CategoryMemberRow(category, member, model, { openBook(member.book.book.id) }, Modifier.animateItem())
        }
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
        Row(horizontalArrangement = Arrangement.spacedBy(14.dp), verticalAlignment = Alignment.Top) {
            CategoryCover(category, Modifier.size(84.dp, 108.dp))
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(details.name, style = DeftTypography.headline)
                Text(
                    category.types
                        .joinToString(" · ") {
                            it.name
                        }.ifEmpty { "untyped" },
                    style = DeftTypography.mono,
                    color = LocalDeftColors.current.muted,
                )
                details.description?.let { Text(it, style = DeftTypography.body) }
            }
        }
        OrganizationProgress(category.stats)
        HorizontalDivider(color = LocalDeftColors.current.line)
        CategoryRating(category, model)
        Row(
            Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            DeftButton("add books", { action("add") }, Modifier.weight(1f), icon = DeftActionIcons.Added)
            IconButton({ action("edit") }) { Icon(DeftActionIcons.Rename, "edit category") }
            OrganizationMenu(listOf("merge category" to { action("merge") }, "delete category" to { action("delete") }))
        }
    }
}

@Composable
private fun CategoryReview(
    category: CategorySummary,
    model: OrganizationViewModel,
) {
    val suggestions =
        category.books
            .filter { it.membership.suggested }
            .map { it.book.book.id }
            .toSet()
    OrganizationPanel {
        Row(
            Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Icon(DeftActionIcons.Info, null, tint = LocalDeftColors.current.pausedText)
            Column(Modifier.weight(1f)) {
                Text("${suggestions.size} to review", style = DeftTypography.title)
                Text("Added from series metadata", style = DeftTypography.mono, color = LocalDeftColors.current.muted)
            }
            IconButton({ model.edit { confirmMembers(category.category.id, suggestions) } }) {
                Icon(DeftActionIcons.Check, "confirm all suggestions")
            }
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
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(if (override == null) "average rating" else "your rating", style = DeftTypography.label)
            Text(
                if (override ==
                    null
                ) {
                    ratingLabel(category.rating)
                } else {
                    "${ratingLabel(override)} · average ${ratingLabel(category.stats.averageRating)}"
                },
                style = DeftTypography.mono,
                color = LocalDeftColors.current.muted,
            )
        }
        if (override !=
            null
        ) {
            DeftButton("use average", {
                model.edit { setRating(id, null) }
            }, style = DeftButtonStyle.Text, icon = DeftActionIcons.Return)
        }
    }
    RatingBar((category.rating ?: 0.0).toFloat(), { value ->
        model.edit { setRating(id, value.toDouble().takeIf { it > 0.0 }) }
    }, modifier = Modifier.widthIn(max = 248.dp), showValue = false)
}

private const val MEMBER_GRID_MIN_WIDTH = 360
