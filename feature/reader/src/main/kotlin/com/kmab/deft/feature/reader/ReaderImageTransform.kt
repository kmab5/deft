package com.kmab.deft.feature.reader

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.unit.IntSize
import kotlin.math.min

internal data class ReaderImageTransform(
    val scale: Float = 1f,
    val offset: Offset = Offset.Zero,
) {
    fun change(
        centroid: Offset,
        pan: Offset,
        zoom: Float,
        viewport: IntSize,
        image: IntSize,
    ): ReaderImageTransform {
        if (viewport.width == 0 || viewport.height == 0) return this
        val nextScale = (scale * zoom).coerceIn(1f, MAX_IMAGE_ZOOM)
        val focus = centroid - Offset(viewport.width / 2f, viewport.height / 2f)
        val shifted = (offset - focus) * (nextScale / scale) + focus + pan
        val fit = min(viewport.width.toFloat() / image.width, viewport.height.toFloat() / image.height)
        val boundX = ((image.width * fit * nextScale - viewport.width) / 2f).coerceAtLeast(0f)
        val boundY = ((image.height * fit * nextScale - viewport.height) / 2f).coerceAtLeast(0f)
        return if (nextScale == 1f) {
            ReaderImageTransform()
        } else {
            ReaderImageTransform(
                nextScale,
                Offset(shifted.x.coerceIn(-boundX, boundX), shifted.y.coerceIn(-boundY, boundY)),
            )
        }
    }
}

private const val MAX_IMAGE_ZOOM = 8f
