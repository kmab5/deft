@file:Suppress("MatchingDeclarationName") // ReaderBattery is supporting state for the status-line UI.

package com.kmab.deft.feature.reader

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import android.text.format.DateFormat
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.kmab.deft.core.designsystem.icons.DeftActionIcons
import com.kmab.deft.core.model.ReaderPreferences
import com.kmab.deft.core.model.TrackingPreferences
import kotlinx.coroutines.delay
import java.util.Date

internal data class ReaderBattery(
    val percentage: Int?,
    val charging: Boolean,
)

internal fun readerBattery(intent: Intent?): ReaderBattery {
    val level = intent?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
    val scale = intent?.getIntExtra(BatteryManager.EXTRA_SCALE, -1) ?: -1
    val status = intent?.getIntExtra(BatteryManager.EXTRA_STATUS, -1)
    return ReaderBattery(
        percentage = if (level >= 0 && scale > 0) (level * PERCENT_SCALE / scale).coerceIn(0, PERCENT_SCALE) else null,
        charging = status == BatteryManager.BATTERY_STATUS_CHARGING || status == BatteryManager.BATTERY_STATUS_FULL,
    )
}

@Composable
internal fun ReaderStatusFooter(
    state: ReaderState,
    positions: List<org.readium.r2.shared.publication.Locator>,
    preferences: ReaderPreferences,
    trackingPreferences: TrackingPreferences,
    tracking: ReaderTrackingState,
) {
    val showTracking = trackingPreferences.showIndicator && state.rendered
    if (preferences.showStatusLine || showTracking) {
        ReaderStatus(
            progress =
                if (preferences.showStatusLine) {
                    progressLabel(
                        (state.locator?.locations?.totalProgression ?: 0.0).toFloat(),
                        positions,
                        preferences.showPageNumber,
                    )
                } else {
                    null
                },
            tracking = tracking.active.takeIf { showTracking },
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable
internal fun ReaderStatus(
    progress: String?,
    tracking: Boolean?,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    var battery by remember { mutableStateOf(ReaderBattery(null, false)) }
    DisposableEffect(context) {
        val receiver =
            object : BroadcastReceiver() {
                override fun onReceive(
                    context: Context?,
                    intent: Intent?,
                ) {
                    battery = readerBattery(intent)
                }
            }
        val sticky = context.registerReceiver(receiver, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
        battery = readerBattery(sticky)
        onDispose { context.unregisterReceiver(receiver) }
    }
    val time by produceState(initialValue = DateFormat.getTimeFormat(context).format(Date()), context) {
        while (true) {
            value = DateFormat.getTimeFormat(context).format(Date())
            delay(STATUS_REFRESH_MILLIS)
        }
    }
    ReaderStatusContent(progress, tracking, time, battery, modifier)
}

@Composable
internal fun ReaderStatusContent(
    progress: String?,
    tracking: Boolean?,
    time: String,
    battery: ReaderBattery,
    modifier: Modifier = Modifier,
) {
    val ink = MaterialTheme.colorScheme.onBackground
    val style = MaterialTheme.typography.labelSmall
    Row(
        modifier.padding(horizontal = 16.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Text(progress.orEmpty(), Modifier.weight(1f), style = style, color = ink)
        Text(
            when (tracking) {
                true -> "reading"
                false -> "paused"
                null -> ""
            },
            Modifier.weight(1f).testTag("tracking_status"),
            style = style,
            textAlign = TextAlign.Center,
            color = ink.copy(alpha = 0.75f),
        )
        Row(
            Modifier.weight(1f),
            horizontalArrangement = Arrangement.End,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (progress != null) {
                Text(
                    listOfNotNull(time, battery.percentage?.let { "$it%" }).joinToString(" · "),
                    Modifier.weight(1f, fill = false),
                    style = style,
                    textAlign = TextAlign.End,
                    color = ink,
                )
                if (battery.charging) Icon(DeftActionIcons.Charging, "charging", Modifier.size(12.dp), tint = ink)
            }
        }
    }
}

private const val PERCENT_SCALE = 100
private const val STATUS_REFRESH_MILLIS = 30_000L
