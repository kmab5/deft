package com.kmab.deft.feature.library

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.kmab.deft.core.designsystem.DeftButton
import com.kmab.deft.core.designsystem.DeftButtonStyle
import com.kmab.deft.core.designsystem.DeftShapes
import com.kmab.deft.core.designsystem.DeftTypography
import com.kmab.deft.core.designsystem.LocalDeftColors
import com.kmab.deft.core.designsystem.icons.DeftActionIcons

@Composable
internal fun SelectionMark(modifier: Modifier = Modifier) {
    val c = LocalDeftColors.current
    Icon(
        DeftActionIcons.Check,
        "selected",
        modifier.background(c.surface, DeftShapes.small).padding(4.dp).size(20.dp),
        tint = c.accentText,
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun BooksSelectionBar(
    count: Int,
    onClear: () -> Unit,
    onAll: () -> Unit,
    onAdd: () -> Unit,
) {
    val c = LocalDeftColors.current
    FlowRow(Modifier.padding(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        IconButton(onClear) { Icon(DeftActionIcons.Close, "clear selection", tint = c.muted) }
        Text("$count selected", Modifier.padding(vertical = 14.dp), style = DeftTypography.label, color = c.text)
        DeftButton("select all", onAll, style = DeftButtonStyle.Text)
        DeftButton("add to category", onAdd, icon = DeftActionIcons.FolderPlus)
    }
}
