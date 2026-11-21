package com.kmab.deft.core.designsystem

import android.os.Build
import android.provider.Settings
import android.view.HapticFeedbackConstants
import android.view.View

enum class DeftHaptic { BACK, CHOICE, CONFIRM, SELECTION }

/** User actions only; Android remains responsible for device support and intensity. */
fun View.performDeftHaptic(event: DeftHaptic): Boolean {
    val enabled = Settings.System.getInt(context.contentResolver, Settings.System.HAPTIC_FEEDBACK_ENABLED, 1) != 0
    if (!isHapticFeedbackEnabled || !enabled) return false
    val effect =
        when (event) {
            DeftHaptic.BACK -> HapticFeedbackConstants.CONTEXT_CLICK
            DeftHaptic.CHOICE -> HapticFeedbackConstants.CLOCK_TICK
            DeftHaptic.SELECTION -> HapticFeedbackConstants.LONG_PRESS
            DeftHaptic.CONFIRM ->
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    HapticFeedbackConstants.CONFIRM
                } else {
                    HapticFeedbackConstants.CONTEXT_CLICK
                }
        }
    // The platform overload uses flags = 0, so neither view nor system settings are bypassed.
    return performHapticFeedback(effect)
}
