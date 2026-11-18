package com.kmab.deft.feature.settings

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import com.kmab.deft.core.designsystem.DeftButton
import com.kmab.deft.core.designsystem.DeftButtonStyle
import com.kmab.deft.core.designsystem.DeftTypography

/** Notifications are optional; refusing them must never prevent a library scan. */
@Composable
internal fun ScanNotificationPermission() {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return
    val context = LocalContext.current
    var allowed by remember {
        mutableStateOf(
            context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED,
        )
    }
    var requested by rememberSaveable { mutableStateOf(false) }
    val permission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { allowed = it }
    if (!allowed) {
        Text("Show scan progress when you leave Deft.", style = DeftTypography.body)
        DeftButton(
            if (requested) "notification settings" else "allow scan notifications",
            {
                if (requested) {
                    context.startActivity(
                        Intent(
                            Settings.ACTION_APP_NOTIFICATION_SETTINGS,
                        ).putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName),
                    )
                } else {
                    requested = true
                    permission.launch(Manifest.permission.POST_NOTIFICATIONS)
                }
            },
            style = DeftButtonStyle.Text,
        )
    }
}
