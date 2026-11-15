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
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Checkbox
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.kmab.deft.core.database.entity.LibraryFolderEntity
import com.kmab.deft.core.datastore.LibrarySettings
import com.kmab.deft.core.designsystem.DeftButton
import com.kmab.deft.core.designsystem.DeftButtonStyle
import com.kmab.deft.core.designsystem.DeftDialog
import com.kmab.deft.core.designsystem.DeftTopBar
import com.kmab.deft.core.designsystem.DeftTypography
import com.kmab.deft.core.designsystem.LocalDeftColors
import com.kmab.deft.core.model.BookFormat
import java.util.Locale

@Composable
fun LibrarySettingsScreen(
    onBack: () -> Unit,
    viewModel: LibrarySettingsViewModel = hiltViewModel(),
) {
    val colors = LocalDeftColors.current
    val folders by viewModel.folders.folders.collectAsState(emptyList())
    val settings by viewModel.settings.settings.collectAsState(LibrarySettings())
    val error by viewModel.error.collectAsState()
    val scan by viewModel.scanStatus.collectAsState(
        com.kmab.deft.library.scanner
            .ScanStatus(),
    )
    var removing by remember { mutableStateOf<LibraryFolderEntity?>(null) }
    var exclusionRootUri by rememberSaveable { mutableStateOf<String?>(null) }
    var exclusionRootName by rememberSaveable { mutableStateOf<String?>(null) }
    val picker =
        rememberLauncherForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri ->
            if (uri != null) {
                val root = exclusionRootUri
                if (root == null) viewModel.add(uri) else viewModel.exclude(root, exclusionRootName.orEmpty(), uri)
            }
            exclusionRootUri = null
            exclusionRootName = null
        }
    Scaffold(containerColor = colors.bg, contentColor = colors.text, topBar = {
        DeftTopBar("library", actions = {
            DeftButton("back", onBack, style = DeftButtonStyle.Text)
        })
    }) { padding ->
        Column(Modifier.padding(padding).verticalScroll(rememberScrollState()).padding(20.dp)) {
            Text("folders", style = DeftTypography.monoSmall, color = colors.muted)
            if (folders.isEmpty()) Text("Choose a folder to find your books.", Modifier.padding(vertical = 12.dp))
            folders.forEach { folder ->
                FolderRow(folder, viewModel, { removing = folder }) {
                    exclusionRootUri = folder.treeUri
                    exclusionRootName = folder.displayName
                    picker.launch(Uri.parse(folder.treeUri))
                }
            }
            DeftButton("add a folder", {
                exclusionRootUri = null
                exclusionRootName = null
                picker.launch(null)
            }, style = DeftButtonStyle.Text)
            HorizontalDivider(color = colors.line, modifier = Modifier.padding(vertical = 16.dp))
            Text("file types", style = DeftTypography.monoSmall, color = colors.muted)
            BookFormat.entries.forEach { format ->
                Row(
                    Modifier.fillMaxWidth().heightIn(min = 48.dp).toggleable(
                        value = format in settings.enabledFormats,
                        role = Role.Checkbox,
                        onValueChange = { viewModel.format(format, it) },
                    ),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Checkbox(
                        checked = format in settings.enabledFormats,
                        onCheckedChange = null,
                    )
                    Text(formatLabel(format), Modifier.padding(start = 12.dp))
                }
            }
            DeftButton("rescan now", { viewModel.scanNow() }, enabled = !scan.isScanning)
            if (scan.isScanning) LinearProgressIndicator(Modifier.fillMaxWidth())
            scan.message?.let { Text(it) }
            scan.report?.let { Text("added ${it.added} · missing ${it.markedMissing} · ${it.errors} errors") }
            error?.let { message ->
                Text(message, color = MaterialTheme.colorScheme.error)
                DeftButton("dismiss", viewModel::dismissError, style = DeftButtonStyle.Text)
            }
        }
    }
    removing?.let { folder ->
        DeftDialog(
            title = "remove this folder?",
            body = "Its books will be marked missing. Their history stays.",
            confirmLabel = "remove",
            onConfirm = {
                viewModel.remove(folder.treeUri)
                removing = null
            },
            onDismiss = { removing = null },
        )
    }
}

@Composable
private fun FolderRow(
    folder: LibraryFolderEntity,
    viewModel: LibrarySettingsViewModel,
    onRemove: () -> Unit,
    onExclude: () -> Unit,
) {
    var expanded by remember(folder.treeUri) { mutableStateOf(false) }
    val exclusions by viewModel.folders.exclusions(folder.treeUri).collectAsState(emptyList())
    Row(Modifier.fillMaxWidth()) {
        DeftButton(folder.displayName, { expanded = !expanded }, Modifier.weight(1f), style = DeftButtonStyle.Text)
        DeftButton("remove", onRemove, style = DeftButtonStyle.Text)
    }
    if (expanded) {
        exclusions.forEach { exclusion ->
            Row(Modifier.fillMaxWidth()) {
                Text(exclusion.displayPath, Modifier.weight(1f).padding(12.dp))
                DeftButton(
                    "include",
                    { viewModel.unexclude(folder.treeUri, exclusion.documentId) },
                    style = DeftButtonStyle.Text,
                )
            }
        }
        DeftButton("exclude a subfolder", onExclude, style = DeftButtonStyle.Text)
    }
}

internal fun formatLabel(format: BookFormat): String =
    when (format) {
        BookFormat.MOBI -> "mobi/prc"
        else -> format.name.lowercase(Locale.ROOT)
    }
