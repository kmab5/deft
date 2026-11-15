package com.kmab.deft.core.designsystem

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.selectable
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.kmab.deft.core.designsystem.icons.DeftIcons

private const val PERCENTAGE_SCALE = 100
private const val COVER_ASPECT_RATIO = 2f / 3f

/** UI-only reading-state appearance. Domain reading states arrive with the model task. */
enum class DeftReadingState(
    val label: String,
) {
    NotRead("not read"),
    Reading("reading"),
    Finished("finished"),
    Paused("paused"),
    Abandoned("abandoned"),
}

fun DeftReadingState.color(c: DeftColors): Color =
    when (this) {
        DeftReadingState.NotRead -> c.line2
        DeftReadingState.Reading -> c.accent
        DeftReadingState.Finished -> c.finished
        DeftReadingState.Paused -> c.paused
        DeftReadingState.Abandoned -> c.abandoned
    }

private fun DeftReadingState.textColor(c: DeftColors): Color =
    when (this) {
        DeftReadingState.NotRead -> c.muted
        DeftReadingState.Reading -> c.accentText
        DeftReadingState.Finished -> c.finishedText
        DeftReadingState.Paused -> c.pausedText
        DeftReadingState.Abandoned -> c.abandoned
    }

@Composable
fun StateChip(
    state: DeftReadingState,
    selected: Boolean = false,
    onClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    val c = LocalDeftColors.current
    val outline by animateColorAsState(if (selected) state.color(c) else Color.Transparent, tween(DeftMotion.SHORT))
    Box(
        modifier.heightIn(min = 48.dp).then(
            if (onClick !=
                null
            ) {
                Modifier.selectable(selected, role = Role.Checkbox, onClick = onClick)
            } else {
                Modifier
            },
        ),
        contentAlignment = Alignment.Center,
    ) {
        Row(
            Modifier
                .clip(
                    DeftShapes.small,
                ).background(c.surface2)
                .border(1.dp, outline, DeftShapes.small)
                .padding(horizontal = 8.dp, vertical = 5.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Box(Modifier.size(6.dp).background(state.color(c)))
            Text(state.label, color = state.textColor(c), style = DeftTypography.mono)
        }
    }
}

@Composable
fun FilterChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val c = LocalDeftColors.current
    Box(
        modifier.heightIn(min = 48.dp).selectable(selected, role = Role.Checkbox, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            label,
            Modifier
                .clip(
                    DeftShapes.small,
                ).background(
                    c.surface2,
                ).border(1.dp, if (selected) c.accent else Color.Transparent, DeftShapes.small)
                .padding(horizontal = 10.dp, vertical = 6.dp),
            color = if (selected) c.accentText else c.text,
            style = DeftTypography.mono,
        )
    }
}

enum class DeftButtonStyle { Primary, Secondary, Text }

@Composable
fun DeftButton(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    style: DeftButtonStyle = DeftButtonStyle.Primary,
    enabled: Boolean = true,
) {
    val c = LocalDeftColors.current
    Box(
        modifier
            .heightIn(
                min = 48.dp,
            ).clip(DeftShapes.small)
            .clickable(enabled = enabled, role = Role.Button, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        val bg = if (style == DeftButtonStyle.Primary) c.accent else Color.Transparent
        val fg =
            when (style) {
                DeftButtonStyle.Primary -> c.onAccent
                DeftButtonStyle.Secondary -> c.text
                DeftButtonStyle.Text -> c.accentText
            }
        Box(
            Modifier
                .heightIn(min = 44.dp)
                .background(if (enabled) bg else c.surface2, DeftShapes.small)
                .border(
                    1.dp,
                    if (style ==
                        DeftButtonStyle.Secondary
                    ) {
                        c.line2
                    } else {
                        Color.Transparent
                    },
                    DeftShapes.small,
                ).padding(horizontal = 16.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text(label, style = DeftTypography.label, color = if (enabled) fg else c.dim)
        }
    }
}

@Composable
fun StateBar(
    state: DeftReadingState,
    progress: Float,
    modifier: Modifier = Modifier,
) {
    val c = LocalDeftColors.current
    val value by animateFloatAsState(progress.coerceIn(0f, 1f), tween(DeftMotion.MEDIUM))
    val color by animateColorAsState(state.color(c), tween(DeftMotion.SHORT))
    Canvas(modifier.fillMaxWidth().height(2.dp)) {
        drawRect(c.line)
        if (state != DeftReadingState.NotRead) drawRect(color, size = size.copy(width = size.width * value))
    }
}

@Composable
fun GeneratedCover(
    title: String,
    author: String,
    modifier: Modifier = Modifier,
) {
    val tint = Palette.coverTints[Math.floorMod(title.hashCode(), Palette.coverTints.size)]
    Column(modifier.fillMaxSize().background(tint).padding(10.dp), verticalArrangement = Arrangement.SpaceBetween) {
        Text(
            title,
            style = DeftTypography.cardTitle,
            color = DeftColors.Ink.text,
            maxLines = 5,
            overflow = TextOverflow.Ellipsis,
        )
        Text(
            author,
            style = DeftTypography.monoSmall,
            color = DeftColors.Ink.text,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
fun CoverBadge(
    label: String,
    isProtected: Boolean = false,
    modifier: Modifier = Modifier,
) {
    val c = DeftColors.Ink
    Row(
        modifier.background(c.scrim, DeftShapes.small).padding(4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(3.dp),
    ) {
        if (isProtected) Icon(DeftIcons.Lock, null, Modifier.size(12.dp), tint = c.text)
        Text(label, style = DeftTypography.monoSmall, color = c.text)
    }
}

@Composable
fun CoverImage(
    title: String,
    author: String,
    modifier: Modifier = Modifier,
    missing: Boolean = false,
    isProtected: Boolean = false,
    image: (@Composable BoxScope.() -> Unit)? = null,
) {
    val c = LocalDeftColors.current
    Box(modifier.aspectRatio(COVER_ASPECT_RATIO).clip(DeftShapes.cover).border(1.dp, c.line, DeftShapes.cover)) {
        if (image == null) GeneratedCover(title, author) else image()
        Column(Modifier.align(Alignment.TopStart).padding(4.dp), verticalArrangement = Arrangement.spacedBy(3.dp)) {
            if (missing) CoverBadge("missing")
            if (isProtected) CoverBadge("protected", isProtected = true)
        }
    }
}

@Composable
fun BookCover(
    title: String,
    author: String,
    state: DeftReadingState,
    progress: Float,
    modifier: Modifier = Modifier,
    image: (@Composable BoxScope.() -> Unit)? = null,
) {
    val c = LocalDeftColors.current
    Column(modifier, verticalArrangement = Arrangement.spacedBy(4.dp)) {
        CoverImage(title, author, image = image)
        StateBar(state, progress)
        Text(title, style = DeftTypography.cardTitle, color = c.text, maxLines = 1, overflow = TextOverflow.Ellipsis)
        Text(author, style = DeftTypography.monoSmall, color = c.muted, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

data class ProgressSegment(
    val state: DeftReadingState,
    val fraction: Float,
)

@Composable
fun SegmentedProgress(
    segments: List<ProgressSegment>,
    modifier: Modifier = Modifier,
    legend: String? = null,
) {
    val c = LocalDeftColors.current
    val total = segments.sumOf { it.fraction.coerceAtLeast(0f).toDouble() }.toFloat().coerceAtLeast(1f)
    val fractions =
        segments.map { segment ->
            animateFloatAsState(segment.fraction.coerceAtLeast(0f) / total, tween(DeftMotion.MEDIUM)).value
        }
    val colors =
        segments.map { segment ->
            animateColorAsState(segment.state.color(c), tween(DeftMotion.SHORT)).value
        }
    Column(modifier, verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Canvas(Modifier.fillMaxWidth().height(6.dp).clip(DeftShapes.cover)) {
            drawRect(c.surface2)
            var left = 0f
            fractions.forEachIndexed { index, fraction ->
                val width = size.width * fraction
                drawRect(
                    colors[index],
                    topLeft =
                        androidx.compose.ui.geometry
                            .Offset(left, 0f),
                    size = size.copy(width = width),
                )
                left += width
            }
        }
        if (legend != null) Text(legend, style = DeftTypography.mono, color = c.muted)
    }
}

@Composable
fun BookRow(
    title: String,
    authors: String,
    state: DeftReadingState,
    progress: Float,
    timeRead: String,
    modifier: Modifier = Modifier,
) {
    val c = LocalDeftColors.current
    Row(
        modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        CoverImage(title, authors, Modifier.width(56.dp))
        Column(Modifier.weight(1f)) {
            Text(title, style = DeftTypography.title, color = c.text, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(authors, style = DeftTypography.mono, color = c.muted, maxLines = 1)
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                StateChip(state)
                Text(
                    "${(progress.coerceIn(0f, 1f) * PERCENTAGE_SCALE).toInt()}% · $timeRead",
                    style = DeftTypography.mono,
                    color = c.muted,
                )
            }
        }
    }
}

@Composable
fun StatTile(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    delta: String? = null,
    increasing: Boolean = true,
) {
    val c = LocalDeftColors.current
    Column(
        modifier.background(c.surface, DeftShapes.card).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(label, color = c.muted, style = DeftTypography.monoSmall)
        Text(value, color = c.text, style = DeftTypography.statLarge)
        if (delta != null) Text(delta, color = if (increasing) c.finishedText else c.muted, style = DeftTypography.mono)
    }
}

@Composable
fun PersonalBestStrip(
    what: String,
    modifier: Modifier = Modifier,
) {
    val c = LocalDeftColors.current
    Box(
        modifier
            .fillMaxWidth()
            .heightIn(min = 32.dp)
            .background(c.accent)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        contentAlignment = Alignment.CenterStart,
    ) {
        Text("personal best · $what", style = DeftTypography.mono, color = c.onAccent)
    }
}

@Composable
fun ContinueCard(
    title: String,
    author: String,
    progress: Float,
    timeLeft: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val c = LocalDeftColors.current
    Row(
        modifier
            .fillMaxWidth()
            .background(
                c.surface,
                DeftShapes.card,
            ).clip(DeftShapes.card)
            .clickable(role = Role.Button, onClick = onClick)
            .padding(12.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        CoverImage(title, author, Modifier.width(48.dp))
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text("continue", style = DeftTypography.monoSmall, color = c.muted)
            Text(title, style = DeftTypography.title, color = c.text, maxLines = 1, overflow = TextOverflow.Ellipsis)
            StateBar(DeftReadingState.Reading, progress)
            Text(
                "${(progress.coerceIn(0f, 1f) * PERCENTAGE_SCALE).toInt()}% · $timeLeft left",
                style = DeftTypography.mono,
                color = c.muted,
            )
        }
    }
}
