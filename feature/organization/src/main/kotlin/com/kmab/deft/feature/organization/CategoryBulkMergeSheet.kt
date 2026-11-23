package com.kmab.deft.feature.organization

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.kmab.deft.core.datastore.LibrarySettings
import com.kmab.deft.core.designsystem.DeftDialog
import com.kmab.deft.core.designsystem.DeftTypography
import com.kmab.deft.library.organization.CategorySummary

@Composable
internal fun CategoryBulkMergeSheet(
    ids: Set<Long>,
    categories: List<CategorySummary>,
    model: OrganizationViewModel,
    close: () -> Unit,
    done: () -> Unit,
) {
    var targetId by rememberSaveable { mutableStateOf<Long?>(null) }
    var query by rememberSaveable { mutableStateOf("") }
    val busy by model.busy.collectAsState()
    val options =
        browseCategories(categories.filter { ids.size > 1 || it.category.id !in ids }, LibrarySettings(), query)
    OrganizationSheet("merge into", close, model, scrollable = false) {
        Text(
            "Choose the category to keep. Its details and existing book positions take priority.",
            style = DeftTypography.body,
        )
        BrowseSearch(query, { query = it }, "find a category")
        LazyColumn(Modifier.weight(1f, fill = false), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            if (options.isEmpty()) item { OrganizationEmpty("No matching destination categories.") }
            items(options, key = { it.category.id }) { candidate ->
                OrganizationRow(
                    candidate.category.name,
                    candidate.choiceDetails(),
                    enabled = !busy,
                    modifier = Modifier.testTag("merge_target_${candidate.category.id}"),
                ) {
                    targetId = candidate.category.id
                }
            }
        }
    }
    val target = categories.firstOrNull { it.category.id == targetId }
    if (target != null) {
        DeftDialog(
            "merge categories?",
            "Move ${(ids - target.category.id).size} categories into ${target.category.name}. " +
                "Their books and types will be combined, then the source categories removed. Books and history stay.",
            "merge",
            {
                targetId = null
                model.edit(done) {
                    val covers =
                        get()
                            .categories
                            .filter { it.category.id in ids && it.category.id != target.category.id }
                            .mapNotNull { it.category.coverPath }
                    mergeCategories(ids, target.category.id)
                    removeUnusedCovers(covers, model.covers)
                }
            },
            { targetId = null },
            modifier = Modifier.verticalScroll(rememberScrollState()),
        )
    }
}
