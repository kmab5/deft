package com.kmab.deft.feature.readersettings

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.kmab.deft.core.designsystem.DeftDialog
import com.kmab.deft.core.designsystem.DeftNoticeHost
import com.kmab.deft.core.designsystem.DeftNoticeKind
import com.kmab.deft.core.designsystem.DeftTopBar
import com.kmab.deft.core.designsystem.LocalDeftColors
import com.kmab.deft.core.designsystem.icons.DeftActionIcons
import com.kmab.deft.core.designsystem.rememberDeftNoticeState
import com.kmab.deft.core.model.ReaderFont
import com.kmab.deft.core.model.ReaderTheme

@Composable
fun ReaderSettingsScreen(onBack: () -> Unit) {
    val colors = LocalDeftColors.current
    Scaffold(containerColor = colors.bg, contentColor = colors.text, topBar = {
        DeftTopBar("reader", navigationIcon = {
            IconButton(onClick = onBack) { Icon(DeftActionIcons.Back, "back") }
        })
    }) { padding -> ReaderPreferencesPanel(Modifier.fillMaxSize().padding(padding), manageAssets = true) }
}

@Composable
fun ReaderPreferencesPanel(
    modifier: Modifier = Modifier,
    manageAssets: Boolean = false,
) {
    val viewModel: ReaderSettingsViewModel = hiltViewModel()
    val preferences by viewModel.preferences.collectAsStateWithLifecycle()
    val themes by viewModel.themes.collectAsStateWithLifecycle()
    val fonts by viewModel.fonts.collectAsStateWithLifecycle()
    val notices = rememberDeftNoticeState()
    var choosingFont by remember { mutableStateOf(false) }
    var editingTheme by remember { mutableStateOf(false) }
    var themeToEdit by remember { mutableStateOf<ReaderTheme?>(null) }
    var themeToDelete by remember { mutableStateOf<ReaderTheme?>(null) }
    var fontToDelete by remember { mutableStateOf<ReaderFont?>(null) }
    val picker =
        rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
            if (uri != null) viewModel.importFont(uri)
        }
    LaunchedEffect(viewModel) { viewModel.errors.collect { notices.show(it, DeftNoticeKind.Error) } }
    Box(modifier) {
        ReaderPreferencesContent(
            preferences,
            themes,
            fonts,
            viewModel::update,
            onChooseFont = { choosingFont = true },
            onEditTheme = {
                themeToEdit = it
                editingTheme = true
            },
            onDeleteTheme = { themeToDelete = it },
            manageAssets = manageAssets,
        )
        DeftNoticeHost(notices, Modifier.align(Alignment.BottomCenter))
    }
    if (choosingFont) {
        FontPicker(
            fonts,
            preferences.fontFamily,
            manageAssets,
            onSelect = { id ->
                viewModel.update { it.copy(fontFamily = id) }
                choosingFont = false
            },
            onImport = {
                choosingFont = false
                picker.launch(
                    arrayOf(
                        "font/*",
                        "application/x-font-ttf",
                        "application/x-font-opentype",
                        "application/octet-stream",
                    ),
                )
            },
            onRemove = {
                fontToDelete = it
                choosingFont = false
            },
            onDismiss = { choosingFont = false },
        )
    }
    if (editingTheme) {
        ReaderThemeEditor(
            themeToEdit,
            onSave = viewModel::saveTheme,
            onDismiss = { editingTheme = false },
        )
    }
    themeToDelete?.let { theme ->
        DeftDialog(
            title = "delete ${theme.name}?",
            body = "Readers using this theme will return to light or dark.",
            confirmLabel = "delete",
            onConfirm = {
                viewModel.deleteTheme(theme.id)
                themeToDelete = null
            },
            onDismiss = { themeToDelete = null },
        )
    }
    fontToDelete?.let { font ->
        DeftDialog(
            title = "remove ${font.name}?",
            body = "If selected, the reader will switch to Literata. The original font file stays untouched.",
            confirmLabel = "remove",
            onConfirm = {
                viewModel.removeFont(font.id)
                fontToDelete = null
            },
            onDismiss = { fontToDelete = null },
        )
    }
}
