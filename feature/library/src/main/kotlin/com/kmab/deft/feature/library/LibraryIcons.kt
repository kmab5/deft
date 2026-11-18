package com.kmab.deft.feature.library

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.PathBuilder
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

/** Literal 24dp outline paths using the shared square-capped icon grammar. */
@Suppress("MagicNumber")
internal object LibraryIcons {
    private fun icon(
        name: String,
        draw: PathBuilder.() -> Unit,
    ) = ImageVector
        .Builder(name, 24.dp, 24.dp, 24f, 24f)
        .apply {
            path(
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 1.75f,
                strokeLineCap = StrokeCap.Square,
                strokeLineJoin = StrokeJoin.Miter,
                pathBuilder = draw,
            )
        }.build()

    val Missing =
        icon("missing file") {
            moveTo(6f, 3f)
            lineTo(15f, 3f)
            lineTo(19f, 7f)
            lineTo(19f, 21f)
            lineTo(6f, 21f)
            close()
            moveTo(10f, 11f)
            lineTo(15f, 16f)
            moveTo(15f, 11f)
            lineTo(10f, 16f)
        }

    val More =
        icon("more") {
            moveTo(12f, 5f)
            lineTo(12f, 5.1f)
            moveTo(12f, 12f)
            lineTo(12f, 12.1f)
            moveTo(12f, 19f)
            lineTo(12f, 19.1f)
        }
    val List =
        icon("list") {
            moveTo(4f, 5f)
            lineTo(20f, 5f)
            moveTo(4f, 12f)
            lineTo(20f, 12f)
            moveTo(4f, 19f)
            lineTo(20f, 19f)
        }
    val Grid =
        icon("grid") {
            moveTo(3f, 3f)
            lineTo(10f, 3f)
            lineTo(10f, 10f)
            lineTo(3f, 10f)
            close()
            moveTo(14f, 3f)
            lineTo(21f, 3f)
            lineTo(21f, 10f)
            lineTo(14f, 10f)
            close()
            moveTo(3f, 14f)
            lineTo(10f, 14f)
            lineTo(10f, 21f)
            lineTo(3f, 21f)
            close()
            moveTo(14f, 14f)
            lineTo(21f, 14f)
            lineTo(21f, 21f)
            lineTo(14f, 21f)
            close()
        }
}
