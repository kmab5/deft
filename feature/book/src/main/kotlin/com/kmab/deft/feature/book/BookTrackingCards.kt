package com.kmab.deft.feature.book

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.unit.dp
import com.kmab.deft.core.designsystem.DeftButton
import com.kmab.deft.core.designsystem.DeftDialog
import com.kmab.deft.core.designsystem.DeftHaptic
import com.kmab.deft.core.designsystem.DeftReadingState
import com.kmab.deft.core.designsystem.DeftShapes
import com.kmab.deft.core.designsystem.DeftTypography
import com.kmab.deft.core.designsystem.LocalDeftColors
import com.kmab.deft.core.designsystem.StateChip
import com.kmab.deft.core.designsystem.color
import com.kmab.deft.core.designsystem.icons.DeftActionIcons
import com.kmab.deft.core.designsystem.icons.DeftIcons
import com.kmab.deft.core.designsystem.performDeftHaptic
import com.kmab.deft.core.model.BookState
import com.kmab.deft.core.model.BookTrackingSnapshot
import com.kmab.deft.core.model.ReadThroughOutcome
import com.kmab.deft.feature.celebrate.AssessmentControls
import java.time.Instant
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

@Suppress("CyclomaticComplexMethod") // Fixed reading-state and popup dispatch mirrors the five-state UI.
@Composable
internal fun BookTrackingCards(
    snapshot: BookTrackingSnapshot,
    busy: Boolean,
    actions: BookTrackingActions,
) {
    var sheet by rememberSaveable { mutableStateOf<String?>(null) }
    var selectedId by rememberSaveable { mutableStateOf<Long?>(null) }
    TrackingCard("your reading", DeftIcons.Books) {
        Row(
            Modifier
                .fillMaxWidth()
                .clickable(enabled = !busy, role = Role.Button) { sheet = "state" }
                .semantics {
                    contentDescription = "change reading state"
                    stateDescription = snapshot.state.appearance().label
                },
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text("reading state", Modifier.weight(1f), style = DeftTypography.label)
            StateChip(snapshot.state.appearance())
            Icon(DeftActionIcons.ChevronRight, null, tint = LocalDeftColors.current.accentText)
        }
        HorizontalDivider(color = LocalDeftColors.current.line)
        AssessmentControls(
            snapshot.assessment.rating,
            snapshot.assessment.favorite,
            actions.rating,
            actions.favorite,
            !busy,
        )
        HorizontalDivider(color = LocalDeftColors.current.line)
        val review = snapshot.assessment.review.orEmpty()
        Row(
            Modifier
                .fillMaxWidth()
                .heightIn(min = 48.dp)
                .clickable(enabled = !busy, role = Role.Button) { sheet = "review" },
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text("your review", Modifier.weight(1f), style = DeftTypography.label)
            Text(
                if (review.isBlank()) "write a review" else "edit review",
                style = DeftTypography.label,
                color = LocalDeftColors.current.accentText,
            )
            Icon(DeftActionIcons.Rename, null, Modifier.size(18.dp), tint = LocalDeftColors.current.accentText)
        }
        if (review.isNotBlank()) Text(review, style = DeftTypography.body)
    }
    ReadingHistoryCard(snapshot, busy) { id, action ->
        selectedId = id
        sheet = action
    }
    val selected = snapshot.readThroughs.firstOrNull { it.id == selectedId }
    when (sheet) {
        "state" ->
            TrackingSheet("reading state", { sheet = null }, Modifier.testTag("reading_state_sheet")) {
                val sheetView = LocalView.current
                ReadingStateChoices(snapshot.state, !busy) { target ->
                    when (target) {
                        BookState.NOT_STARTED -> sheet = "reset"
                        BookState.FINISHED -> sheet = "finish"
                        else -> {
                            actions.setState(target, Instant.now(), false)
                            sheetView.performDeftHaptic(DeftHaptic.CONFIRM)
                            sheet = null
                        }
                    }
                }
            }
        "reset" ->
            DeftDialog(
                "mark as not read?",
                "This deletes every read-through and reading session for this book. " +
                    "Your rating, review and saved position stay. This can't be undone.",
                "erase reading history",
                {
                    actions.setState(BookState.NOT_STARTED, Instant.now(), true)
                    sheet = null
                },
                {
                    sheet =
                        null
                },
            )
        "finish" -> {
            val reference by rememberSaveable { mutableStateOf(Instant.now().toString()) }
            val original = Instant.parse(reference)
            CalendarDialog(original.localDate(), { sheet = null }) { date ->
                actions.setState(BookState.FINISHED, dateInstant(date, original), false)
                sheet = null
            }
        }
        "dates" ->
            selected?.let { attempt ->
                TrackingDates(attempt.startedAt, attempt.endedAt, { sheet = null }) { start, end ->
                    actions.editDates(attempt.id, start, end)
                    sheet = null
                }
            }
        "delete" ->
            selected?.let { attempt ->
                DeftDialog(
                    "delete this read-through?",
                    "Its sessions will also be deleted. Earlier read-throughs stay. This can't be undone.",
                    "delete read-through",
                    {
                        actions.delete(attempt.id)
                        sheet = null
                    },
                    { sheet = null },
                )
            }
        "review" ->
            ReviewEditor(snapshot.assessment.review.orEmpty(), { sheet = null }) {
                actions.review(it)
                sheet =
                    null
            }
        "sessions" -> selected?.let { SessionHistory(snapshot, it.id) { sheet = null } }
    }
}

@Composable
private fun TrackingCard(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    content: @Composable ColumnScope.() -> Unit,
) {
    val c = LocalDeftColors.current
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Icon(icon, null, Modifier.size(20.dp), tint = c.muted)
            Text(title, style = DeftTypography.title, color = c.text)
        }
        Surface(
            shape = DeftShapes.card,
            color = c.surface,
            border = BorderStroke(1.dp, c.line),
            shadowElevation = 1.dp,
        ) {
            Column(
                Modifier.fillMaxWidth().padding(16.dp).animateContentSize(),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                content = content,
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun TrackingSheet(
    title: String,
    dismiss: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    val c = LocalDeftColors.current
    ModalBottomSheet(
        onDismissRequest = dismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = c.surface,
        contentColor = c.text,
        shape = DeftShapes.sheet,
        dragHandle = null,
    ) {
        Column(
            modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(title, Modifier.weight(1f), style = DeftTypography.title)
                IconButton(dismiss) { Icon(DeftActionIcons.Close, "close popup") }
            }
            content()
        }
    }
}

@Composable
private fun ReviewEditor(
    initial: String,
    dismiss: () -> Unit,
    save: (String) -> Unit,
) {
    var text by rememberSaveable { mutableStateOf(initial) }
    TrackingSheet("your review", dismiss) {
        Text("Private plain text. Clearing the text removes your review.", style = DeftTypography.body)
        OutlinedTextField(text, { text = it }, Modifier.fillMaxWidth(), label = { Text("review") }, minLines = 4)
        DeftButton("save review", { save(text) }, icon = DeftActionIcons.Check)
    }
}

private const val SECONDS_PER_HOUR = 3600
private const val SECONDS_PER_MINUTE = 60

private fun sessionCountLabel(count: Int): String = if (count == 1) "1 session" else "$count sessions"

internal fun durationLabel(seconds: Long): String =
    if (seconds >= SECONDS_PER_HOUR) {
        "${seconds / SECONDS_PER_HOUR}h ${seconds % SECONDS_PER_HOUR / SECONDS_PER_MINUTE}m"
    } else if (seconds in 1 until SECONDS_PER_MINUTE) {
        "${seconds}s"
    } else {
        "${seconds / SECONDS_PER_MINUTE}m"
    }

internal fun BookState.appearance(): DeftReadingState =
    when (this) {
        BookState.NOT_STARTED -> DeftReadingState.NotRead
        BookState.READING -> DeftReadingState.Reading
        BookState.FINISHED -> DeftReadingState.Finished
        BookState.PAUSED -> DeftReadingState.Paused
        BookState.ABANDONED -> DeftReadingState.Abandoned
    }

private fun ReadThroughOutcome.appearance(): DeftReadingState =
    when (this) {
        ReadThroughOutcome.IN_PROGRESS -> DeftReadingState.Reading
        ReadThroughOutcome.FINISHED -> DeftReadingState.Finished
        ReadThroughOutcome.PAUSED -> DeftReadingState.Paused
        ReadThroughOutcome.ABANDONED -> DeftReadingState.Abandoned
    }

@Composable
private fun ReadingHistoryCard(
    snapshot: BookTrackingSnapshot,
    busy: Boolean,
    onAction: (Long, String) -> Unit,
) {
    TrackingCard("reading history", DeftActionIcons.History) {
        val c = LocalDeftColors.current
        if (snapshot.readThroughs.isEmpty()) {
            Text("Your read-throughs will appear here.", style = DeftTypography.body, color = c.muted)
        }
        if (snapshot.readThroughs.size > 1) {
            Text(
                "${durationLabel(snapshot.activeSeconds)} · ${sessionCountLabel(snapshot.sessionCount)} in total",
                style = DeftTypography.mono,
            )
            HorizontalDivider(color = c.line)
        }
        snapshot.readThroughs.sortedByDescending { it.startedAt }.forEachIndexed { index, attempt ->
            if (index > 0) HorizontalDivider(color = c.line)
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Column(
                    Modifier
                        .weight(1f)
                        .heightIn(min = 48.dp)
                        .clickable(
                            role = Role.Button,
                            onClickLabel = "view sessions",
                        ) { onAction(attempt.id, "sessions") },
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    val state = attempt.outcome.appearance()
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Box(Modifier.size(6.dp).background(state.color(c)))
                        Text(state.label, style = DeftTypography.title)
                    }
                    Text(
                        "${attempt.startedAt.localDate()} — ${attempt.endedAt?.localDate() ?: "ongoing"}",
                        style = DeftTypography.mono,
                        color = c.muted,
                    )
                    val counted = snapshot.sessions.filter { it.readThroughId == attempt.id && !it.browsing }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            "${durationLabel(counted.sumOf { it.activeSeconds })} · ${sessionCountLabel(counted.size)}",
                            style = DeftTypography.label,
                            color = c.accentText,
                        )
                        Icon(DeftActionIcons.ChevronRight, null, Modifier.size(18.dp), tint = c.accentText)
                    }
                }
                HistoryOptions(attempt.startedAt.localDate(), busy) { onAction(attempt.id, it) }
            }
        }
    }
}

@Composable
private fun HistoryOptions(
    date: String,
    busy: Boolean,
    select: (String) -> Unit,
) {
    var open by rememberSaveable { mutableStateOf(false) }
    Box {
        IconButton({ open = true }, enabled = !busy) {
            Icon(DeftActionIcons.More, "read-through options, $date", tint = LocalDeftColors.current.muted)
        }
        DropdownMenu(open, { open = false }) {
            DropdownMenuItem(text = { Text("edit dates") }, onClick = {
                open = false
                select("dates")
            })
            DropdownMenuItem(text = { Text("delete") }, onClick = {
                open = false
                select("delete")
            })
        }
    }
}

@Composable
private fun SessionHistory(
    snapshot: BookTrackingSnapshot,
    readThroughId: Long,
    dismiss: () -> Unit,
) {
    TrackingSheet("sessions", dismiss) {
        val sessions =
            snapshot.sessions
                .filter {
                    it.readThroughId == readThroughId
                }.sortedByDescending { it.startedAt }
        if (sessions.isEmpty()) {
            Text(
                "No recorded sessions in this read-through.",
                style = DeftTypography.body,
            )
        }
        sessions.forEach { session ->
            Column(
                Modifier.fillMaxWidth().padding(vertical = 10.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                val local = session.startedAt.atOffset(ZoneOffset.ofTotalSeconds(session.zoneOffsetSeconds))
                Text(
                    local.format(DateTimeFormatter.ofPattern("d MMM yyyy · HH:mm")),
                    style = DeftTypography.label,
                )
                val pageTurns = if (session.pagesTurned == 1) "1 page turn" else "${session.pagesTurned} page turns"
                Text(
                    "${durationLabel(session.activeSeconds)} · $pageTurns",
                    style = DeftTypography.mono,
                )
                if (session.browsing) {
                    Text(
                        "just browsing · excluded from totals",
                        style = DeftTypography.body,
                        color = LocalDeftColors.current.muted,
                    )
                }
            }
            HorizontalDivider(color = LocalDeftColors.current.line)
        }
    }
}
