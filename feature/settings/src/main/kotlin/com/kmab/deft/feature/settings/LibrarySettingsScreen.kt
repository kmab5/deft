package com.kmab.deft.feature.settings

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import com.kmab.deft.core.designsystem.DeftNoticeKind
import com.kmab.deft.core.designsystem.DeftShapes
import com.kmab.deft.core.designsystem.DeftTopBar
import com.kmab.deft.core.designsystem.DeftTypography
import com.kmab.deft.core.designsystem.LocalDeftColors
import com.kmab.deft.core.designsystem.LocalDeftNoticeState
import com.kmab.deft.core.designsystem.icons.DeftActionIcons

@Composable
fun LibrarySettingsScreen(
    onBack: () -> Unit,
    viewModel: LibrarySettingsViewModel = hiltViewModel(),
    onScanDetails: () -> Unit = {},
    onIgnoredBooks: () -> Unit = {},
) {
    val colors = LocalDeftColors.current
    val folders by viewModel.folders.folders.collectAsState(emptyList())
    val settings by viewModel.settings.settings.collectAsState(LibrarySettings())
    val error by viewModel.error.collectAsState()
    val notices = LocalDeftNoticeState.current
    LaunchedEffect(error, notices) {
        error?.let { message ->
            if (notices != null) {
                notices.show(message, DeftNoticeKind.Error)
                viewModel.dismissError()
            }
        }
    }
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
        DeftTopBar("library settings", navigationIcon = {
            IconButton(onClick = onBack) { Icon(DeftActionIcons.Back, "back") }
        })
    }) { padding ->
        Column(
            Modifier.padding(padding).verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp),
        ) {
            LibraryScanPanel(scan, viewModel::scanNow, onScanDetails)
            LibraryFolders(
                folders,
                viewModel,
                onAdd = {
                    exclusionRootUri = null
                    exclusionRootName = null
                    picker.launch(null)
                },
                onRemove = { removing = it },
                onExclude = { folder ->
                    exclusionRootUri = folder.treeUri
                    exclusionRootName = folder.displayName
                    picker.launch(Uri.parse(folder.treeUri))
                },
            )
            LibraryFileTypes(settings, viewModel::format)
            Row(
                Modifier.fillMaxWidth().toggleable(
                    value = settings.advancedSearch,
                    role = Role.Switch,
                    onValueChange = viewModel::advancedSearch,
                ),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(Modifier.weight(1f).padding(end = 12.dp)) {
                    Text("advanced search", style = DeftTypography.title)
                    Text("Case, whole words and regex in your library and books.", style = DeftTypography.body)
                }
                androidx.compose.material3.Switch(settings.advancedSearch, onCheckedChange = null)
            }
            IgnoredBooksEntry(onIgnoredBooks)
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
private fun LibraryFolders(
    folders: List<LibraryFolderEntity>,
    viewModel: LibrarySettingsViewModel,
    onAdd: () -> Unit,
    onRemove: (LibraryFolderEntity) -> Unit,
    onExclude: (LibraryFolderEntity) -> Unit,
) {
    val colors = LocalDeftColors.current
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text("your folders", Modifier.weight(1f), style = DeftTypography.title)
                IconButton(onClick = onAdd) {
                    Icon(DeftActionIcons.FolderPlus, "add a folder", tint = colors.accentText)
                }
            }
            Text(
                "Subfolders are included. Exclude any you don't want to scan.",
                style = DeftTypography.body,
                color = colors.muted,
            )
        }
        if (folders.isEmpty()) {
            SettingsPanel {
                Text("Choose a folder to find your books.", style = DeftTypography.body, color = colors.muted)
                DeftButton("add a folder", onAdd, style = DeftButtonStyle.Secondary, icon = DeftActionIcons.FolderPlus)
            }
        } else {
            Column(Modifier.animateContentSize(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                folders.forEach { folder ->
                    FolderRow(folder, viewModel, { onRemove(folder) }, { onExclude(folder) })
                }
            }
        }
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
    SettingsPanel {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Icon(DeftActionIcons.Folder, null, Modifier.size(24.dp), tint = colors.muted)
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(folder.displayName, style = DeftTypography.title)
                Text(
                    if (exclusions.isEmpty()) "All subfolders included" else "${exclusions.size} excluded subfolders",
                    style = DeftTypography.body,
                    color = colors.muted,
                )
            }
        }
        exclusions.forEach { exclusion ->
            HorizontalDivider(color = colors.line)
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(
                    exclusion.displayPath,
                    Modifier.weight(1f).padding(end = 8.dp, top = 8.dp, bottom = 8.dp),
                    style = DeftTypography.body,
                )
                DeftButton(
                    "include",
                    { viewModel.unexclude(folder.treeUri, exclusion.documentId) },
                    style = DeftButtonStyle.Text,
                    icon = DeftActionIcons.FolderPlus,
                )
            }
        }
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            DeftButton(
                "exclude a subfolder",
                onExclude,
                style = DeftButtonStyle.Secondary,
                icon = DeftActionIcons.FolderMinus,
            )
            DeftButton("remove", onRemove, style = DeftButtonStyle.Text, icon = DeftActionIcons.Trash)
        }
    }
}

@Composable
private fun IgnoredBooksEntry(onClick: () -> Unit) {
    val colors = LocalDeftColors.current
    Surface(
        shape = DeftShapes.card,
        color = colors.surface,
        border = BorderStroke(1.dp, colors.line),
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .clickable(role = Role.Button, onClick = onClick)
                .heightIn(min = 72.dp)
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Icon(DeftActionIcons.FolderMinus, null, tint = colors.muted)
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("ignored books", style = DeftTypography.label, color = colors.text)
                Text("Hidden from your library. History stays.", style = DeftTypography.body, color = colors.muted)
            }
            Icon(DeftActionIcons.ChevronRight, null, tint = colors.muted)
        }
    }
}
