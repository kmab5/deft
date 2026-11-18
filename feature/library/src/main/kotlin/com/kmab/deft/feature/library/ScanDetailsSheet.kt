package com.kmab.deft.feature.library

import androidx.compose.animation.Crossfade
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.kmab.deft.core.designsystem.DeftMotion
import com.kmab.deft.core.designsystem.DeftTypography
import com.kmab.deft.core.designsystem.LocalDeftColors
import com.kmab.deft.library.scanner.ScanDetailEntry
import com.kmab.deft.library.scanner.ScanDetailOutcome
import com.kmab.deft.library.scanner.ScanDetails
import com.kmab.deft.library.scanner.ScanReport

@Composable
fun ScanDetailsSheet(
    report: ScanReport?,
    details: ScanDetails?,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    isLoading: Boolean = false,
) {
    val c = LocalDeftColors.current
    val grouped =
        remember(details) {
            ScanDetailGroup.entries.associateWith { group ->
                details?.entries.orEmpty().filter {
                    it.outcome in
                        group.outcomes
                }
            }
        }
    var selected by rememberSaveable(report?.reportId) {
        mutableStateOf(
            if ((report?.errors ?: 0) >
                0
            ) {
                ScanDetailGroup.ERRORS
            } else {
                ScanDetailGroup.ADDED
            },
        )
    }
    LibraryPopup("scan details", onDismiss, modifier) {
        Text(
            report?.summary() ?: "Scan stopped. Files saved before it stopped are listed below.",
            style = DeftTypography.body,
            color = c.muted,
            modifier = Modifier.padding(bottom = 8.dp),
        )
        when {
            isLoading ->
                Row(Modifier.padding(vertical = 20.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    CircularProgressIndicator(Modifier.size(20.dp), color = c.accent, strokeWidth = 2.dp)
                    Text("loading scan details…", style = DeftTypography.body, color = c.muted)
                }
            details == null ->
                Text(
                    "Detailed file names aren’t available for this scan.",
                    style = DeftTypography.body,
                    color = c.muted,
                    modifier = Modifier.padding(vertical = 16.dp),
                )
            else -> {
                Row(Modifier.fillMaxWidth().selectableGroup()) {
                    ScanDetailGroup.entries.forEach { group ->
                        DetailTab(
                            group,
                            grouped.getValue(group).size,
                            selected == group,
                            { selected = group },
                            Modifier.weight(1f),
                        )
                    }
                }
                HorizontalDivider(color = c.line)
                Crossfade(selected, animationSpec = tween(DeftMotion.SHORT), label = "scan detail group") { group ->
                    val entries = grouped.getValue(group)
                    if (entries.isEmpty()) {
                        Text(
                            group.emptyMessage,
                            style = DeftTypography.body,
                            color = c.muted,
                            modifier = Modifier.padding(vertical = 24.dp),
                        )
                    } else {
                        LazyColumn(Modifier.heightIn(max = 440.dp)) {
                            items(entries, key = { "${it.id}:${it.outcome}" }) { entry -> ScanDetailRow(entry) }
                        }
                    }
                }
                if (details.omittedCount > 0) {
                    Text(
                        "${details.omittedCount} more scan records weren’t retained. Scan totals are unchanged.",
                        style = DeftTypography.cardTitle,
                        color = c.muted,
                        modifier = Modifier.padding(top = 8.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun DetailTab(
    group: ScanDetailGroup,
    count: Int,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier,
) {
    val c = LocalDeftColors.current
    val background by animateColorAsState(if (selected) c.surface2 else Color.Transparent, tween(DeftMotion.SHORT))
    val foreground by animateColorAsState(if (selected) c.accentText else c.muted, tween(DeftMotion.SHORT))
    Row(
        modifier
            .background(background)
            .heightIn(min = 48.dp)
            .selectable(selected, role = Role.Tab, onClick = onClick)
            .padding(horizontal = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(group.icon, null, Modifier.size(18.dp), tint = foreground)
        Text("${group.label} $count", style = DeftTypography.label, color = foreground)
    }
}

@Composable
private fun ScanDetailRow(entry: ScanDetailEntry) {
    val c = LocalDeftColors.current
    Column(Modifier.fillMaxWidth().padding(vertical = 12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(entry.name, style = DeftTypography.label, color = c.text)
        if (entry.path.isNotBlank() &&
            entry.path != entry.name
        ) {
            Text(entry.path, style = DeftTypography.cardTitle, color = c.muted)
        }
        Text(entry.reason, style = DeftTypography.body, color = c.muted)
    }
    HorizontalDivider(color = c.line)
}

private enum class ScanDetailGroup(
    val label: String,
    val emptyMessage: String,
    val icon: ImageVector,
    val outcomes: Set<ScanDetailOutcome>,
) {
    ADDED(
        "added",
        "No books added or changed in this scan.",
        LibraryIcons.Added,
        setOf(ScanDetailOutcome.ADDED, ScanDetailOutcome.UPDATED, ScanDetailOutcome.RESTORED),
    ),
    SKIPPED(
        "skipped",
        "No skipped or unchanged files in this scan.",
        LibraryIcons.Skipped,
        setOf(ScanDetailOutcome.SKIPPED, ScanDetailOutcome.UNCHANGED, ScanDetailOutcome.DUPLICATE),
    ),
    ERRORS(
        "errors",
        "No errors or metadata fallbacks in this scan.",
        LibraryIcons.Warning,
        setOf(ScanDetailOutcome.ERROR, ScanDetailOutcome.FALLBACK),
    ),
}
