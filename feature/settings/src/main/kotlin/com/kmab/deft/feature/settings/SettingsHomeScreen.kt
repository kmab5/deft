package com.kmab.deft.feature.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.kmab.deft.core.designsystem.DeftTopBar
import com.kmab.deft.core.designsystem.DeftTypography
import com.kmab.deft.core.designsystem.LocalDeftColors
import com.kmab.deft.core.designsystem.icons.DeftActionIcons
import com.kmab.deft.core.designsystem.icons.DeftIcons

@Composable
fun SettingsHomeScreen(
    onBack: () -> Unit,
    onLibrary: () -> Unit,
    onReader: () -> Unit,
) {
    val colors = LocalDeftColors.current
    Scaffold(containerColor = colors.bg, contentColor = colors.text, topBar = {
        DeftTopBar("settings", navigationIcon = {
            IconButton(onBack) { Icon(DeftActionIcons.Back, "back", tint = colors.text) }
        })
    }) { padding ->
        Column(Modifier.padding(padding).padding(horizontal = 20.dp)) {
            SettingsCategory("library", "Folders, file types and ignored books.", DeftActionIcons.Folder, onLibrary)
            HorizontalDivider(color = colors.line)
            SettingsCategory("reader", "Text, themes and page controls.", DeftIcons.Books, onReader)
        }
    }
}

@Composable
private fun SettingsCategory(
    title: String,
    description: String,
    icon: ImageVector,
    onClick: () -> Unit,
) {
    val colors = LocalDeftColors.current
    Row(
        Modifier
            .fillMaxWidth()
            .heightIn(min = 72.dp)
            .clickable(role = Role.Button, onClick = onClick)
            .padding(vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Icon(icon, null, tint = colors.muted)
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(title, style = DeftTypography.title, color = colors.text)
            Text(description, style = DeftTypography.body, color = colors.muted)
        }
        Icon(DeftActionIcons.ChevronRight, null, tint = colors.muted)
    }
}
