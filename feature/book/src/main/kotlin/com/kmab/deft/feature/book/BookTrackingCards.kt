package com.kmab.deft.feature.book

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.unit.dp
import com.kmab.deft.core.designsystem.DeftButton
import com.kmab.deft.core.designsystem.DeftButtonStyle
import com.kmab.deft.core.designsystem.DeftDialog
import com.kmab.deft.core.designsystem.DeftReadingState
import com.kmab.deft.core.designsystem.DeftShapes
import com.kmab.deft.core.designsystem.DeftTypography
import com.kmab.deft.core.designsystem.LocalDeftColors
import com.kmab.deft.core.designsystem.StateChip
import com.kmab.deft.core.designsystem.icons.DeftActionIcons
import com.kmab.deft.core.model.BookState
import com.kmab.deft.core.model.BookTrackingSnapshot
import com.kmab.deft.core.model.MIN_SESSION_SECONDS
import com.kmab.deft.core.model.ReadThroughOutcome
import com.kmab.deft.feature.celebrate.AssessmentControls
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
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
    val view = LocalView.current
    val confirm: () -> Unit = {
        view.performHapticFeedback(
            if (android.os.Build.VERSION.SDK_INT >=
                android.os.Build.VERSION_CODES.R
            ) {
                android.view.HapticFeedbackConstants.CONFIRM
            } else {
                android.view.HapticFeedbackConstants.CONTEXT_CLICK
            },
        )
    }
    TrackingCard("your reading", DeftActionIcons.History) {
        Row(
            Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            StateChip(snapshot.state.appearance())
            DeftButton("change state", { sheet = "state" }, style = DeftButtonStyle.Text, enabled = !busy)
        }
        AssessmentControls(
            snapshot.assessment.rating,
            snapshot.assessment.favorite,
            actions.rating,
            actions.favorite,
            !busy,
        )
    }
    TrackingCard("your review", DeftActionIcons.Rename) {
        Text(
            snapshot.assessment.review?.takeIf { it.isNotBlank() } ?: "Keep your thoughts here. Only you can see them.",
            style = DeftTypography.body,
        )
        DeftButton(
            if (snapshot.assessment.review.isNullOrEmpty()) "write a review" else "edit review",
            { sheet = "review" },
            style = DeftButtonStyle.Text,
            enabled = !busy,
            icon = DeftActionIcons.Rename,
        )
    }
    ReadingHistoryCard(snapshot, busy) { id, action ->
        selectedId = id
        sheet = action
    }
    val selected = snapshot.readThroughs.firstOrNull { it.id == selectedId }
    when (sheet) {
        "state" ->
            TrackingSheet("reading state", { sheet = null }) {
                BookState.entries.forEach { target ->
                    DeftButton(target.appearance().label, {
                        when (target) {
                            BookState.NOT_STARTED -> sheet = "reset"
                            BookState.FINISHED -> sheet = "finish"
                            else -> {
                                actions.setState(target, Instant.now(), false)
                                confirm()
                                sheet = null
                            }
                        }
                    }, Modifier.fillMaxWidth(), style = DeftButtonStyle.Text, enabled = !busy)
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
        "finish" ->
            TrackingDates("finish date", null, Instant.now(), { sheet = null }) { _, end ->
                actions.setState(BookState.FINISHED, checkNotNull(end), false)
                confirm()
                sheet = null
            }
        "dates" ->
            selected?.let { attempt ->
                TrackingDates("edit dates", attempt.startedAt, attempt.endedAt, {
                    sheet =
                        null
                }) { start, end ->
                    actions.editDates(attempt.id, checkNotNull(start), end)
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

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun FlowRowActions(content: @Composable () -> Unit) {
    FlowRow(horizontalArrangement = Arrangement.spacedBy(4.dp)) { content() }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun TrackingSheet(
    title: String,
    dismiss: () -> Unit,
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
            Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(16.dp),
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

@Composable
private fun TrackingDates(
    title: String,
    initialStart: Instant?,
    initialEnd: Instant?,
    dismiss: () -> Unit,
    save: (Instant?, Instant?) -> Unit,
) {
    var start by rememberSaveable { mutableStateOf(initialStart?.localDate().orEmpty()) }
    var end by rememberSaveable { mutableStateOf(initialEnd?.localDate().orEmpty()) }
    var error by rememberSaveable { mutableStateOf<String?>(null) }
    TrackingSheet(title, dismiss) {
        Text("Use YYYY-MM-DD. Session timestamps stay unchanged.", style = DeftTypography.body)
        if (initialStart != null) {
            OutlinedTextField(
                start,
                {
                    start = it
                    error = null
                },
                Modifier.fillMaxWidth(),
                label = { Text("started") },
                singleLine = true,
            )
        }
        if (initialEnd != null) {
            OutlinedTextField(
                end,
                {
                    end = it
                    error = null
                },
                Modifier.fillMaxWidth(),
                label = { Text("finished / ended") },
                singleLine = true,
            )
        }
        error?.let { Text(it, style = DeftTypography.body, color = LocalDeftColors.current.abandoned) }
        DeftButton("save date", {
            try {
                val parsedStart = initialStart?.let { dateInstant(start, it) }
                val parsedEnd = initialEnd?.let { dateInstant(end, it) }
                require(parsedStart == null || parsedEnd == null || !parsedEnd.isBefore(parsedStart))
                save(parsedStart, parsedEnd)
            } catch (
                _: java.time.DateTimeException,
            ) {
                error = "Enter a valid date as YYYY-MM-DD."
            } catch (
                _: IllegalArgumentException,
            ) {
                error = "The end date must be on or after the start date."
            }
        }, icon = DeftActionIcons.Check)
    }
}

internal fun dateInstant(
    text: String,
    original: Instant,
): Instant {
    val date = LocalDate.parse(text)
    val zone = ZoneId.systemDefault()
    return date.atTime(original.atZone(zone).toLocalTime()).atZone(zone).toInstant()
}

internal fun Instant.localDate(): String = atZone(ZoneId.systemDefault()).toLocalDate().toString()

private const val SECONDS_PER_HOUR = 3600
private const val SECONDS_PER_MINUTE = 60

private fun sessionCountLabel(count: Int): String = if (count == 1) "1 session" else "$count sessions"

internal fun durationLabel(seconds: Long): String =
    if (seconds >= SECONDS_PER_HOUR) {
        "${seconds / SECONDS_PER_HOUR}h ${seconds % SECONDS_PER_HOUR / SECONDS_PER_MINUTE}m"
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

private fun ReadThroughOutcome.label(): String =
    when (this) {
        ReadThroughOutcome.IN_PROGRESS -> "reading"
        ReadThroughOutcome.FINISHED -> "finished"
        ReadThroughOutcome.PAUSED -> "paused"
        ReadThroughOutcome.ABANDONED -> "abandoned"
    }

@Composable
private fun ReadingHistoryCard(
    snapshot: BookTrackingSnapshot,
    busy: Boolean,
    onAction: (Long, String) -> Unit,
) {
    TrackingCard("reading history", DeftActionIcons.History) {
        Text(
            "${durationLabel(snapshot.activeSeconds)} · ${sessionCountLabel(snapshot.sessionCount)}",
            style = DeftTypography.mono,
        )
        if (snapshot.readThroughs.isEmpty()) {
            Text(
                "No read-throughs yet. A minute of reading starts one.",
                style = DeftTypography.body,
            )
        }
        snapshot.readThroughs.sortedByDescending { it.id }.forEach { attempt ->
            HorizontalDivider(color = LocalDeftColors.current.line)
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(attempt.outcome.label(), style = DeftTypography.title)
                Text(
                    "${attempt.startedAt.localDate()} — ${attempt.endedAt?.localDate() ?: "ongoing"}",
                    style = DeftTypography.mono,
                )
                val sessions =
                    snapshot.sessions.filter {
                        it.readThroughId == attempt.id &&
                            it.activeSeconds >= MIN_SESSION_SECONDS
                    }
                val counted = sessions.filterNot { it.browsing }
                Text(
                    "${durationLabel(counted.sumOf { it.activeSeconds })} · ${sessionCountLabel(counted.size)}",
                    style = DeftTypography.body,
                )
                FlowRowActions {
                    DeftButton("sessions", {
                        onAction(attempt.id, "sessions")
                    }, style = DeftButtonStyle.Text)
                    DeftButton(
                        "edit dates",
                        {
                            onAction(attempt.id, "dates")
                        },
                        style = DeftButtonStyle.Text,
                        enabled = !busy,
                        icon = DeftActionIcons.Rename,
                    )
                    DeftButton(
                        "delete",
                        {
                            onAction(attempt.id, "delete")
                        },
                        style = DeftButtonStyle.Text,
                        enabled = !busy,
                        icon = DeftActionIcons.Trash,
                    )
                }
            }
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
                    it.readThroughId == readThroughId && it.activeSeconds >= MIN_SESSION_SECONDS
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
