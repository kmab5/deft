package com.kmab.deft.feature.book

import android.app.DatePickerDialog
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.kmab.deft.core.designsystem.DeftButton
import com.kmab.deft.core.designsystem.DeftTypography
import com.kmab.deft.core.designsystem.LocalDeftColors
import com.kmab.deft.core.designsystem.icons.DeftActionIcons
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

@Composable
internal fun TrackingDates(
    initialStart: Instant,
    initialEnd: Instant?,
    dismiss: () -> Unit,
    save: (Instant, Instant?) -> Unit,
) {
    var start by rememberSaveable { mutableStateOf(initialStart.localDate()) }
    var end by rememberSaveable { mutableStateOf(initialEnd?.localDate()) }
    var picking by rememberSaveable { mutableStateOf<String?>(null) }
    var invalidRange by rememberSaveable { mutableStateOf(false) }
    TrackingSheet("edit dates", dismiss) {
        DateField("started", start) { picking = "started" }
        end?.let { DateField("ended", it) { picking = "ended" } }
        if (invalidRange) {
            Text(
                "The end date must be on or after the start date.",
                style = DeftTypography.body,
                color = LocalDeftColors.current.abandoned,
            )
        }
        DeftButton("save date", {
            val endDay = end?.let(LocalDate::parse)
            invalidRange = endDay?.isBefore(LocalDate.parse(start)) == true
            if (!invalidRange) {
                val from = dateInstant(start, initialStart)
                val to = initialEnd?.let { dateInstant(checkNotNull(end), it).coerceAtLeast(from) }
                save(from, to)
            }
        }, icon = DeftActionIcons.Check)
    }
    picking?.let { field ->
        CalendarDialog(
            if (field == "started") start else checkNotNull(end),
            { picking = null },
        ) { date ->
            if (field == "started") start = date else end = date
            invalidRange = false
            picking = null
        }
    }
}

@Composable
private fun DateField(
    label: String,
    date: String,
    choose: () -> Unit,
) {
    val c = LocalDeftColors.current
    Row(
        Modifier.fillMaxWidth().heightIn(min = 48.dp).clickable(role = Role.Button, onClick = choose),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(label, style = DeftTypography.label)
            Text(
                LocalDate.parse(date).format(DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM)),
                style = DeftTypography.mono,
                color = c.accentText,
            )
        }
        Icon(DeftActionIcons.ChevronRight, null, tint = c.accentText)
    }
}

private const val DARK_SURFACE_LUMINANCE = 0.5f

@Composable
internal fun CalendarDialog(
    initial: String,
    dismiss: () -> Unit,
    select: (String) -> Unit,
) {
    val context = LocalContext.current
    val onDismiss by rememberUpdatedState(dismiss)
    val onSelect by rememberUpdatedState(select)
    val style =
        if (LocalDeftColors.current.surface.luminance() < DARK_SURFACE_LUMINANCE) {
            android.R.style.Theme_Material_Dialog_Alert
        } else {
            android.R.style.Theme_Material_Light_Dialog_Alert
        }
    DisposableEffect(context, initial, style) {
        val date = LocalDate.parse(initial)
        val dialog =
            DatePickerDialog(
                context,
                style,
                { _, year, month, day -> onSelect(LocalDate.of(year, month + 1, day).toString()) },
                date.year,
                date.monthValue - 1,
                date.dayOfMonth,
            )
        dialog.setOnDismissListener { onDismiss() }
        dialog.show()
        onDispose {
            dialog.setOnDismissListener(null)
            dialog.dismiss()
        }
    }
}

internal fun dateInstant(
    text: String,
    original: Instant,
): Instant {
    val zone = ZoneId.systemDefault()
    return LocalDate
        .parse(text)
        .atTime(original.atZone(zone).toLocalTime())
        .atZone(zone)
        .toInstant()
}

internal fun Instant.localDate(): String = atZone(ZoneId.systemDefault()).toLocalDate().toString()
