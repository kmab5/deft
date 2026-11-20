package com.kmab.deft.feature.settings

import android.Manifest
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.unit.dp
import androidx.core.app.NotificationManagerCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.kmab.deft.core.designsystem.DeftShapes
import com.kmab.deft.core.designsystem.DeftTypography
import com.kmab.deft.core.designsystem.LocalDeftColors
import com.kmab.deft.core.designsystem.icons.DeftActionIcons
import com.kmab.deft.library.scanner.SCAN_NOTIFICATION_CHANNEL_ID

/** Notifications are optional; refusing them must never prevent a library scan. */
@Composable
internal fun ScanNotificationPermission() {
    val context = LocalContext.current
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    var allowed by remember { mutableStateOf(context.scanNotificationsAllowed()) }
    var requested by rememberSaveable { mutableStateOf(false) }
    val permission =
        rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
            allowed = context.scanNotificationsAllowed()
        }
    DisposableEffect(lifecycle, context) {
        val observer =
            LifecycleEventObserver { _, event ->
                if (event == Lifecycle.Event.ON_RESUME) allowed = context.scanNotificationsAllowed()
            }
        lifecycle.addObserver(observer)
        onDispose { lifecycle.removeObserver(observer) }
    }
    ScanNotificationControl(allowed, requested) {
        if (requested || context.notificationPermissionGranted()) {
            context.startActivity(context.scanNotificationSettingsIntent())
        } else {
            requested = true
            permission.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }
}

@Composable
private fun ScanNotificationControl(
    allowed: Boolean,
    requested: Boolean,
    onClick: () -> Unit,
) {
    val colors = LocalDeftColors.current
    Surface(
        onClick = onClick,
        modifier =
            Modifier.fillMaxWidth().heightIn(min = 48.dp).testTag("scan-notifications").semantics {
                stateDescription = if (allowed) "notifications enabled" else "notifications disabled"
            },
        shape = DeftShapes.card,
        color = colors.surface2,
        border = BorderStroke(1.dp, colors.line2),
        shadowElevation = 2.dp,
    ) {
        Row(
            Modifier.padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                if (allowed) DeftActionIcons.Notifications else DeftActionIcons.NotificationsOff,
                null,
                Modifier.size(24.dp),
                tint = if (allowed) colors.text else colors.muted,
            )
            Text(
                when {
                    allowed -> "scan notifications on"
                    requested -> "notification settings"
                    else -> "allow scan notifications"
                },
                style = DeftTypography.label,
                color = colors.text,
            )
        }
    }
}

internal fun Context.scanNotificationsAllowed(): Boolean {
    val channel = getSystemService(NotificationManager::class.java).getNotificationChannel(SCAN_NOTIFICATION_CHANNEL_ID)
    val channelEnabled = channel?.importance != NotificationManager.IMPORTANCE_NONE
    return notificationPermissionGranted() && NotificationManagerCompat.from(this).areNotificationsEnabled() &&
        channelEnabled
}

private fun Context.notificationPermissionGranted(): Boolean =
    Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
        checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED

private fun Context.scanNotificationSettingsIntent(): Intent {
    val manager = getSystemService(NotificationManager::class.java)
    val channelBlocked =
        manager.getNotificationChannel(SCAN_NOTIFICATION_CHANNEL_ID)?.importance ==
            NotificationManager.IMPORTANCE_NONE
    val action =
        if (channelBlocked && manager.areNotificationsEnabled()) {
            Settings.ACTION_CHANNEL_NOTIFICATION_SETTINGS
        } else {
            Settings.ACTION_APP_NOTIFICATION_SETTINGS
        }
    return Intent(action)
        .putExtra(Settings.EXTRA_APP_PACKAGE, packageName)
        .putExtra(Settings.EXTRA_CHANNEL_ID, SCAN_NOTIFICATION_CHANNEL_ID)
}
