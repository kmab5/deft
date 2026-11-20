package com.kmab.deft.core.designsystem

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.disabled
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.setProgress
import androidx.compose.ui.unit.dp
import java.util.Locale
import kotlin.math.cos
import kotlin.math.sin

/**
 * Ten half-star hit regions, plus a single adjustable TalkBack control. Star geometry follows the component
 * specification.
 */
@Suppress("MagicNumber")
@Composable
fun RatingBar(
    rating: Float,
    onRatingChange: (Float) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    val c = LocalDeftColors.current
    val value = (rating.coerceIn(0f, 5f) * 2).toInt() / 2f
    Row(
        modifier.semantics(mergeDescendants = true) {
            contentDescription = "rating"
            progressBarRangeInfo = ProgressBarRangeInfo(value, 0f..5f, 9)
            if (!enabled) {
                disabled()
            } else {
                setProgress {
                    onRatingChange((it.coerceIn(0f, 5f) * 2).toInt() / 2f)
                    true
                }
            }
        },
        verticalAlignment = Alignment.CenterVertically,
    ) {
        repeat(5) { index ->
            Box(
                Modifier.size(48.dp).pointerInput(index, enabled, onRatingChange) {
                    if (!enabled) return@pointerInput
                    detectTapGestures { offset -> onRatingChange(index + if (offset.x < size.width / 2f) 0.5f else 1f) }
                },
                contentAlignment = Alignment.Center,
            ) {
                Canvas(Modifier.size(24.dp)) {
                    val path = Path()
                    repeat(10) { point ->
                        val angle = Math.PI * point / 5 - Math.PI / 2
                        val radius = size.width * if (point % 2 == 0) 0.43f else 0.205f
                        val x = center.x + cos(angle).toFloat() * radius
                        val y = center.y + size.height * 0.04f + sin(angle).toFloat() * radius
                        if (point == 0) path.moveTo(x, y) else path.lineTo(x, y)
                    }
                    path.close()
                    val outline = Stroke(1.75.dp.toPx())
                    drawPath(path, c.muted, style = outline)
                    clipRect(right = size.width * (value - index).coerceIn(0f, 1f)) {
                        drawPath(path, c.accent)
                        drawPath(path, c.accent, style = outline)
                    }
                }
            }
        }
        Text(String.format(Locale.ROOT, "%.1f", value), style = DeftTypography.mono, color = c.text)
    }
}
