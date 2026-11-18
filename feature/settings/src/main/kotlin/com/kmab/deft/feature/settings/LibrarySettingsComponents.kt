package com.kmab.deft.feature.settings

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material3.Checkbox
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.kmab.deft.core.datastore.LibrarySettings
import com.kmab.deft.core.designsystem.DeftButton
import com.kmab.deft.core.designsystem.DeftButtonStyle
import com.kmab.deft.core.designsystem.DeftShapes
import com.kmab.deft.core.designsystem.DeftTypography
import com.kmab.deft.core.designsystem.LocalDeftColors
import com.kmab.deft.core.designsystem.icons.DeftActionIcons
import com.kmab.deft.core.model.BookFormat
import com.kmab.deft.library.scanner.ScanStatus
import java.util.Locale

@Composable
internal fun SettingsPanel(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    val colors = LocalDeftColors.current
    Surface(
        modifier = modifier.fillMaxWidth().animateContentSize(),
        shape = DeftShapes.card,
        color = colors.surface,
        contentColor = colors.text,
        border = BorderStroke(1.dp, colors.line),
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp), content = content)
    }
}

@Composable
@OptIn(ExperimentalLayoutApi::class)
internal fun LibraryScanPanel(
    scan: ScanStatus,
    onScan: () -> Unit,
    onScanDetails: () -> Unit,
) {
    val colors = LocalDeftColors.current
    SettingsPanel(Modifier.testTag("library-scan-panel")) {
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(DeftActionIcons.Refresh, null, Modifier.size(24.dp), tint = colors.accentText)
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("library scan", style = DeftTypography.title)
                Text(
                    if (scan.isScanning) "scanning your folders" else "Find new and changed book files.",
                    style = DeftTypography.body,
                    color = colors.muted,
                )
            }
        }
        AnimatedVisibility(scan.isScanning) {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                LinearProgressIndicator(
                    Modifier.fillMaxWidth().testTag("library-scan-progress"),
                    color = colors.accent,
                    trackColor = colors.surface2,
                )
                Text(scan.message ?: "Checking book files…", style = DeftTypography.body, color = colors.muted)
                ScanNotificationPermission()
            }
        }
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            DeftButton("rescan now", onScan, enabled = !scan.isScanning, style = DeftButtonStyle.Secondary)
            if (scan.report != null || (scan.isFinished && scan.workId != null)) {
                DeftButton(
                    "scan details",
                    onScanDetails,
                    Modifier.semantics { contentDescription = "last scan details" },
                    style = DeftButtonStyle.Text,
                    icon = DeftActionIcons.Info,
                )
            }
        }
    }
}

@Composable
internal fun LibraryFileTypes(
    settings: LibrarySettings,
    onFormat: (BookFormat, Boolean) -> Unit,
) {
    val colors = LocalDeftColors.current
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text("file types", style = DeftTypography.title)
            Text(
                "${settings.enabledFormats.size} of ${BookFormat.entries.size} types enabled · " +
                    "unchecked types are not scanned",
                style = DeftTypography.body,
                color = colors.muted,
            )
        }
        Surface(
            shape = DeftShapes.card,
            color = colors.surface,
            border = BorderStroke(1.dp, colors.line),
        ) {
            BoxWithConstraints(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 4.dp)) {
                val columns = if (maxWidth < 280.dp || LocalDensity.current.fontScale > LARGE_FONT_SCALE) 1 else 2
                Column {
                    BookFormat.entries.chunked(columns).forEachIndexed { index, formats ->
                        if (index > 0) HorizontalDivider(color = colors.line)
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            formats.forEach { format ->
                                FormatCheckbox(
                                    format,
                                    format in settings.enabledFormats,
                                    { onFormat(format, it) },
                                    Modifier.weight(1f),
                                )
                            }
                            if (formats.size < columns) Spacer(Modifier.weight(1f))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun FormatCheckbox(
    format: BookFormat,
    selected: Boolean,
    onSelected: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier.heightIn(min = 48.dp).toggleable(selected, role = Role.Checkbox, onValueChange = onSelected),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(formatLabel(format), Modifier.weight(1f).padding(vertical = 12.dp), style = DeftTypography.body)
        Checkbox(checked = selected, onCheckedChange = null)
    }
}

internal fun formatLabel(format: BookFormat): String =
    when (format) {
        BookFormat.MOBI -> "mobi/prc"
        else -> format.name.lowercase(Locale.ROOT)
    }

private const val LARGE_FONT_SCALE = 1.3f
