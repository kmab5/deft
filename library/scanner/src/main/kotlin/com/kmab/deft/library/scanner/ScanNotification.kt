package com.kmab.deft.library.scanner

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.pm.ServiceInfo
import android.os.Build
import androidx.work.ForegroundInfo
import kotlinx.coroutines.CancellationException

internal class ScanNotification(
    private val context: Context,
    private val cancel: PendingIntent,
) {
    fun foregroundInfo(processedFiles: Int): ForegroundInfo {
        val manager = context.getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(
            NotificationChannel(CHANNEL, "Library scans", NotificationManager.IMPORTANCE_LOW),
        )
        val builder =
            Notification
                .Builder(context, CHANNEL)
                .setSmallIcon(R.drawable.ic_scan_notification)
                .setContentTitle("scanning your library")
                .setContentText("${checkedFiles(processedFiles)} · you can leave Deft")
                .setOngoing(true)
                .setOnlyAlertOnce(true)
                .setProgress(0, 0, true)
                .addAction(Notification.Action.Builder(null, "cancel", cancel).build())
        context.packageManager.getLaunchIntentForPackage(context.packageName)?.let { intent ->
            builder.setContentIntent(
                PendingIntent.getActivity(
                    context,
                    0,
                    intent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
                ),
            )
        }
        val notification = builder.build()
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ForegroundInfo(NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC)
        } else {
            ForegroundInfo(NOTIFICATION_ID, notification)
        }
    }

    private companion object {
        const val CHANNEL = "library-scan"
        const val NOTIFICATION_ID = 1001
    }
}

/** App visibility can change between a tap and worker execution. Keep the bounded fallback. */
internal suspend fun promoteScan(
    userInitiated: Boolean,
    promote: suspend () -> Unit,
): Boolean {
    if (!userInitiated) return false
    return try {
        promote()
        true
    } catch (cancelled: CancellationException) {
        throw cancelled
    } catch (_: IllegalStateException) {
        false
    } catch (_: SecurityException) {
        false
    }
}

internal fun shouldYieldScan(
    foreground: Boolean,
    elapsedMillis: Long,
): Boolean = !foreground && elapsedMillis >= BACKGROUND_SCAN_BUDGET_MS

internal fun canContinueScan(runAttemptCount: Int): Boolean = runAttemptCount < MAX_BACKGROUND_RETRIES

private const val BACKGROUND_SCAN_BUDGET_MS = 4 * 60 * 1000L
private const val MAX_BACKGROUND_RETRIES = 5
