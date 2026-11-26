package com.kmab.deft.feature.notes

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier

@Composable
internal fun BookmarkEditor(
    label: String,
    onSave: (String) -> Unit,
    onDismiss: () -> Unit,
    onDelete: () -> Unit,
    busy: Boolean,
) {
    var name by rememberSaveable { mutableStateOf(label) }
    NoteEditorSheet(
        title = "rename bookmark",
        busy = busy,
        onDismiss = onDismiss,
        onSave = { onSave(name.trim()) },
        onDelete = onDelete,
        canSave = name.isNotBlank(),
        deleteLabel = "delete bookmark",
    ) {
        OutlinedTextField(
            name,
            { name = it },
            Modifier.fillMaxWidth(),
            enabled = !busy,
            label = { Text("bookmark name") },
            singleLine = true,
        )
    }
}
