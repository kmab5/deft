package com.kmab.deft.feature.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.kmab.deft.core.designsystem.DeftNoticeKind
import com.kmab.deft.core.designsystem.DeftSettingsTopBar
import com.kmab.deft.core.designsystem.DeftSwitch
import com.kmab.deft.core.designsystem.DeftTypography
import com.kmab.deft.core.designsystem.LocalDeftColors
import com.kmab.deft.core.designsystem.LocalDeftNoticeState
import com.kmab.deft.core.designsystem.icons.DeftActionIcons
import com.kmab.deft.core.model.AppTheme

@Composable
fun AppearanceSettingsScreen(
    onBack: () -> Unit,
    viewModel: AppearanceSettingsViewModel = hiltViewModel(),
) {
    val theme = viewModel.theme.collectAsState(initial = null).value
    val celebrations = viewModel.celebrations.collectAsState(initial = null).value
    val error by viewModel.error.collectAsState()
    val notices = LocalDeftNoticeState.current
    LaunchedEffect(error) {
        error?.let {
            notices?.show(it, DeftNoticeKind.Error)
            viewModel.error.value = null
        }
    }
    if (theme == null || celebrations == null) {
        SettingsPage("appearance", onBack) {}
    } else {
        AppearanceSettingsContent(theme, celebrations, onBack, viewModel::setTheme, viewModel::setCelebrations)
    }
}

@Composable
internal fun AppearanceSettingsContent(
    theme: AppTheme,
    celebrations: Boolean,
    onBack: () -> Unit,
    onTheme: (AppTheme) -> Unit,
    onCelebrations: (Boolean) -> Unit,
) {
    val colors = LocalDeftColors.current
    SettingsPage("appearance", onBack) {
        SettingsPanel {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("app theme", style = DeftTypography.title)
                Text(
                    "System follows Android’s appearance. Reader colors are set separately.",
                    style = DeftTypography.body,
                    color = colors.muted,
                )
            }
            Column(Modifier.selectableGroup()) {
                AppTheme.entries.forEachIndexed { index, option ->
                    if (index > 0) HorizontalDivider(color = colors.line)
                    ThemeOption(option, theme == option, onTheme)
                }
            }
        }
        SettingsPanel {
            Row(
                Modifier
                    .fillMaxWidth()
                    .heightIn(min = 48.dp)
                    .toggleable(celebrations, role = Role.Switch, onValueChange = onCelebrations),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("celebrations", style = DeftTypography.title)
                    Text(
                        "Show a finish animation before the rating sheet. With celebrations or system animations " +
                            "off, the rating sheet opens immediately.",
                        style = DeftTypography.body,
                        color = colors.muted,
                    )
                }
                DeftSwitch(celebrations, onCheckedChange = null)
            }
        }
    }
}

@Composable
private fun ThemeOption(
    theme: AppTheme,
    selected: Boolean,
    onTheme: (AppTheme) -> Unit,
) {
    val colors = LocalDeftColors.current
    Row(
        Modifier
            .fillMaxWidth()
            .heightIn(min = 48.dp)
            .selectable(selected, role = Role.RadioButton) { onTheme(theme) },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        RadioButton(
            selected,
            onClick = null,
            colors = RadioButtonDefaults.colors(selectedColor = colors.accent, unselectedColor = colors.muted),
        )
        Text(theme.name.lowercase(), style = DeftTypography.label)
    }
}

@Composable
internal fun SettingsPage(
    title: String,
    onBack: () -> Unit,
    content: @Composable ColumnScope.() -> Unit,
) {
    val colors = LocalDeftColors.current
    Scaffold(containerColor = colors.bg, contentColor = colors.text, topBar = {
        DeftSettingsTopBar(title, navigationIcon = {
            IconButton(onBack) { Icon(DeftActionIcons.Back, "back", tint = colors.text) }
        })
    }) { padding ->
        Column(
            Modifier.padding(padding).verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp),
            content = content,
        )
    }
}
