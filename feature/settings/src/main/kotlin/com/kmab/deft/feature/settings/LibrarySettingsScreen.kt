package com.kmab.deft.feature.settings

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.kmab.deft.core.database.entity.LibraryFolderEntity
import com.kmab.deft.core.datastore.LibrarySettings
import com.kmab.deft.core.model.BookFormat

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LibrarySettingsScreen(onBack: () -> Unit, viewModel: LibrarySettingsViewModel = hiltViewModel()) {
    val folders by viewModel.folders.folders.collectAsState(emptyList())
    val settings by viewModel.settings.settings.collectAsState(LibrarySettings())
    val error by viewModel.error.collectAsState()
    val scan by viewModel.scanStatus.collectAsState(com.kmab.deft.library.scanner.ScanStatus())
    var removing by remember { mutableStateOf<LibraryFolderEntity?>(null) }
    var exclusionRoot by remember { mutableStateOf<LibraryFolderEntity?>(null) }
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri ->
        if (uri != null) {
            val root = exclusionRoot
            if (root == null) viewModel.add(uri) else viewModel.exclude(root.treeUri, root.displayName, uri)
        }
        exclusionRoot = null
    }
    Scaffold(topBar = {
        TopAppBar(title = { Text("library") }, navigationIcon = {
            TextButton(onClick = onBack) { Text("back") }
        })
    }) { padding ->
        Column(Modifier.padding(padding).verticalScroll(rememberScrollState()).padding(16.dp)) {
            Text("folders", style = MaterialTheme.typography.titleLarge)
            folders.forEach { folder ->
                FolderRow(folder, viewModel, { removing = folder }) {
                    exclusionRoot = folder
                    picker.launch(Uri.parse(folder.treeUri))
                }
            }
            TextButton(onClick = { exclusionRoot = null; picker.launch(null) }) { Text("add a folder") }
            HorizontalDivider()
            Text("file types", style = MaterialTheme.typography.titleLarge)
            BookFormat.entries.forEach { format ->
                Row(Modifier.fillMaxWidth().heightIn(min = 48.dp)) {
                    Checkbox(checked = format in settings.enabledFormats,
                        onCheckedChange = { viewModel.format(format, it) })
                    Text(formatLabel(format), Modifier.padding(top = 12.dp))
                }
            }
            TextButton(onClick = { viewModel.scanNow() }, enabled = !scan.isScanning) { Text("rescan now") }
            if (scan.isScanning) LinearProgressIndicator(Modifier.fillMaxWidth())
            scan.message?.let { Text(it) }
            scan.report?.let { Text("added ${it.added} · missing ${it.markedMissing} · ${it.errors} errors") }
            error?.let { message ->
                Text(message, color = MaterialTheme.colorScheme.error)
                TextButton(onClick = { viewModel.error.value = null }) { Text("dismiss") }
            }
        }
    }
    removing?.let { folder ->
        AlertDialog(onDismissRequest = { removing = null }, title = { Text("remove folder?") },
            text = { Text("Books in this folder will be marked missing. Their history is kept.") },
            confirmButton = { TextButton(onClick = { viewModel.remove(folder.treeUri); removing = null }) { Text("remove") } },
            dismissButton = { TextButton(onClick = { removing = null }) { Text("cancel") } })
    }
}

@Composable
private fun FolderRow(folder: LibraryFolderEntity, viewModel: LibrarySettingsViewModel,
    onRemove: () -> Unit, onExclude: () -> Unit) {
    var expanded by remember(folder.treeUri) { mutableStateOf(false) }
    val exclusions by viewModel.folders.exclusions(folder.treeUri).collectAsState(emptyList())
    Row(Modifier.fillMaxWidth()) {
        TextButton(onClick = { expanded = !expanded }, modifier = Modifier.weight(1f)) { Text(folder.displayName) }
        TextButton(onClick = onRemove) { Text("remove") }
    }
    if (expanded) {
        exclusions.forEach { exclusion ->
            Row(Modifier.fillMaxWidth()) {
                Text(exclusion.displayPath, Modifier.weight(1f).padding(12.dp))
                TextButton(onClick = { viewModel.unexclude(folder.treeUri, exclusion.documentId) }) { Text("include") }
            }
        }
        TextButton(onClick = onExclude) { Text("exclude a subfolder") }
    }
}

internal fun formatLabel(format: BookFormat): String = when (format) {
    BookFormat.MOBI -> "MOBI/PRC"
    BookFormat.DJVU -> "DjVu"
    else -> format.name
}

