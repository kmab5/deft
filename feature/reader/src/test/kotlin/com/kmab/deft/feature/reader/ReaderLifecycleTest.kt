package com.kmab.deft.feature.reader

import android.app.Activity
import android.view.KeyEvent
import android.view.WindowManager
import com.kmab.deft.core.model.ReaderPreferences
import com.kmab.deft.core.model.ReadingPosition
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class ReaderLifecycleTest {
    @Test fun pauseRestoresWindowAndResumeReappliesGlobalReaderDefaults() {
        val activity = Robolectric.buildActivity(Activity::class.java).setup().get()
        val window = ReaderWindow(activity.window)
        val defaults = ReaderPreferences(useSystemBrightness = false, brightness = 0.25f, keepScreenOn = true)
        window.apply(defaults)
        assertEquals(0.25f, activity.window.attributes.screenBrightness, 0f)
        assertTrue(activity.window.attributes.flags and WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON != 0)
        window.restore()
        assertEquals(
            WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_NONE,
            activity.window.attributes.screenBrightness,
            0f,
        )
        assertFalse(activity.window.attributes.flags and WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON != 0)
        window.apply(defaults)
        assertEquals(0.25f, activity.window.attributes.screenBrightness, 0f)
        window.apply(defaults.copy(useSystemBrightness = true, keepScreenOn = false))
        assertEquals(
            WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_NONE,
            activity.window.attributes.screenBrightness,
            0f,
        )
        assertFalse(activity.window.attributes.flags and WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON != 0)
    }

    @Test fun volumeKeyReleaseIsConsumedEvenIfSettingChangesDuringPress() {
        val keys = ReaderVolumeKeys()
        val turns = mutableListOf<Boolean>()
        assertFalse(keys.handle(KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_VOLUME_DOWN), false, turns::add))
        assertTrue(keys.handle(KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_VOLUME_DOWN), true, turns::add))
        assertTrue(keys.handle(KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_VOLUME_DOWN), false, turns::add))
        assertEquals(listOf(true), turns)
        assertFalse(keys.handle(KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_VOLUME_DOWN), false, turns::add))
    }

    @Test fun volumePageHapticOccursOncePerEnabledPressAndNeverOnReleaseOrRepeat() {
        var ticks = 0
        val turns = mutableListOf<Boolean>()
        val keys = ReaderVolumeKeys { ticks++ }
        keys.handle(KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_VOLUME_DOWN), false, turns::add)
        keys.handle(KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_VOLUME_DOWN), true, turns::add)
        keys.handle(KeyEvent(0, 10, KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_VOLUME_DOWN, 1), true, turns::add)
        keys.handle(KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_VOLUME_DOWN), true, turns::add)
        assertEquals(1, ticks)
        assertEquals(listOf(true), turns)
    }

    @Test fun scrollWritesCoalescePerBookWithoutDroppingAnotherBooksFinalPosition() {
        val pending = PendingPositions()
        repeat(1000) { pending.offer(1, ReadingPosition("location-$it", it / 1000.0)) }
        pending.offer(2, ReadingPosition("other-book", 0.5))
        assertEquals(1L to ReadingPosition("location-999", 0.999), pending.take())
        assertEquals(2L to ReadingPosition("other-book", 0.5), pending.take())
        assertNull(pending.take())
    }
}
