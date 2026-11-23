package com.kmab.deft.feature.organization

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.kmab.deft.core.designsystem.DeftShapes
import com.kmab.deft.core.designsystem.DeftTypography
import com.kmab.deft.core.designsystem.LocalDeftColors
import com.kmab.deft.core.designsystem.icons.DeftActionIcons

internal enum class CategoryBulkAction(
    val label: String,
) {
    ADD_TYPES("add types"),
    REMOVE_TYPES("remove types"),
    MERGE("merge categories"),
    DELETE("delete categories"),
}

@Composable
internal fun CategorySelectionBar(
    count: Int,
    busy: Boolean,
    clear: () -> Unit,
    selectAll: () -> Unit,
    act: (CategoryBulkAction) -> Unit,
) {
    val colors = LocalDeftColors.current
    var menu by remember { mutableStateOf(false) }
    Row(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .background(colors.surface2, DeftShapes.card)
            .testTag("category_selection_bar"),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(clear, enabled = !busy) { Icon(DeftActionIcons.Close, "clear selection") }
        Text(
            count.toString(),
            Modifier.weight(1f).semantics { contentDescription = "$count selected" },
            style = DeftTypography.label,
        )
        IconButton(
            selectAll,
            enabled = !busy,
        ) { Icon(DeftActionIcons.Finished, "select all", tint = colors.accentText) }
        Box {
            IconButton({ menu = true }, enabled = !busy) { Icon(DeftActionIcons.More, "category selection actions") }
            DropdownMenu(menu, { menu = false }, containerColor = colors.surface, shape = DeftShapes.card) {
                CategoryBulkAction.entries.forEach { action ->
                    DropdownMenuItem(
                        text = { Text(action.label) },
                        leadingIcon = {
                            Icon(
                                when (action) {
                                    CategoryBulkAction.ADD_TYPES -> DeftActionIcons.FolderPlus
                                    CategoryBulkAction.REMOVE_TYPES -> DeftActionIcons.FolderMinus
                                    CategoryBulkAction.MERGE -> DeftActionIcons.Folder
                                    CategoryBulkAction.DELETE -> DeftActionIcons.Trash
                                },
                                null,
                            )
                        },
                        enabled = !busy,
                        onClick = {
                            menu = false
                            act(action)
                        },
                    )
                }
            }
        }
    }
}
