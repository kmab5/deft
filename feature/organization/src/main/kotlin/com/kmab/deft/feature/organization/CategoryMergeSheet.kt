package com.kmab.deft.feature.organization

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.kmab.deft.core.datastore.LibrarySettings
import com.kmab.deft.core.designsystem.DeftDialog
import com.kmab.deft.library.organization.CategorySummary

@Composable
internal fun CategoryMergeSheet(
    source: CategorySummary,
    categories: List<CategorySummary>,
    model: OrganizationViewModel,
    close: () -> Unit,
    merged: () -> Unit,
) {
    var targetId by rememberSaveable { mutableStateOf<Long?>(null) }
    var query by rememberSaveable { mutableStateOf("") }
    var sort by rememberSaveable { mutableStateOf("name") }
    var reverse by rememberSaveable { mutableStateOf(false) }
    val target = categories.firstOrNull { it.category.id == targetId && it.category.id != source.category.id }
    OrganizationSheet("merge into", close, model, scrollable = false) {
        val options =
            browseCategories(
                categories.filter {
                    it.category.id != source.category.id
                },
                LibrarySettings(categorySort = sort, categoryReverse = reverse),
                query,
            )
        LazyColumn(
            Modifier.weight(1f, fill = false).testTag("category_merge_list"),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            item { BrowseSearch(query, { query = it }, "find a category") }
            item { BrowseSort(sort, reverse, categorySortOptions, { sort = it }, { reverse = !reverse }) }
            if (options.isEmpty()) item { OrganizationEmpty("No matching categories.") }
            items(options, key = { it.category.id }) { candidate ->
                OrganizationRow(candidate.category.name, candidate.choiceDetails()) { targetId = candidate.category.id }
            }
        }
    }
    if (target != null) {
        DeftDialog(
            "merge categories?",
            "Move the members and types from ${source.category.name} into ${target.category.name}. " +
                "The target's details and existing book positions will be kept.",
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
