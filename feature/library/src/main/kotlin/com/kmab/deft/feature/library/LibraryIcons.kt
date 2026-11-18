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

    val Close =
        icon("close") {
            moveTo(6f, 6f)
            lineTo(18f, 18f)
            moveTo(18f, 6f)
            lineTo(6f, 18f)
        }
    val Ascending =
        icon("ascending") {
            moveTo(7f, 20f)
            lineTo(7f, 4f)
            moveTo(3f, 8f)
            lineTo(7f, 4f)
            lineTo(11f, 8f)
            moveTo(15f, 7f)
            lineTo(21f, 7f)
            moveTo(15f, 12f)
            lineTo(19f, 12f)
            moveTo(15f, 17f)
            lineTo(17f, 17f)
        }
    val Descending =
        icon("descending") {
            moveTo(7f, 4f)
            lineTo(7f, 20f)
            moveTo(3f, 16f)
            lineTo(7f, 20f)
            lineTo(11f, 16f)
            moveTo(15f, 7f)
            lineTo(17f, 7f)
            moveTo(15f, 12f)
            lineTo(19f, 12f)
            moveTo(15f, 17f)
            lineTo(21f, 17f)
        }
    val History =
        icon("history") {
            moveTo(4f, 7f)
            lineTo(4f, 3f)
            moveTo(4f, 7f)
            lineTo(8f, 7f)
            moveTo(4f, 7f)
            curveTo(8f, 0f, 21f, 3f, 21f, 12f)
            curveTo(21f, 22f, 6f, 24f, 3f, 14f)
            moveTo(12f, 7f)
            lineTo(12f, 12f)
            lineTo(16f, 14f)
        }
    val Settings =
        icon("settings") {
            moveTo(4f, 6f)
            lineTo(20f, 6f)
            moveTo(4f, 12f)
            lineTo(20f, 12f)
            moveTo(4f, 18f)
            lineTo(20f, 18f)
            moveTo(8f, 3f)
            lineTo(8f, 9f)
            moveTo(16f, 9f)
            lineTo(16f, 15f)
            moveTo(10f, 15f)
            lineTo(10f, 21f)
        }
    val Title =
        icon("title") {
            moveTo(4f, 5f)
            lineTo(20f, 5f)
            moveTo(12f, 5f)
            lineTo(12f, 20f)
            moveTo(8f, 20f)
            lineTo(16f, 20f)
        }
    val Added =
        icon("added") {
            moveTo(12f, 4f)
            lineTo(12f, 20f)
            moveTo(4f, 12f)
            lineTo(20f, 12f)
        }
    val Skipped =
        icon("skipped") {
            moveTo(5f, 5f)
            lineTo(15f, 12f)
            lineTo(5f, 19f)
            close()
            moveTo(19f, 5f)
            lineTo(19f, 19f)
        }
    val Warning =
        icon("warning") {
            moveTo(12f, 3f)
            lineTo(22f, 21f)
            lineTo(2f, 21f)
            close()
            moveTo(12f, 9f)
            lineTo(12f, 14f)
            moveTo(12f, 17f)
            lineTo(12f, 18f)
        }

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
