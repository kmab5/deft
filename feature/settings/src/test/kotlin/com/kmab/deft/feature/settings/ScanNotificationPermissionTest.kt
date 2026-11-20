package com.kmab.deft.feature.settings

import android.Manifest
import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.provider.Settings
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.test.core.app.ApplicationProvider
import com.kmab.deft.core.designsystem.DeftTheme
import com.kmab.deft.library.scanner.SCAN_NOTIFICATION_CHANNEL_ID
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ScanNotificationPermissionTest {
    @get:Rule val compose = createComposeRule()
    private val application = ApplicationProvider.getApplicationContext<Application>()
    private val manager = application.getSystemService(NotificationManager::class.java)

    @Test fun returningFromSettingsRefreshesPermissionAndAppNotificationState() {
        shadowOf(application).denyPermissions(Manifest.permission.POST_NOTIFICATIONS)
        shadowOf(manager).setNotificationsEnabled(true)
        val owner = ScreenLifecycle()
        compose.setContent {
            CompositionLocalProvider(LocalLifecycleOwner provides owner) {
                DeftTheme { ScanNotificationPermission() }
            }
        }
        assertState("notifications disabled")
        compose.runOnIdle {
            owner.lifecycle.currentState = Lifecycle.State.STARTED
            shadowOf(application).grantPermissions(Manifest.permission.POST_NOTIFICATIONS)
            owner.lifecycle.currentState = Lifecycle.State.RESUMED
        }
        assertState("notifications enabled")
        compose.onNodeWithTag("scan-notifications").performClick()
        assertEquals(Settings.ACTION_APP_NOTIFICATION_SETTINGS, shadowOf(application).nextStartedActivity.action)
        compose.runOnIdle {
            owner.lifecycle.currentState = Lifecycle.State.STARTED
            shadowOf(manager).setNotificationsEnabled(false)
            owner.lifecycle.currentState = Lifecycle.State.RESUMED
        }
        assertState("notifications disabled")
        compose.onNodeWithTag("scan-notifications").performClick()
        assertEquals(Settings.ACTION_APP_NOTIFICATION_SETTINGS, shadowOf(application).nextStartedActivity.action)
    }

    @Test fun blockedScanChannelOverridesGrantedPermissionAndOpensChannelSettings() {
        shadowOf(application).grantPermissions(Manifest.permission.POST_NOTIFICATIONS)
        shadowOf(manager).setNotificationsEnabled(true)
        val owner = ScreenLifecycle()
        compose.setContent {
            CompositionLocalProvider(LocalLifecycleOwner provides owner) {
                DeftTheme { ScanNotificationPermission() }
            }
        }
        assertState("notifications enabled")
        compose.runOnIdle {
            owner.lifecycle.currentState = Lifecycle.State.STARTED
            manager.createNotificationChannel(
                NotificationChannel(SCAN_NOTIFICATION_CHANNEL_ID, "Library scans", NotificationManager.IMPORTANCE_NONE),
            )
            owner.lifecycle.currentState = Lifecycle.State.RESUMED
        }
        assertState("notifications disabled")
        compose.onNodeWithTag("scan-notifications").performClick()
        val intent = shadowOf(application).nextStartedActivity
        assertEquals(Settings.ACTION_CHANNEL_NOTIFICATION_SETTINGS, intent.action)
        assertEquals(SCAN_NOTIFICATION_CHANNEL_ID, intent.getStringExtra(Settings.EXTRA_CHANNEL_ID))
    }

    @Test
    @Config(sdk = [32])
    fun earlierAndroidStillHonorsTheAppNotificationSwitch() {
        shadowOf(manager).setNotificationsEnabled(false)
        assertFalse(application.scanNotificationsAllowed())
        compose.setContent { DeftTheme { ScanNotificationPermission() } }
        assertState("notifications disabled")
        compose.onNodeWithTag("scan-notifications").performClick()
        assertEquals(Settings.ACTION_APP_NOTIFICATION_SETTINGS, shadowOf(application).nextStartedActivity.action)
    }

    private fun assertState(expected: String) {
        compose.onNodeWithTag("scan-notifications").assert(
            SemanticsMatcher.expectValue(SemanticsProperties.StateDescription, expected),
        )
    }

    private class ScreenLifecycle : LifecycleOwner {
        override val lifecycle = LifecycleRegistry.createUnsafe(this).apply { currentState = Lifecycle.State.RESUMED }
    }
}
