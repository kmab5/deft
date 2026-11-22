package com.kmab.deft

import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import kotlin.math.abs

internal fun topLevelSwipeTarget(
    current: Int,
    distance: Float,
    threshold: Float,
): Int? {
    if (current !in 0..2 || abs(distance) < threshold) return null
    return (current + if (distance < 0) 1 else -1).takeIf { it in 0..2 }
}

internal fun Modifier.topLevelSwipe(
    current: Int,
    onSelect: (Int) -> Unit,
): Modifier =
    if (current < 0) {
        this
    } else {
        pointerInput(current) {
            var distance = 0f
            detectHorizontalDragGestures(
                onDragStart = { distance = 0f },
                onDragCancel = { distance = 0f },
                onDragEnd = { topLevelSwipeTarget(current, distance, 72.dp.toPx())?.let(onSelect) },
                onHorizontalDrag = { change, amount ->
                    change.consume()
                    distance += amount
                },
            )
        }
    }
