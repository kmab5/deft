package com.kmab.deft.feature.library

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.kmab.deft.core.designsystem.DeftMotion
import com.kmab.deft.core.designsystem.DeftShapes
import com.kmab.deft.core.designsystem.DeftTypography
import com.kmab.deft.core.designsystem.LocalDeftColors
import com.kmab.deft.core.designsystem.icons.DeftIcons
import com.kmab.deft.core.model.BookDisplaySize
import com.kmab.deft.core.model.BookFormat
import com.kmab.deft.core.model.BookSort
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

@Composable
internal fun BooksToolbar(
    state: BooksUiState,
    onFilters: () -> Unit,
    onSort: () -> Unit,
    onDisplay: () -> Unit,
) {
    val c = LocalDeftColors.current
    val ascending =
        if (state.sort == BookSort.TITLE ||
            state.sort == BookSort.AUTHOR
        ) {
            !state.reverseSort
        } else {
            state.reverseSort
        }
    val active = (if (state.formatFilter.size != BookFormat.entries.size) 1 else 0) + (if (state.showMissing) 1 else 0)
    Row(Modifier.fillMaxWidth().padding(start = 20.dp, end = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        Row(
            Modifier
                .weight(1f)
                .heightIn(min = 48.dp)
                .clickable(role = Role.Button, onClick = onFilters)
                .semantics { contentDescription = "filter books" },
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(DeftIcons.Filter, null, Modifier.size(20.dp), tint = c.muted)
            Spacer(Modifier.width(8.dp))
            Text(
                if (active == 0) "all books" else "filtered · $active",
                style = DeftTypography.label,
                color = c.text,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Row(
            Modifier
                .heightIn(min = 48.dp)
                .clickable(role = Role.Button, onClick = onSort)
                .padding(horizontal = 8.dp)
                .semantics { contentDescription = "sort books" },
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(state.sort.label(), style = DeftTypography.label, color = c.muted)
            Icon(
                if (ascending) LibraryIcons.Ascending else LibraryIcons.Descending,
                state.sort.orderLabel(state.reverseSort),
                Modifier.size(16.dp),
                tint = c.muted,
            )
        }
        IconButton(onDisplay) {
            Icon(if (state.gridView) LibraryIcons.Grid else LibraryIcons.List, "display options", tint = c.muted)
        }
    }
}

/** Material's sheet animates entry; explicit close also waits for its exit before removing it. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun LibraryPopup(
    title: String,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    val c = LocalDeftColors.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        modifier = modifier.testTag("books_sheet"),
        shape = DeftShapes.sheet,
        containerColor = c.surface,
        contentColor = c.text,
        scrimColor = c.scrim,
        tonalElevation = 0.dp,
        dragHandle = null,
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .animateContentSize()
                .padding(horizontal = 16.dp)
                .padding(bottom = 12.dp),
        ) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(
                    title,
                    style = DeftTypography.title,
                    color = c.text,
                    modifier = Modifier.weight(1f).semantics { heading() },
                )
                IconButton({
                    scope.launch {
                        sheetState.hide()
                        onDismiss()
                    }
                }) {
                    Icon(LibraryIcons.Close, "close popup", tint = c.muted)
                }
            }
            content()
        }
    }
}

@Composable
internal fun BooksFilters(
    state: BooksUiState,
    onDismiss: () -> Unit,
    onShowMissingChange: (Boolean) -> Unit,
    onFormatChange: (BookFormat, Boolean) -> Unit,
) {
    val c = LocalDeftColors.current
    LibraryPopup("filter books", onDismiss) {
        Column(Modifier.verticalScroll(rememberScrollState())) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text("file types", style = DeftTypography.label, color = c.muted, modifier = Modifier.weight(1f))
                Text(
                    "reset",
                    style = DeftTypography.label,
                    color = c.accentText,
                    modifier =
                        Modifier
                            .heightIn(min = 48.dp)
                            .clickable(role = Role.Button) {
                                BookFormat.entries.forEach { onFormatChange(it, true) }
                                onShowMissingChange(false)
                            }.padding(vertical = 14.dp, horizontal = 8.dp),
                )
            }
            BookFormat.entries.chunked(FORMATS_PER_ROW).forEach { formats ->
                Row(Modifier.fillMaxWidth()) {
                    formats.forEach { format ->
                        CheckOption(
                            format.displayName.lowercase(),
                            format in state.formatFilter,
                            { onFormatChange(format, it) },
                            Modifier.weight(1f),
                        )
                    }
                }
            }
            HorizontalDivider(color = c.line)
            CheckOption(
                "show missing",
                state.showMissing,
                onShowMissingChange,
                Modifier.fillMaxWidth(),
                LibraryIcons.Missing,
            )
        }
    }
}

@Composable
private fun CheckOption(
    label: String,
    checked: Boolean,
    onChecked: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
) {
    val c = LocalDeftColors.current
    Row(
        modifier
            .heightIn(min = 48.dp)
            .toggleable(checked, role = Role.Checkbox, onValueChange = onChecked)
            .semantics { contentDescription = label },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Checkbox(checked, null, Modifier.size(24.dp))
        if (icon != null) Icon(icon, null, Modifier.size(18.dp), tint = c.muted)
        Text(label, style = DeftTypography.label, color = c.text)
    }
}

@Composable
internal fun BooksSort(
    state: BooksUiState,
    onDismiss: () -> Unit,
    onSortChange: (BookSort) -> Unit,
    onReverseSortChange: (Boolean) -> Unit,
) {
    val c = LocalDeftColors.current
    LibraryPopup("sort books", onDismiss) {
        Column(Modifier.verticalScroll(rememberScrollState()).animateContentSize()) {
            Column(Modifier.selectableGroup()) {
                BookSort.entries.forEach { sort ->
                    val icon =
                        when (sort) {
                            BookSort.TITLE -> LibraryIcons.Title
                            BookSort.AUTHOR -> DeftIcons.Authors
                            BookSort.DATE_ADDED -> LibraryIcons.Added
                            BookSort.LAST_READ -> LibraryIcons.History
                        }
                    RadioOption(sort.label(), state.sort == sort, { onSortChange(sort) }, icon)
                }
            }
            HorizontalDivider(color = c.line)
            Row(Modifier.fillMaxWidth().selectableGroup(), horizontalArrangement = Arrangement.spacedBy(24.dp)) {
                listOf(false, true).forEach { reverse ->
                    RadioOption(
                        state.sort.orderLabel(reverse),
                        state.reverseSort == reverse,
                        { onReverseSortChange(reverse) },
                        null,
                        Modifier.weight(1f),
                    )
                }
            }
            if (state.sort == BookSort.LAST_READ) {
                Text(
                    "Books with no reading history stay at the end.",
                    style = DeftTypography.cardTitle,
                    color = c.muted,
                    modifier = Modifier.padding(bottom = 8.dp),
                )
            }
        }
    }
}

@Composable
private fun RadioOption(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    icon: ImageVector?,
    modifier: Modifier = Modifier,
) {
    val c = LocalDeftColors.current
    Row(
        modifier.fillMaxWidth().heightIn(min = 48.dp).selectable(selected, role = Role.RadioButton, onClick = onClick),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        if (icon != null) Icon(icon, null, Modifier.size(20.dp), tint = c.muted)
        Text(label, style = DeftTypography.label, color = c.text, modifier = Modifier.weight(1f))
        RadioButton(selected, null, Modifier.size(24.dp))
    }
}

@Composable
internal fun BooksDisplay(
    state: BooksUiState,
    onDismiss: () -> Unit,
    onGridViewChange: (Boolean) -> Unit,
    onGridSizeChange: (BookDisplaySize) -> Unit,
    onListSizeChange: (BookDisplaySize) -> Unit,
) {
    val c = LocalDeftColors.current
    val size = if (state.gridView) state.gridSize else state.listSize
    val changeSize = if (state.gridView) onGridSizeChange else onListSizeChange
    LibraryPopup("display", onDismiss) {
        Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
                Modifier
                    .fillMaxWidth()
                    .border(1.dp, c.line2, DeftShapes.small)
                    .clip(DeftShapes.small)
                    .selectableGroup(),
            ) {
                LayoutOption("grid", LibraryIcons.Grid, state.gridView, { onGridViewChange(true) }, Modifier.weight(1f))
                LayoutOption(
                    "list",
                    LibraryIcons.List,
                    !state.gridView,
                    { onGridViewChange(false) },
                    Modifier.weight(1f),
                )
            }
            Text(if (state.gridView) "grid size" else "list size", style = DeftTypography.label, color = c.muted)
            Slider(
                value = size.ordinal.toFloat(),
                onValueChange = {
                    val selected = BookDisplaySize.entries[it.roundToInt()]
                    if (selected != size) changeSize(selected)
                },
                valueRange = 0f..2f,
                steps = 1,
                modifier =
                    Modifier.fillMaxWidth().semantics {
                        contentDescription = if (state.gridView) "grid size" else "list size"
                        stateDescription = size.name.lowercase(java.util.Locale.ROOT)
                    },
            )
            Row(Modifier.fillMaxWidth().selectableGroup()) {
                BookDisplaySize.entries.forEach { option ->
                    Text(
                        option.name.lowercase(java.util.Locale.ROOT),
                        style = DeftTypography.label,
                        color = if (option == size) c.accentText else c.muted,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                        modifier =
                            Modifier
                                .weight(1f)
                                .heightIn(min = 48.dp)
                                .selectable(
                                    option == size,
                                    role = Role.RadioButton,
                                ) { changeSize(option) }
                                .padding(vertical = 12.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun LayoutOption(
    label: String,
    icon: ImageVector,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier,
) {
    val c = LocalDeftColors.current
    val background by animateColorAsState(if (selected) c.surface2 else Color.Transparent, tween(DeftMotion.SHORT))
    val foreground by animateColorAsState(if (selected) c.accentText else c.muted, tween(DeftMotion.SHORT))
    Row(
        modifier
            .background(
                background,
            ).heightIn(min = 48.dp)
            .selectable(selected, role = Role.RadioButton, onClick = onClick),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, null, Modifier.size(20.dp), tint = foreground)
        Spacer(Modifier.width(8.dp))
        Text(label, style = DeftTypography.label, color = foreground)
    }
}

private const val FORMATS_PER_ROW = 3
