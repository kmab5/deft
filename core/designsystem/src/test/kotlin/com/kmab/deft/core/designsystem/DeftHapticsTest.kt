package com.kmab.deft.core.designsystem

import android.content.Context
import android.os.Build
import android.provider.Settings
import android.view.HapticFeedbackConstants
import android.view.View
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.util.ReflectionHelpers

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class DeftHapticsTest {
    @Test fun actionsUseSubtleEffectsWithoutBypassingSettings() {
        val view = RecordingHapticView(ApplicationProvider.getApplicationContext())
        for (event in DeftHaptic.entries) assertTrue(view.performDeftHaptic(event))
        assertEquals(
            listOf(
                HapticFeedbackConstants.CONTEXT_CLICK,
                HapticFeedbackConstants.CLOCK_TICK,
                HapticFeedbackConstants.CONFIRM,
                HapticFeedbackConstants.LONG_PRESS,
            ),
            view.effects,
        )
        assertTrue(view.flags.all { it == 0 })
    }

    @Test fun disabledSystemOrViewFeedbackIsSilent() {
        val view = RecordingHapticView(ApplicationProvider.getApplicationContext())
        Settings.System.putInt(view.context.contentResolver, Settings.System.HAPTIC_FEEDBACK_ENABLED, 0)
        assertFalse(view.performDeftHaptic(DeftHaptic.SELECTION))
        Settings.System.putInt(view.context.contentResolver, Settings.System.HAPTIC_FEEDBACK_ENABLED, 1)
        view.isHapticFeedbackEnabled = false
        assertFalse(view.performDeftHaptic(DeftHaptic.BACK))
        assertTrue(view.effects.isEmpty())
    }

    @Test fun confirmationChoosesFallbackForOlderApiLevel() {
        val view = RecordingHapticView(ApplicationProvider.getApplicationContext())
        val sdk = Build.VERSION.SDK_INT
        try {
            ReflectionHelpers.setStaticField(Build.VERSION::class.java, "SDK_INT", 28)
            assertTrue(view.performDeftHaptic(DeftHaptic.CONFIRM))
            assertEquals(listOf(HapticFeedbackConstants.CONTEXT_CLICK), view.effects)
        } finally {
            ReflectionHelpers.setStaticField(Build.VERSION::class.java, "SDK_INT", sdk)
        }
    }
}

internal class RecordingHapticView(
    context: Context,
) : View(context) {
    val effects = mutableListOf<Int>()
    val flags = mutableListOf<Int>()

    override fun performHapticFeedback(feedbackConstant: Int): Boolean = performHapticFeedback(feedbackConstant, 0)

    override fun performHapticFeedback(
        feedbackConstant: Int,
        flags: Int,
    ): Boolean {
        effects += feedbackConstant
        this.flags += flags
        return true
    }
}
