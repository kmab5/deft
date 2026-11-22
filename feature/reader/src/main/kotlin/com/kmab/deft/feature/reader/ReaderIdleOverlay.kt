@file:Suppress("MagicNumber") // Quiet idle-film opacity and slow visual pulse.

package com.kmab.deft.feature.reader

import android.animation.ValueAnimator
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.kmab.deft.core.designsystem.DeftTypography

@Composable
internal fun ReaderIdleOverlay(
    visible: Boolean,
    onResume: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val resume by rememberUpdatedState(onResume)
    AnimatedVisibility(
        visible,
        modifier,
        enter = fadeIn(tween(250)),
        exit = fadeOut(tween(150)),
    ) {
        val alpha = idleLabelAlpha()
        Box(
            Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.55f))
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    role = Role.Button,
                    onClickLabel = "resume reading",
                    onClick = onResume,
                ).pointerInput(Unit) {
                    awaitEachGesture {
                        awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial).consume()
                        do {
                            val event = awaitPointerEvent(PointerEventPass.Initial)
                            event.changes.forEach { it.consume() }
                        } while (event.changes.any { it.pressed })
                        resume()
                    }
                }.semantics { contentDescription = "resume reading" },
            contentAlignment = Alignment.Center,
        ) {
            Box(
                Modifier
                    .fillMaxWidth()
                    .background(Color.Black.copy(alpha = 0.7f * alpha.value))
                    .padding(vertical = 16.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    "idling",
                    style = DeftTypography.mono,
                    color = Color.White,
                    modifier = Modifier.graphicsLayer { this.alpha = 0.65f + 0.35f * alpha.value },
                )
            }
        }
    }
}

@Composable
private fun idleLabelAlpha(): State<Float> {
    if (!ValueAnimator.areAnimatorsEnabled()) return rememberUpdatedState(1f)
    val pulse = rememberInfiniteTransition(label = "idle pulse")
    return pulse.animateFloat(
        0f,
        1f,
        infiniteRepeatable(tween(1400), RepeatMode.Reverse),
        label = "idle label opacity",
    )
}
