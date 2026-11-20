package com.kmab.deft.feature.reader

import android.view.KeyEvent
import android.view.Window
import android.view.WindowManager
import com.kmab.deft.core.model.ReaderPreferences

internal class ReaderWindow(
    private val window: Window,
) {
    private val originalBrightness = window.attributes.screenBrightness
    private val originalKeepAwake = window.attributes.flags and WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON != 0

    fun apply(preferences: ReaderPreferences) {
        window.attributes =
            window.attributes.apply {
                screenBrightness =
                    if (preferences.useSystemBrightness) {
                        WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_NONE
                    } else {
                        preferences.brightness
                    }
            }
        if (preferences.keepScreenOn) {
            window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        } else {
            window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
    }

    fun restore() {
        window.attributes = window.attributes.apply { screenBrightness = originalBrightness }
        if (originalKeepAwake) {
            window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        } else {
            window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
    }
}

internal class ReaderVolumeKeys {
    private val held = mutableSetOf<Int>()

    fun handle(
        event: KeyEvent,
        enabled: Boolean,
        turn: (Boolean) -> Unit,
    ): Boolean {
        val volume = event.keyCode == KeyEvent.KEYCODE_VOLUME_UP || event.keyCode == KeyEvent.KEYCODE_VOLUME_DOWN
        if (!volume || !enabled && event.keyCode !in held) return false
        if (event.action == KeyEvent.ACTION_DOWN) {
            held += event.keyCode
            if (event.repeatCount == 0) turn(event.keyCode == KeyEvent.KEYCODE_VOLUME_DOWN)
        } else if (event.action == KeyEvent.ACTION_UP) {
            held -= event.keyCode
        }
        return true
    }

    fun reset() {
        held.clear()
    }
}
