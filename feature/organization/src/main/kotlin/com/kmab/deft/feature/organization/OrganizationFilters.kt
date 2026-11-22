package com.kmab.deft.feature.organization

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.kmab.deft.core.database.entity.CategoryTypeEntity
import com.kmab.deft.core.datastore.LibrarySettings
import com.kmab.deft.core.designsystem.DeftShapes
import com.kmab.deft.core.designsystem.DeftSwitch
import com.kmab.deft.core.designsystem.DeftTypography
import com.kmab.deft.core.designsystem.LocalDeftColors
import com.kmab.deft.core.designsystem.icons.DeftActionIcons
import com.kmab.deft.core.model.BookState

@Composable
internal fun FilterSheet(
    title: String,
    model: OrganizationViewModel,
    close: () -> Unit,
    reset: () -> Unit,
    content: @Composable ColumnScope.() -> Unit,
) {
    OrganizationSheet(title, close, model, footer = {
        Row(Modifier.fillMaxWidth().height(IntrinsicSize.Min), Arrangement.spacedBy(8.dp)) {
            OrganizationButton("reset filters", reset, Modifier.weight(1f).fillMaxHeight())
            OrganizationButton("done", close, Modifier.weight(1f).fillMaxHeight(), primary = true)
        }
    }, content = content)
}

@Composable
internal fun FilterValue(
    label: String,
    current: String,
    options: List<String>,
    change: (String) -> Unit,
) {
    val colors = LocalDeftColors.current
    var open by remember { mutableStateOf(false) }
    Box {
        Surface(shape = DeftShapes.small, color = colors.surface, border = BorderStroke(1.dp, colors.line)) {
            Row(
                Modifier
                    .fillMaxWidth()
                    .heightIn(min = 48.dp)
                    .clickable(role = Role.Button) { open = true }
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(label, Modifier.weight(1f), style = DeftTypography.label)
                Text(current, Modifier.weight(1f), style = DeftTypography.mono, color = colors.muted)
                Icon(DeftActionIcons.ChevronRight, null, Modifier.size(20.dp), tint = colors.muted)
            }
        }
        OrganizationChoiceMenu(open, { open = false }, options, current, select = change)
    }
}

@Composable
internal fun ReadingStateFilter(
    state: BookState?,
    change: (BookState?) -> Unit,
) {
    val options = listOf("all states") + BookState.entries.map { it.readingLabel() }
    FilterValue("reading state", state?.readingLabel() ?: "all states", options) { label ->
        change(BookState.entries.find { it.readingLabel() == label })
    }
}

@Composable
internal fun CategoryFilters(
    preferences: LibrarySettings,
    types: List<CategoryTypeEntity>,
    filter: Long?,
    model: OrganizationViewModel,
    close: () -> Unit,
) {
    FilterSheet("category filters", model, close, { model.preference { resetCategoryFilters() } }) {
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text("category type", style = DeftTypography.label)
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                val options = listOf(null to "all", 0L to "untyped") + types.map { it.id to it.name }
                options.forEach { (id, name) ->
                    FilterChip(
                        filter == id,
                        { model.preference { setCategoryTypeFilter(id) } },
                        label = { Text(name) },
                        shape = DeftShapes.small,
                    )
                }
            }
        }
        FilterValue("review state", preferences.categoryReview, listOf("all", "review", "reviewed")) {
            model.preference { setCategoryReview(it) }
        }
        ReadingStateFilter(preferences.categoryState) { model.preference { setCategoryState(it) } }
        val grouped = preferences.groupCategoriesByType
        Row(
            Modifier
                .fillMaxWidth()
                .heightIn(min = 48.dp)
                .toggleable(grouped, role = Role.Switch) { model.preference { setGroupCategoriesByType(it) } }
                .padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text("group by type", Modifier.weight(1f), style = DeftTypography.label)
            DeftSwitch(grouped, onCheckedChange = null)
        }
    }
}

@Composable
internal fun AuthorFilters(
    preferences: LibrarySettings,
    model: OrganizationViewModel,
    close: () -> Unit,
) {
    FilterSheet("author filters", model, close, { model.preference { resetAuthorFilters() } }) {
        ReadingStateFilter(preferences.authorState) { model.preference { setAuthorState(it) } }
        FilterValue("rating", preferences.authorRating, listOf("all", "rated", "unrated")) {
            model.preference { setAuthorRating(it) }
        }
    }
}
