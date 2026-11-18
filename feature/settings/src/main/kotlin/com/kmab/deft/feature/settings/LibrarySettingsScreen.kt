package com.kmab.deft.feature.settings

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import com.kmab.deft.core.designsystem.DeftShapes
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
        DeftTopBar("library settings", actions = {
            DeftButton("back", onBack, style = DeftButtonStyle.Text)
        })
    }) { padding ->
        Column(
            Modifier.padding(padding).verticalScroll(rememberScrollState()).padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            LibraryScanPanel(scan, viewModel::scanNow)
            error?.let { message ->
                Column(Modifier.fillMaxWidth().background(colors.surface, DeftShapes.card).padding(16.dp)) {
                    Text(message, color = MaterialTheme.colorScheme.error, style = DeftTypography.body)
                    DeftButton("dismiss", viewModel::dismissError, style = DeftButtonStyle.Text)
                }
            }
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("your folders", style = DeftTypography.title)
                Text(
                    "Subfolders are included. Exclude any you don't want to scan.",
                    style = DeftTypography.body,
                    color = colors.muted,
                )
            }
            if (folders.isEmpty()) Text("Choose a folder to find your books.", style = DeftTypography.body)
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
            }, style = DeftButtonStyle.Secondary)
            HorizontalDivider(color = colors.line)
            FileTypes(settings, viewModel::format)
            HorizontalDivider(color = colors.line)
            LibraryStorageSection(scan.isScanning)
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
@OptIn(ExperimentalLayoutApi::class)
private fun FolderRow(
    folder: LibraryFolderEntity,
    viewModel: LibrarySettingsViewModel,
    onRemove: () -> Unit,
    onExclude: () -> Unit,
) {
    val colors = LocalDeftColors.current
    val exclusions by viewModel.folders.exclusions(folder.treeUri).collectAsState(emptyList())
    Column(
        Modifier.fillMaxWidth().background(colors.surface, DeftShapes.card).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(folder.displayName, style = DeftTypography.title)
        Text(
            if (exclusions.isEmpty()) "All subfolders included" else "${exclusions.size} excluded subfolders",
            style = DeftTypography.body,
            color = colors.muted,
        )
        exclusions.forEach { exclusion ->
            Row(Modifier.fillMaxWidth()) {
                Text(exclusion.displayPath, Modifier.weight(1f).padding(vertical = 12.dp), style = DeftTypography.body)
                DeftButton(
                    "include",
                    { viewModel.unexclude(folder.treeUri, exclusion.documentId) },
                    style = DeftButtonStyle.Text,
                )
            }
        }
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            DeftButton("exclude a subfolder", onExclude, style = DeftButtonStyle.Secondary)
            DeftButton("remove", onRemove, style = DeftButtonStyle.Text)
        }
    }
}

@Composable
private fun FileTypes(
    settings: LibrarySettings,
    onFormat: (BookFormat, Boolean) -> Unit,
) {
    val colors = LocalDeftColors.current
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text("file types", style = DeftTypography.title)
        Text(
            "${settings.enabledFormats.size} of ${BookFormat.entries.size} types enabled. " +
                "Unchecked types are not scanned.",
            style = DeftTypography.body,
            color = colors.muted,
        )
        BookFormat.entries.chunked(2).forEach { formats ->
            Row(Modifier.fillMaxWidth()) {
                formats.forEach { format ->
                    Row(
                        Modifier.weight(1f).heightIn(min = 48.dp).toggleable(
                            value = format in settings.enabledFormats,
                            role = Role.Checkbox,
                            onValueChange = { onFormat(format, it) },
                        ),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Checkbox(checked = format in settings.enabledFormats, onCheckedChange = null)
                        Text(formatLabel(format), Modifier.padding(start = 8.dp), style = DeftTypography.body)
                    }
                }
            }
        }
    }
}

@Composable
private fun LibraryScanPanel(
    scan: com.kmab.deft.library.scanner.ScanStatus,
    onScan: () -> Unit,
) {
    val colors = LocalDeftColors.current
    var details by rememberSaveable { mutableStateOf(false) }
    Column(
        Modifier.fillMaxWidth().background(colors.surface, DeftShapes.card).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(if (scan.isScanning) "scanning your folders" else "library scan", style = DeftTypography.title)
        if (scan.isScanning) {
            LinearProgressIndicator(Modifier.fillMaxWidth(), color = colors.accent, trackColor = colors.surface2)
            Text(scan.message ?: "Checking book files…", style = DeftTypography.body, color = colors.muted)
            ScanNotificationPermission()
        } else {
            scan.message?.let { Text(it, style = DeftTypography.body) }
            scan.report?.let {
                Text(it.summary(), style = DeftTypography.body)
                if (it.errors > 0) Text("${it.errors} issues need attention", style = DeftTypography.body)
                DeftButton(if (details) "hide scan details" else "show scan details", {
                    details = !details
                }, style = DeftButtonStyle.Text)
                if (details) {
                    it.details().forEach { detail ->
                        Text(detail, style = DeftTypography.body, color = colors.muted)
                    }
                }
            }
                ?: Text(
                    "Find new books and check your existing files.",
                    style = DeftTypography.body,
                    color = colors.muted,
                )
        }
        DeftButton("rescan now", onScan, enabled = !scan.isScanning)
    }
}

internal fun formatLabel(format: BookFormat): String =
    when (format) {
        BookFormat.MOBI -> "mobi/prc"
        else -> format.name.lowercase(Locale.ROOT)
    }
