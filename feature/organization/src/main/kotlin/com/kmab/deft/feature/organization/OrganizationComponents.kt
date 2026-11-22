package com.kmab.deft.feature.organization

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.kmab.deft.core.designsystem.DeftReadingState
import com.kmab.deft.core.designsystem.DeftShapes
import com.kmab.deft.core.designsystem.DeftTypography
import com.kmab.deft.core.designsystem.LocalDeftColors
import com.kmab.deft.core.designsystem.ProgressSegment
import com.kmab.deft.core.designsystem.SegmentedProgress
import com.kmab.deft.core.designsystem.icons.DeftActionIcons
import com.kmab.deft.core.model.BookState
import com.kmab.deft.library.organization.OrganizationStats
import java.math.BigDecimal
import java.util.Locale

@Composable
internal fun OrganizationPanel(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    val colors = LocalDeftColors.current
    Surface(
        modifier.fillMaxWidth(),
        shape = DeftShapes.card,
        color = colors.surface,
        border = BorderStroke(1.dp, colors.line),
        shadowElevation = 1.dp,
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp), content = content)
    }
}

@Composable
internal fun OrganizationMenu(
    actions: List<Pair<String, () -> Unit>>,
    trigger: @Composable (() -> Unit) -> Unit = { expand ->
        IconButton(expand) { Icon(DeftActionIcons.More, "more options") }
    },
) {
    var open by remember { mutableStateOf(false) }
    Box {
        trigger { open = true }
        DropdownMenu(
            open,
            { open = false },
            shape = DeftShapes.card,
            containerColor = LocalDeftColors.current.surface,
            border = BorderStroke(1.dp, LocalDeftColors.current.line),
        ) {
            actions.forEach { (label, select) ->
                DropdownMenuItem(text = { Text(label) }, onClick = {
                    open = false
                    select()
                })
            }
        }
    }
}

@Composable
internal fun OrganizationRow(
    title: String,
    subtitle: String,
    enabled: Boolean = true,
    onClick: () -> Unit,
) {
    Row(
        Modifier
            .fillMaxWidth()
            .heightIn(min = 48.dp)
            .clickable(enabled = enabled, role = Role.Button, onClick = onClick)
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(title, style = DeftTypography.title)
            Text(subtitle, style = DeftTypography.mono, color = LocalDeftColors.current.muted)
        }
        Icon(DeftActionIcons.ChevronRight, null)
    }
}

@Composable
internal fun OrganizationProgress(
    stats: OrganizationStats,
    legend: Boolean = true,
    compactLegend: Boolean = false,
) {
    val segments =
        listOf(
            DeftReadingState.Finished to stats.finished,
            DeftReadingState.Reading to stats.reading,
            DeftReadingState.Paused to stats.paused,
            DeftReadingState.Abandoned to stats.abandoned,
            DeftReadingState.NotRead to
                (stats.total - stats.finished - stats.reading - stats.paused - stats.abandoned),
        )
    SegmentedProgress(
        segments.filter { it.second > 0 }.map { ProgressSegment(it.first, it.second.toFloat()) },
        legend = if (legend) "${stats.finished} finished · ${stats.total} books" else null,
    )
    val breakdown = if (compactLegend) segments.drop(1).filter { it.second > 0 } else segments
    if (legend && stats.total > 0 && breakdown.isNotEmpty()) {
        FlowRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            breakdown.forEach { (state, count) ->
                Text(
                    "$count ${state.label}",
                    style = DeftTypography.mono,
                    color = LocalDeftColors.current.muted,
                )
            }
        }
    }
}

@Composable
internal fun OrganizationEmpty(
    text: String,
    loading: Boolean = false,
) {
    Box(Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) {
        if (loading) CircularProgressIndicator() else Text(text, style = DeftTypography.body)
    }
}

internal fun positionLabel(position: Double): String = BigDecimal.valueOf(position).stripTrailingZeros().toPlainString()

internal fun ratingLabel(rating: Double?): String = rating?.let { String.format(Locale.ROOT, "%.1f", it) } ?: "unrated"

internal fun timeLabel(seconds: Long): String {
    val hours = seconds / SECONDS_PER_HOUR
    val minutes = seconds % SECONDS_PER_HOUR / SECONDS_PER_MINUTE
    return "$hours h $minutes min"
}

internal fun BookState.readingLabel(): String =
    when (this) {
        BookState.NOT_STARTED -> "not read"
        BookState.READING -> "reading"
        BookState.PAUSED -> "paused"
        BookState.FINISHED -> "finished"
        BookState.ABANDONED -> "abandoned"
    }

private const val SECONDS_PER_HOUR = 3600
private const val SECONDS_PER_MINUTE = 60
