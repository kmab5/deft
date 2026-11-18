package com.kmab.deft.feature.reader

import android.os.Bundle
import android.view.KeyEvent
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.fragment.app.FragmentActivity
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class ReaderActivity : FragmentActivity() {
    private val model: ReaderViewModel by viewModels()
    internal var navigator: ReaderNavigator? = null
    private val volumeKeys = ReaderVolumeKeys()
    private lateinit var readerWindow: ReaderWindow

    override fun onCreate(savedInstanceState: Bundle?) {
        // Readium fragments require a live publication in their constructor. Rebuild that fragment
        // explicitly; restore only our locator, never Android's constructor-less fragment state.
        super.onCreate(null)
        readerWindow = ReaderWindow(window)
        enableEdgeToEdge()
        model.open(intent.getLongExtra(ReaderLauncher.BOOK_ID, -1L), savedInstanceState?.getString(LOCATION))
        setContent { ReaderScreen(model, this) }
    }

    override fun onResume() {
        super.onResume()
        model.interaction.visibility(true)
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
        navigator?.detach()
        navigator = null
        resetWindow()
        super.onDestroy()
    }

    internal fun applyWindow(
        chrome: Boolean,
        dark: Boolean,
    ) {
        readerWindow.apply(model.preferences.value)
        WindowCompat.getInsetsController(window, window.decorView).apply {
            systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            isAppearanceLightStatusBars = !dark
            isAppearanceLightNavigationBars = !dark
            if (chrome) show(WindowInsetsCompat.Type.systemBars()) else hide(WindowInsetsCompat.Type.systemBars())
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
