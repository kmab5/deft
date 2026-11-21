package com.kmab.deft.core.designsystem

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
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
import kotlin.math.ceil
import kotlin.math.cos
import kotlin.math.sin

/**
 * One 48dp-high adjustable control with ten half-star hit regions. The stars share the available
 * width, keeping touch and TalkBack input usable alongside the other assessment controls.
 */
@Suppress("MagicNumber")
@Composable
fun RatingBar(
    rating: Float,
    onRatingChange: (Float) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    showValue: Boolean = true,
) {
    val c = LocalDeftColors.current
    val value = (rating.coerceIn(0f, 5f) * 2).toInt() / 2f
    Row(
        modifier.width(240.dp).height(48.dp).semantics(mergeDescendants = true) {
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
        Row(
            Modifier.weight(1f).fillMaxHeight().pointerInput(enabled, onRatingChange) {
                if (!enabled) return@pointerInput
                detectTapGestures { offset ->
                    val selected = (ceil(offset.x / size.width * 10f) / 2f).coerceIn(0.5f, 5f)
                    onRatingChange(selected)
                }
            },
        ) {
            repeat(5) { index ->
                Box(
                    Modifier.weight(1f).fillMaxHeight(),
                    contentAlignment = Alignment.Center,
                ) {
                    Canvas(Modifier.size(28.dp)) {
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
        }
        if (showValue) {
            Text(
                String.format(Locale.ROOT, "%.1f", value),
                Modifier.padding(start = 8.dp),
                style = DeftTypography.mono,
                color = c.text,
            )
        }
    }
}
