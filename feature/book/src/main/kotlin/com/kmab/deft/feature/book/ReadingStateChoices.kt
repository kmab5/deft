package com.kmab.deft.feature.book

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.kmab.deft.core.designsystem.DeftColors
import com.kmab.deft.core.designsystem.DeftShapes
import com.kmab.deft.core.designsystem.DeftTypography
import com.kmab.deft.core.designsystem.LocalDeftColors
import com.kmab.deft.core.designsystem.icons.DeftActionIcons
import com.kmab.deft.core.model.BookState

private const val SELECTED_STATE_ALPHA = 0.1f

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun ReadingStateChoices(
    current: BookState,
    enabled: Boolean,
    onChoose: (BookState) -> Unit,
) {
    FlowRow(
        Modifier.selectableGroup(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        BookState.entries.forEach { state ->
            StateChoice(state, state == current, enabled) { onChoose(state) }
        }
    }
}

@Composable
private fun StateChoice(
    state: BookState,
    selected: Boolean,
    enabled: Boolean,
    onChoose: () -> Unit,
) {
    val c = LocalDeftColors.current
    val color = state.choiceColor(c)
    Surface(
        modifier =
            Modifier
                .heightIn(min = 48.dp)
                .selectable(selected, enabled = enabled, role = Role.RadioButton, onClick = onChoose)
                .semantics { contentDescription = "mark as ${state.appearance().label}" },
        shape = DeftShapes.small,
        color = if (selected) color.copy(alpha = SELECTED_STATE_ALPHA) else c.surface,
        contentColor = color,
        border = BorderStroke(1.dp, if (selected) color else c.line2),
    ) {
        Row(
            Modifier.padding(horizontal = 12.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Icon(state.choiceIcon(), null, Modifier.size(20.dp))
            Text(state.appearance().label, style = DeftTypography.label)
            if (selected) Icon(DeftActionIcons.Check, null, Modifier.size(16.dp))
        }
    }
}

private fun BookState.choiceColor(c: DeftColors): Color =
    when (this) {
        BookState.NOT_STARTED -> c.text
        BookState.READING -> c.accentText
        BookState.FINISHED -> c.finishedText
        BookState.PAUSED -> c.pausedText
        BookState.ABANDONED -> c.abandoned
    }

private fun BookState.choiceIcon(): ImageVector =
    when (this) {
        BookState.NOT_STARTED -> DeftActionIcons.NotRead
        BookState.READING -> DeftActionIcons.Reading
        BookState.FINISHED -> DeftActionIcons.Finished
        BookState.PAUSED -> DeftActionIcons.Paused
        BookState.ABANDONED -> DeftActionIcons.Abandoned
    }
