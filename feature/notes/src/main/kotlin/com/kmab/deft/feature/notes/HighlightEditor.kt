package com.kmab.deft.feature.notes

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SheetValue
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.unit.dp
import com.kmab.deft.core.designsystem.DeftButton
import com.kmab.deft.core.designsystem.DeftButtonStyle
import com.kmab.deft.core.designsystem.DeftDialog
import com.kmab.deft.core.designsystem.DeftNoticeHost
import com.kmab.deft.core.designsystem.DeftShapes
import com.kmab.deft.core.designsystem.DeftTypography
import com.kmab.deft.core.designsystem.LocalDeftColors
import com.kmab.deft.core.designsystem.LocalDeftNoticeState
import com.kmab.deft.core.model.HighlightColor
import com.kmab.deft.library.notes.HighlightDraft
import java.util.Locale

@Composable
fun HighlightEditor(
    draft: HighlightDraft,
    onSave: (HighlightDraft) -> Unit,
    onDismiss: () -> Unit,
    onDelete: (() -> Unit)? = null,
    busy: Boolean = false,
) {
    var note by rememberSaveable(draft.bookId, draft.locator) { mutableStateOf(draft.note.orEmpty()) }
    var underline by rememberSaveable(draft.bookId, draft.locator) { mutableStateOf(draft.underline) }
    var color by rememberSaveable(draft.bookId, draft.locator) { mutableStateOf(draft.color.name) }
    NoteEditorSheet(
        title = "highlight and note",
        busy = busy,
        onDismiss = onDismiss,
        onSave = {
            onSave(
                draft.copy(color = HighlightColor.valueOf(color), note = note.ifBlank { null }, underline = underline),
            )
        },
        onDelete = onDelete,
    ) {
        Text(draft.text, style = DeftTypography.body.copy(fontStyle = FontStyle.Italic))
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            HighlightColor.entries.forEach { choice ->
                val label = choice.label
                FilterChip(
                    selected = choice.name == color,
                    onClick = { color = choice.name },
                    label = { Text(label, color = choice.displayColor()) },
                    enabled = !busy,
                    colors = notesChipColors(),
                )
            }
        }
        FilterChip(
            selected = underline,
            onClick = { underline = !underline },
            label = { Text("underline") },
            enabled = !busy && color != HighlightColor.NONE.name,
            colors = notesChipColors(),
        )
        OutlinedTextField(
            note,
            { note = it },
            Modifier.fillMaxWidth(),
            enabled = !busy,
            label = { Text("note") },
            minLines = 2,
            maxLines = 6,
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun NoteEditorSheet(
    title: String,
    busy: Boolean,
    onDismiss: () -> Unit,
    onSave: () -> Unit,
    onDelete: (() -> Unit)?,
    canSave: Boolean = true,
    deleteLabel: String = "delete annotation",
    fields: @Composable ColumnScope.() -> Unit,
) {
    var confirm by rememberSaveable { mutableStateOf(false) }
    val latestBusy by rememberUpdatedState(busy)
    val colors = LocalDeftColors.current
    val notices = LocalDeftNoticeState.current
    val dismiss = { if (!latestBusy) onDismiss() }
    val sheetState =
        rememberModalBottomSheetState(
            skipPartiallyExpanded = true,
            confirmValueChange = { it != SheetValue.Hidden || !latestBusy },
        )
    ModalBottomSheet(
        onDismissRequest = dismiss,
        modifier = Modifier.imePadding(),
        sheetState = sheetState,
        sheetGesturesEnabled = !busy,
        containerColor = colors.surface,
        contentColor = colors.text,
        scrimColor = colors.scrim,
        shape = DeftShapes.sheet,
        dragHandle = null,
    ) {
        Box(Modifier.testTag("notes_editor_sheet")) {
            Column(Modifier.padding(horizontal = 20.dp, vertical = 12.dp)) {
                Column(
                    Modifier.weight(1f, fill = false).verticalScroll(rememberScrollState()).padding(bottom = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text(title, style = DeftTypography.headline)
                    fields()
                }
                FlowRow(
                    Modifier.align(Alignment.End),
                    horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End),
                ) {
                    if (onDelete != null) {
                        DeftButton("delete", { confirm = true }, style = DeftButtonStyle.Text, enabled = !busy)
                    }
                    DeftButton("cancel", dismiss, style = DeftButtonStyle.Text, enabled = !busy)
                    DeftButton("save", onSave, enabled = !busy && canSave)
                }
            }
            if (notices != null) DeftNoticeHost(notices, Modifier.align(Alignment.BottomCenter))
        }
    }
    if (confirm && onDelete != null) {
        DeftDialog(
            "delete this entry?",
            "This entry will be removed from your notes.",
            deleteLabel,
            {
                confirm = false
                if (!latestBusy) onDelete()
            },
            { confirm = false },
            dismissLabel = "keep",
        )
    }
}

internal val HighlightColor.label: String get() = name.lowercase(Locale.ROOT)

@Composable
internal fun notesChipColors() =
    LocalDeftColors.current.let { colors ->
        FilterChipDefaults.filterChipColors(
            containerColor = colors.surface,
            labelColor = colors.text,
            selectedContainerColor = colors.surface2,
            selectedLabelColor = colors.accentText,
        )
    }

@Composable
internal fun HighlightColor.displayColor(): Color {
    val colors = LocalDeftColors.current
    return when (this) {
        HighlightColor.PURPLE ->
            Color(if (colors.surface.luminance() > LIGHT_SURFACE_LUMINANCE) PURPLE_ON_LIGHT else PURPLE_ON_DARK)
        HighlightColor.GREEN -> colors.finishedText
        HighlightColor.AMBER -> colors.pausedText
        HighlightColor.GRAY -> colors.muted
        HighlightColor.NONE -> colors.text
    }
}

@Composable
internal fun HighlightColor.ruleColor(): Color =
    when (this) {
        HighlightColor.GRAY, HighlightColor.NONE -> LocalDeftColors.current.line2
        else -> displayColor()
    }

private const val LIGHT_SURFACE_LUMINANCE = 0.5f
private const val PURPLE_ON_LIGHT = 0xFF7436A4
private const val PURPLE_ON_DARK = 0xFFC38AF1
