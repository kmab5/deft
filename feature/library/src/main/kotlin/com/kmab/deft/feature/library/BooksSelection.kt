package com.kmab.deft.feature.library

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.kmab.deft.core.designsystem.DeftButton
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

@Composable
internal fun BooksSelectionBar(
    count: Int,
    onClear: () -> Unit,
    onAll: () -> Unit,
    onAdd: () -> Unit,
) {
    val c = LocalDeftColors.current
    Row(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 8.dp)
            .background(c.surface2, DeftShapes.card)
            .padding(end = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        IconButton(onClear) { Icon(DeftActionIcons.Close, "clear selection", tint = c.muted) }
        Text(
            count.toString(),
            Modifier.weight(1f).semantics { contentDescription = "$count selected" },
            style = DeftTypography.label,
            color = c.text,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        IconButton(onAll) { Icon(DeftActionIcons.Finished, "select all", tint = c.accentText) }
        DeftButton(
            "add",
            onAdd,
            icon = DeftActionIcons.FolderPlus,
            modifier = Modifier.heightIn(min = 48.dp).semantics { contentDescription = "add to category" },
        )
    }
}
