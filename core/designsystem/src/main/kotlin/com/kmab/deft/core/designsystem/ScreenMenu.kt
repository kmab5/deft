package com.kmab.deft.core.designsystem

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Box
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.unit.dp
import com.kmab.deft.core.designsystem.icons.DeftActionIcons

@Composable
fun DeftScreenMenu(
    onSettings: () -> Unit,
    onScanDetails: (() -> Unit)? = null,
) {
    val colors = LocalDeftColors.current
    var expanded by remember { mutableStateOf(false) }
    Box {
        IconButton({ expanded = true }) { Icon(DeftActionIcons.More, "more options", tint = colors.muted) }
        DropdownMenu(
            expanded,
            { expanded = false },
            shape = DeftShapes.card,
            containerColor = colors.surface,
            tonalElevation = 0.dp,
            shadowElevation = 0.dp,
            border = BorderStroke(1.dp, colors.line),
        ) {
            DropdownMenuItem(
                text = { Text("last scan", style = DeftTypography.label) },
                leadingIcon = { Icon(DeftActionIcons.History, null) },
                enabled = onScanDetails != null,
                onClick = {
                    expanded = false
                    onScanDetails?.invoke()
                },
            )
            DropdownMenuItem(
                text = { Text("settings", style = DeftTypography.label) },
                leadingIcon = { Icon(DeftActionIcons.Settings, null) },
                onClick = {
                    expanded = false
                    onSettings()
                },
            )
        }
    }
}
