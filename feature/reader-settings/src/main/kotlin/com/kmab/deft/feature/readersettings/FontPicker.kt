package com.kmab.deft.feature.readersettings

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.selectable
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.Typeface
import androidx.compose.ui.unit.dp
import com.kmab.deft.core.designsystem.DeftShapes
import com.kmab.deft.core.designsystem.DeftTypography
import com.kmab.deft.core.designsystem.LocalDeftColors
import com.kmab.deft.core.designsystem.icons.DeftActionIcons
import com.kmab.deft.core.model.ReaderFont
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun FontPicker(
    fonts: List<ReaderFont>,
    selected: String,
    manage: Boolean,
    onSelect: (String) -> Unit,
    onImport: () -> Unit,
    onRemove: (ReaderFont) -> Unit,
    onDismiss: () -> Unit,
) {
    val state = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()

    fun close(action: () -> Unit) {
        scope.launch {
            state.hide()
            action()
        }
    }
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = state,
        shape = DeftShapes.sheet,
        containerColor = LocalDeftColors.current.surface,
    ) {
        Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text("font", Modifier.weight(1f), style = DeftTypography.title)
                IconButton(onClick = { close(onDismiss) }) { Icon(DeftActionIcons.Close, "close fonts") }
            }
            if (manage) {
                TextButton(onClick = { close(onImport) }, modifier = Modifier.heightIn(min = 48.dp)) {
                    Text("+ import TTF or OTF", color = LocalDeftColors.current.accentText)
                }
            }
            LazyColumn(Modifier.heightIn(max = 520.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(fonts, key = { it.id }) { font ->
                    FontRow(
                        font,
                        font.id == selected,
                        { close { onSelect(font.id) } },
                        if (manage && font.imported) ({ close { onRemove(font) } }) else null,
                    )
                }
            }
        }
    }
}

@Composable
private fun FontRow(
    font: ReaderFont,
    selected: Boolean,
    select: () -> Unit,
    remove: (() -> Unit)?,
) {
    val context = LocalContext.current
    val family by produceState<FontFamily?>(null, font.id) {
        value =
            withContext(Dispatchers.IO) {
                try {
                    val native =
                        ReaderFontCatalog.previewAsset(font.id)?.let {
                            android.graphics.Typeface.createFromAsset(context.assets, it)
                        } ?: font.faces
                            .firstOrNull()
                            ?.privateFilePath
                            ?.let { android.graphics.Typeface.createFromFile(it) }
                    native?.let { FontFamily(Typeface(it)) }
                } catch (_: RuntimeException) {
                    null
                }
            }
    }
    val colors = LocalDeftColors.current
    Surface(
        shape = DeftShapes.small,
        color = if (selected) colors.surface2 else colors.surface,
        border = BorderStroke(1.dp, if (selected) colors.accentText else colors.line),
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .heightIn(min = 56.dp)
                .selectable(selected, role = Role.RadioButton, onClick = select)
                .padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            RadioButton(selected, onClick = null)
            Text(
                font.name,
                Modifier.weight(1f).padding(start = 8.dp),
                style = DeftTypography.title,
                fontFamily = family,
            )
            if (remove != null) IconButton(onClick = remove) { Icon(DeftActionIcons.Trash, "remove ${font.name}") }
        }
    }
}
