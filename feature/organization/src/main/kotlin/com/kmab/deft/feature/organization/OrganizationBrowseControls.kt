package com.kmab.deft.feature.organization

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.unit.dp
import com.kmab.deft.core.designsystem.DeftShapes
import com.kmab.deft.core.designsystem.DeftTypography
import com.kmab.deft.core.designsystem.LocalDeftColors
import com.kmab.deft.core.designsystem.icons.DeftActionIcons
import com.kmab.deft.core.designsystem.icons.DeftIcons

@Composable
internal fun BrowseSearch(
    query: String,
    change: (String) -> Unit,
    label: String,
) {
    OutlinedTextField(
        query,
        change,
        Modifier.fillMaxWidth().semantics { contentDescription = label },
        singleLine = true,
        shape = DeftShapes.small,
        textStyle = DeftTypography.body,
        placeholder = { Text(label, style = DeftTypography.body) },
        leadingIcon = { Icon(DeftIcons.Search, null, Modifier.size(20.dp)) },
        trailingIcon =
            if (query.isEmpty()) {
                null
            } else {
                { IconButton({ change("") }) { Icon(DeftActionIcons.Close, "clear search") } }
            },
    )
}

@Composable
internal fun OrganizationButton(
    label: String,
    click: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    primary: Boolean = false,
    enabled: Boolean = true,
) {
    val colors = LocalDeftColors.current
    Surface(
        onClick = click,
        modifier = modifier,
        enabled = enabled,
        shape = DeftShapes.small,
        color = if (primary && enabled) colors.accent else colors.surface,
        contentColor =
            when {
                !enabled -> colors.dim
                primary -> colors.onAccent
                else -> colors.text
            },
        border = if (primary && enabled) null else BorderStroke(1.dp, colors.line2),
    ) {
        Row(
            Modifier.heightIn(min = 48.dp).padding(horizontal = 12.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (icon != null) Icon(icon, null, Modifier.size(20.dp))
            Text(label, style = DeftTypography.label)
        }
    }
}

@Composable
internal fun BrowseSort(
    sort: String,
    reverse: Boolean,
    options: List<String>,
    select: (String) -> Unit,
    toggle: () -> Unit,
    filterCount: Int = 0,
    openFilters: (() -> Unit)? = null,
    display: (@Composable () -> Unit)? = null,
) {
    BoxWithConstraints(Modifier.fillMaxWidth()) {
        val compact = maxWidth / LocalDensity.current.fontScale < 300.dp
        if (compact && openFilters != null && display != null) {
            Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                SortControl(sort, reverse, options, select, toggle, Modifier.fillMaxWidth())
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    FilterAction(filterCount, compact = false, openFilters)
                    Spacer(Modifier.weight(1f))
                    display()
                }
            }
            return@BoxWithConstraints
        }
        Row(Modifier.fillMaxWidth(), Arrangement.spacedBy(8.dp), Alignment.CenterVertically) {
            Box(Modifier.weight(1f)) {
                val limit = if (openFilters == null) 240.dp else 380.dp
                Row(
                    Modifier.widthIn(max = limit).fillMaxWidth(),
                    Arrangement.spacedBy(8.dp),
                    Alignment.CenterVertically,
                ) {
                    SortControl(sort, reverse, options, select, toggle, Modifier.weight(1f))
                    if (openFilters != null) FilterAction(filterCount, compact, openFilters)
                }
            }
            if (display != null) display()
        }
    }
}

@Composable
private fun SortControl(
    sort: String,
    reverse: Boolean,
    options: List<String>,
    select: (String) -> Unit,
    toggle: () -> Unit,
    modifier: Modifier,
) {
    val colors = LocalDeftColors.current
    var expanded by remember { mutableStateOf(false) }
    Surface(modifier, shape = DeftShapes.small, color = colors.surface, border = BorderStroke(1.dp, colors.line2)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.weight(1f)) {
                Row(
                    Modifier
                        .fillMaxWidth()
                        .heightIn(min = 48.dp)
                        .clickable(role = Role.Button, onClickLabel = "choose sort") { expanded = true }
                        .padding(horizontal = 10.dp, vertical = 6.dp),
                    Arrangement.spacedBy(8.dp),
                    Alignment.CenterVertically,
                ) {
                    Icon(DeftActionIcons.Contents, null, Modifier.size(20.dp), tint = colors.muted)
                    Text(sortLabel(sort), style = DeftTypography.label)
                }
                OrganizationChoiceMenu(expanded, { expanded = false }, options, sort, ::sortLabel, select)
            }
            IconButton(toggle, Modifier.semantics { stateDescription = if (reverse) "reversed" else "default order" }) {
                Icon(if (reverse) DeftActionIcons.Descending else DeftActionIcons.Ascending, "reverse sort")
            }
        }
    }
}

@Composable
private fun FilterAction(
    count: Int,
    compact: Boolean,
    click: () -> Unit,
) {
    val colors = LocalDeftColors.current
    val label = if (count == 0) "filters" else "filters · $count"
    Surface(
        click,
        Modifier.widthIn(min = 48.dp).semantics { contentDescription = label },
        shape = DeftShapes.small,
        color = colors.surface,
        contentColor = if (count > 0) colors.accentText else colors.text,
        border = BorderStroke(1.dp, if (count > 0) colors.accent else colors.line2),
    ) {
        Row(
            Modifier.heightIn(min = 48.dp).padding(horizontal = 10.dp),
            Arrangement.spacedBy(6.dp, Alignment.CenterHorizontally),
            Alignment.CenterVertically,
        ) {
            Icon(DeftActionIcons.Settings, null, Modifier.size(20.dp))
            if (!compact) {
                Text(label, style = DeftTypography.label)
            } else if (count > 0) {
                Text(count.toString(), style = DeftTypography.label)
            }
        }
    }
}

@Composable
internal fun OrganizationChoiceMenu(
    open: Boolean,
    close: () -> Unit,
    options: List<String>,
    current: String,
    label: (String) -> String = { it },
    select: (String) -> Unit,
) {
    val colors = LocalDeftColors.current
    DropdownMenu(
        open,
        close,
        shape = DeftShapes.card,
        containerColor = colors.surface,
        border = BorderStroke(1.dp, colors.line),
    ) {
        options.forEach { option ->
            DropdownMenuItem(
                modifier = Modifier.semantics { selected = option == current },
                text = { Text(label(option), style = DeftTypography.label) },
                trailingIcon = { if (option == current) Icon(DeftActionIcons.Check, null, tint = colors.accentText) },
                onClick = {
                    close()
                    select(option)
                },
            )
        }
    }
}

@Composable
internal fun DisplayAction(
    grid: Boolean,
    noun: String,
    toggle: () -> Unit,
) {
    IconButton(toggle) {
        val view = if (grid) "list" else "grid"
        Icon(if (grid) DeftActionIcons.List else DeftActionIcons.Grid, "show $noun $view")
    }
}

internal val categorySortOptions = listOf("name", "created", "book count", "rating")
internal val authorSortOptions = listOf("name", "book count", "time", "rating")
internal val organizationBookSortOptions = listOf("title", "author", "date added", "rating", "file location")

private fun sortLabel(value: String): String = if (value == "created") "date created" else value
