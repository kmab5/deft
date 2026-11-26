package com.kmab.deft.feature.notes

import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyItemScope
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kmab.deft.core.designsystem.DeftButton
import com.kmab.deft.core.designsystem.DeftButtonStyle
import com.kmab.deft.core.designsystem.DeftMotion
import com.kmab.deft.core.designsystem.DeftShapes
import com.kmab.deft.core.designsystem.DeftTypography
import com.kmab.deft.core.designsystem.LocalDeftColors
import com.kmab.deft.core.designsystem.icons.DeftActionIcons
import com.kmab.deft.core.model.HighlightColor
import com.kmab.deft.library.notes.NoteEntry
import com.kmab.deft.library.notes.NoteFilter
import com.kmab.deft.library.notes.NoteType
import org.json.JSONException
import org.json.JSONObject
import kotlin.math.roundToInt

private const val PERCENT_SCALE = 100

@Composable
internal fun NotesContent(
    state: NotesList,
    books: List<NoteEntry>,
    filter: NoteFilter,
    onFilter: (NoteFilter) -> Unit,
    onRetry: () -> Unit,
    onOpen: (NoteEntry) -> Unit,
    onEdit: (NoteEntry) -> Unit,
    onExport: (() -> Unit)?,
    busy: Boolean,
    showBookFilter: Boolean,
    modifier: Modifier = Modifier,
) {
    // Keep the controls in the bounded list so they can scroll away in landscape or at very large text sizes.
    Box(modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
        LazyColumn(
            Modifier.widthIn(max = 720.dp).fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            item("filters") {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    NoteFilters(filter, books, showBookFilter, onFilter, onExport, busy)
                }
            }
            when (state) {
                NotesList.Loading ->
                    item("loading") {
                        Box(
                            noteItemAnimation().fillMaxWidth().padding(24.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            CircularProgressIndicator(color = LocalDeftColors.current.accent)
                        }
                    }
                NotesList.Failure ->
                    item("failure") {
                        Column(noteItemAnimation().padding(vertical = 16.dp)) {
                            Text("Couldn't load notes.", style = DeftTypography.body)
                            DeftButton("try again", onRetry, style = DeftButtonStyle.Text)
                        }
                    }
                is NotesList.Ready -> {
                    if (state.entries.isEmpty()) {
                        item("empty") { EmptyNotes(filter, showBookFilter, noteItemAnimation()) }
                    }
                    items(state.entries, key = NoteEntry::key) { entry ->
                        NoteRow(entry, showBookFilter, busy, { onOpen(entry) }, { onEdit(entry) }, noteItemAnimation())
                    }
                }
            }
        }
    }
}

private fun LazyItemScope.noteItemAnimation(): Modifier =
    Modifier.animateItem(
        fadeInSpec = tween(DeftMotion.SHORT),
        placementSpec = tween(DeftMotion.MEDIUM, easing = DeftMotion.standard),
        fadeOutSpec = tween(DeftMotion.SHORT),
    )

@Composable
private fun EmptyNotes(
    filter: NoteFilter,
    showBookFilter: Boolean,
    modifier: Modifier,
) {
    val message =
        when {
            filter.query.isNotEmpty() || filter.color != null || filter.type != null ||
                (showBookFilter && filter.bookId != null) -> "No notes match these filters."
            else -> "Your highlights, notes and bookmarks appear here."
        }
    Text(
        message,
        modifier.padding(vertical = 16.dp),
        style = DeftTypography.body,
        color = LocalDeftColors.current.muted,
    )
}

@Composable
private fun NoteRow(
    entry: NoteEntry,
    showBook: Boolean,
    busy: Boolean,
    onOpen: () -> Unit,
    onEdit: () -> Unit,
    modifier: Modifier,
) {
    val colors = LocalDeftColors.current
    val rule = (entry.color ?: HighlightColor.GRAY).ruleColor()
    Surface(modifier, color = colors.surface, shape = DeftShapes.card) {
        Box(
            Modifier.fillMaxWidth().drawBehind {
                drawRect(rule, size = Size(3.dp.toPx(), size.height))
            },
        ) {
            Column(
                Modifier
                    .fillMaxWidth()
                    .clickable(onClickLabel = "open saved location", onClick = onOpen)
                    .padding(start = 14.dp, end = 54.dp, top = 12.dp, bottom = 12.dp),
                verticalArrangement = Arrangement.spacedBy(5.dp),
            ) {
                if (entry.type == NoteType.BOOKMARK) {
                    Text(entry.label.orEmpty(), style = DeftTypography.body)
                } else {
                    Text(
                        entry.text,
                        style = DeftTypography.body.copy(fontSize = 13.sp, fontStyle = FontStyle.Italic),
                        maxLines = 5,
                        overflow = TextOverflow.Ellipsis,
                    )
                    entry.note?.takeIf(String::isNotBlank)?.let {
                        if (entry.type == NoteType.HIGHLIGHT) HorizontalDivider(color = colors.line)
                        Text(
                            it,
                            style = DeftTypography.body.copy(fontSize = 13.sp),
                            maxLines = 8,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
                val location = rememberLocation(entry)
                val metadata = if (showBook) "${entry.bookTitle} · $location" else location
                Text(metadata, style = DeftTypography.monoSmall, color = colors.muted)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (entry.missing) Text("missing", style = DeftTypography.monoSmall, color = colors.pausedText)
                    if (entry.drmProtected) {
                        Text(
                            "protected",
                            style = DeftTypography.monoSmall,
                            color = colors.pausedText,
                        )
                    }
                }
            }
            IconButton(onEdit, Modifier.align(Alignment.TopEnd), enabled = !busy) {
                Icon(
                    DeftActionIcons.Rename,
                    if (entry.type == NoteType.BOOKMARK) "rename bookmark" else "edit annotation",
                    tint = colors.muted,
                )
            }
        }
    }
}

@Composable
private fun rememberLocation(entry: NoteEntry): String =
    androidx.compose.runtime.remember(entry.locator, entry.totalProgression) {
        val title =
            try {
                JSONObject(entry.locator).optString("title").takeIf { it.isNotBlank() && it != "null" }
            } catch (_: JSONException) {
                null
            }
        val progress = "${(entry.totalProgression.coerceIn(0.0, 1.0) * PERCENT_SCALE).roundToInt()}%"
        if (title == null) progress else "$title · $progress"
    }
