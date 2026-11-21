package com.kmab.deft.feature.reader

import android.os.Build
import android.view.HapticFeedbackConstants
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue

@Composable
internal fun ReaderTrackingEffects(
    model: ReaderViewModel,
    activity: ReaderActivity,
    state: ReaderState,
    tracking: ReaderTrackingState,
    overlayOpen: Boolean,
) {
    var confirmed by rememberSaveable(state.completedBookId) { mutableStateOf(false) }
    LaunchedEffect(state.completedBookId) {
        if (state.completedBookId != null) {
            if (!confirmed) {
                confirmed = true
                activity.window.decorView.performHapticFeedback(
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                        HapticFeedbackConstants.CONFIRM
                    } else {
                        HapticFeedbackConstants.CONTEXT_CLICK
                    },
                )
            }
            activity.finish()
        }
    }
    SideEffect { model.interaction.obscured(overlayOpen || tracking.finishPrompt) }
}
