package com.kmab.deft.feature.organization

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.kmab.deft.core.designsystem.DeftButton
import com.kmab.deft.core.designsystem.DeftButtonStyle
import com.kmab.deft.core.designsystem.DeftShapes
import com.kmab.deft.core.designsystem.LocalDeftColors
import com.kmab.deft.core.designsystem.icons.DeftActionIcons
import com.kmab.deft.core.model.BookState

@Composable
internal fun BrowseChoice(
    label: String,
    options: List<String>,
    selected: String,
    onSelect: (String) -> Unit,
    icon: ImageVector = DeftActionIcons.Settings,
) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        DeftButton(label, { expanded = true }, style = DeftButtonStyle.Secondary, icon = icon)
        DropdownMenu(
            expanded,
            {
                expanded = false
            },
            shape = DeftShapes.card,
            containerColor = LocalDeftColors.current.surface,
            border =
                BorderStroke(
                    1.dp,
                    LocalDeftColors.current.line,
                ),
        ) {
            options.forEach { option ->
                DropdownMenuItem(text = { Text(option) }, leadingIcon = {
                    if (option ==
                        selected
                    ) {
                        Icon(DeftActionIcons.Check, null)
                    }
                }, onClick = {
                    expanded = false
                    onSelect(option)
                })
            }
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
) {
    Row {
        BrowseChoice(sortLabel(sort), options.map(::sortLabel), sortLabel(sort), {
            select(
                if (it ==
                    "date created"
                ) {
                    "created"
                } else {
                    it
                },
            )
        }, DeftActionIcons.Contents)
        IconButton(
            toggle,
        ) { Icon(if (reverse) DeftActionIcons.Descending else DeftActionIcons.Ascending, "reverse sort") }
    }
}

@Composable
internal fun BrowseState(
    state: BookState?,
    change: (BookState?) -> Unit,
) {
    val options = listOf("all states") + BookState.entries.map { it.readingLabel() }
    BrowseChoice(state?.readingLabel() ?: "all states", options, state?.readingLabel() ?: "all states", { label ->
        change(
            BookState.entries.find {
                it.readingLabel() ==
                    label
            },
        )
    })
}

internal val categorySortOptions = listOf("name", "created", "book count", "rating")
internal val authorSortOptions = listOf("name", "book count", "time", "rating")
internal val organizationBookSortOptions = listOf("title", "author", "date added", "rating", "file location")

private fun sortLabel(value: String): String = if (value == "created") "date created" else value
