package com.kmab.deft.feature.reader

import android.app.Activity
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.ActivityInfo
import android.os.Bundle
import android.os.PowerManager
import android.view.HapticFeedbackConstants
import android.view.KeyEvent
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.fragment.app.FragmentActivity
import com.kmab.deft.core.designsystem.DeftHaptic
import com.kmab.deft.core.designsystem.performDeftHaptic
import com.kmab.deft.core.model.ReaderOrientation
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
@Suppress("TooManyFunctions") // Android lifecycle, window and key callbacks belong to their Activity host.
class ReaderActivity : FragmentActivity() {
    private val model: ReaderViewModel by viewModels()
    internal var navigator: ReaderNavigator? = null
    private val volumeKeys =
        ReaderVolumeKeys {
            window.decorView.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
        }
    private lateinit var readerWindow: ReaderWindow
    private val screenReceiver =
        object : BroadcastReceiver() {
            override fun onReceive(
                context: Context?,
                intent: Intent?,
            ) {
                if (intent?.action == Intent.ACTION_SCREEN_OFF) model.interaction.visibility(false)
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        // Readium fragments require a live publication in their constructor. Rebuild that fragment
        // explicitly while retaining Compose controls and the rest of Android's saved state.
        super.onCreate(savedInstanceState?.withoutReaderFragmentState())
        readerWindow = ReaderWindow(window)
        ContextCompat.registerReceiver(
            this,
            screenReceiver,
            IntentFilter(Intent.ACTION_SCREEN_OFF),
            ContextCompat.RECEIVER_NOT_EXPORTED,
        )
        enableEdgeToEdge()
        model.open(
            intent.getLongExtra(ReaderLauncher.BOOK_ID, -1L),
            savedInstanceState?.getString(LOCATION) ?: intent.getStringExtra(ReaderLauncher.LOCATOR),
        )
        setContent { ReaderScreen(model, this) }
    }

    override fun onResume() {
        super.onResume()
        model.interaction.visibility(getSystemService(PowerManager::class.java).isInteractive)
    }

    override fun finish() {
        // Both toolbar Back and committed system Back reach here. Native selection still
        // consumes its own Back, and automatic completion already plays confirmation.
        if (!isFinishing && model.state.value.completedBookId == null) {
            window.decorView.performDeftHaptic(DeftHaptic.BACK)
        }
        super.finish()
    }

    override fun onPause() {
        model.interaction.visibility(false)
        resetWindow()
        super.onPause()
    }

    override fun onStop() {
        model.interaction.visibility(false)
        resetWindow()
        super.onStop()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        model.state.value.locator
            ?.let { outState.putString(LOCATION, it.toJSON().toString()) }
        super.onSaveInstanceState(outState)
    }

    override fun onDestroy() {
        unregisterReceiver(screenReceiver)
        navigator?.detach()
        navigator = null
        resetWindow()
        super.onDestroy()
    }

    internal fun applyWindow(
        chrome: Boolean,
        dark: Boolean,
    ) {
        val preferences = model.preferences.value
        val tracking = model.trackingState.value
        val keepAwake = tracking.keepAwake && model.state.value.rendered && !tracking.finishPrompt
        readerWindow.apply(preferences.copy(keepScreenOn = preferences.keepScreenOn && keepAwake))
        applyReaderOrientation(preferences.orientation)
        WindowCompat.getInsetsController(window, window.decorView).apply {
            systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            isAppearanceLightStatusBars = !dark
            isAppearanceLightNavigationBars = !dark
            if (chrome || !preferences.fullScreen) {
                show(WindowInsetsCompat.Type.systemBars())
            } else {
                hide(WindowInsetsCompat.Type.systemBars())
            }
        }
    }

    override fun onKeyDown(
        keyCode: Int,
        event: KeyEvent,
    ): Boolean =
        volumeKeys.handle(event, model.preferences.value.volumeKeys && navigator != null) { navigator?.turn(it) } ||
            super.onKeyDown(keyCode, event)

    override fun onKeyUp(
        keyCode: Int,
        event: KeyEvent,
    ): Boolean =
        volumeKeys.handle(event, model.preferences.value.volumeKeys && navigator != null) { navigator?.turn(it) } ||
            super.onKeyUp(keyCode, event)

    private fun resetWindow() {
        readerWindow.restore()
        volumeKeys.reset()
    }

    private companion object {
        const val LOCATION = "reader-location"
    }
}

/** FragmentManager alone needs fresh state because its fragment factory holds the live book. */
internal fun Bundle.withoutReaderFragmentState(): Bundle =
    Bundle(this).apply {
        remove("android:support:fragments")
        val registryKey = "androidx.lifecycle.BundlableSavedStateRegistry.key"
        getBundle(registryKey)?.let { registry ->
            putBundle(registryKey, Bundle(registry).apply { remove("android:support:fragments") })
        }
    }

internal fun Activity.applyReaderOrientation(orientation: ReaderOrientation) {
    val requested =
        when (orientation) {
            ReaderOrientation.SYSTEM -> ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
            ReaderOrientation.PORTRAIT -> ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
            ReaderOrientation.LANDSCAPE -> ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
        }
    if (requestedOrientation != requested) requestedOrientation = requested
}
