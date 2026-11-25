package com.kmab.deft.feature.organization

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.kmab.deft.core.database.entity.CategoryTypeEntity
import com.kmab.deft.core.designsystem.DeftButton
import com.kmab.deft.core.designsystem.DeftDialog
import com.kmab.deft.core.designsystem.DeftTypography
import com.kmab.deft.library.organization.OrganizationSnapshot

@Composable
internal fun CategoryBulkActions(
    action: CategoryBulkAction,
    ids: Set<Long>,
    snapshot: OrganizationSnapshot,
    model: OrganizationViewModel,
    close: () -> Unit,
    done: () -> Unit,
) {
    when (action) {
        CategoryBulkAction.ADD_TYPES, CategoryBulkAction.REMOVE_TYPES ->
            CategoryBulkTypesSheet(ids, snapshot.types, action == CategoryBulkAction.ADD_TYPES, model, close, done)
        CategoryBulkAction.MERGE -> CategoryBulkMergeSheet(ids, snapshot.categories, model, close, done)
        CategoryBulkAction.DELETE ->
            DeftDialog(
                "delete ${ids.size} categories?",
                "Only these categories and their memberships will be removed. Books, files and reading history stay. " +
                    "With series auto-fill enabled, a later scan may recreate categories " +
                    "from books' series metadata.",
                "delete",
                {
                    close()
                    model.edit(done) {
                        val covers =
                            get()
                                .categories
                                .filter {
                                    it.category.id in ids
                                }.mapNotNull { it.category.coverPath }
                        deleteCategories(ids)
                        removeUnusedCovers(covers, model.covers)
                    }
                },
                close,
                modifier = Modifier.verticalScroll(rememberScrollState()),
            )
    }
}

@Composable
private fun CategoryBulkTypesSheet(
    ids: Set<Long>,
    types: List<CategoryTypeEntity>,
    add: Boolean,
    model: OrganizationViewModel,
    close: () -> Unit,
    done: () -> Unit,
) {
    var checked by rememberSaveable { mutableStateOf(emptyList<Long>()) }
    val busy by model.busy.collectAsState()
    OrganizationSheet(if (add) "add types" else "remove types", close, model, footer = {
        DeftButton("apply types", {
            model.edit(done) { updateCategoryTypes(ids, checked.toSet(), add) }
        }, enabled = !busy && checked.isNotEmpty())
    }) {
        Text(
            "${ids.size} categories selected. Other types and category details stay unchanged.",
            style = DeftTypography.body,
        )
        if (!add) {
            Text(
                "Removing Series also removes series aliases and stops automatic series matching.",
                style = DeftTypography.mono,
            )
        }
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            types.forEach { type ->
                FilterChip(type.id in checked, {
                    checked = if (type.id in checked) checked - type.id else checked + type.id
                }, label = { Text(type.name) }, enabled = !busy)
            }
        }
    }
}
