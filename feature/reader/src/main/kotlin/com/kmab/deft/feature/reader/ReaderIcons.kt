package com.kmab.deft.feature.reader

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.PathBuilder
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

@Suppress("MagicNumber")
internal object ReaderIcons {
    private fun icon(
        name: String,
        draw: PathBuilder.() -> Unit,
    ) = ImageVector
        .Builder(name, 24.dp, 24.dp, 24f, 24f)
        .apply {
            path(
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 1.75f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round,
                pathBuilder = draw,
            )
        }.build()

    val Search =
        icon("Search") {
            moveTo(15.5f, 15.5f)
            lineTo(21f, 21f)
            moveTo(17f, 10f)
            curveTo(17f, 13.87f, 13.87f, 17f, 10f, 17f)
            curveTo(6.13f, 17f, 3f, 13.87f, 3f, 10f)
            curveTo(3f, 6.13f, 6.13f, 3f, 10f, 3f)
            curveTo(13.87f, 3f, 17f, 6.13f, 17f, 10f)
        }
    val Contents =
        icon("Contents") {
            moveTo(4f, 5f)
            lineTo(20f, 5f)
            moveTo(4f, 12f)
            lineTo(20f, 12f)
            moveTo(4f, 19f)
            lineTo(15f, 19f)
        }
    val Bookmark =
        icon("Bookmark") {
            moveTo(6f, 3f)
            lineTo(18f, 3f)
            lineTo(18f, 21f)
            lineTo(12f, 17f)
            lineTo(6f, 21f)
            close()
        }
    val Settings =
        icon("Reading settings") {
            moveTo(3f, 6f)
            lineTo(21f, 6f)
            moveTo(3f, 12f)
            lineTo(21f, 12f)
            moveTo(3f, 18f)
            lineTo(21f, 18f)
            moveTo(8f, 3f)
            lineTo(8f, 9f)
            moveTo(16f, 9f)
            lineTo(16f, 15f)
            moveTo(10f, 15f)
            lineTo(10f, 21f)
        }
    val Rename =
        icon("Rename") {
            moveTo(4f, 16f)
            lineTo(16f, 4f)
            lineTo(20f, 8f)
            lineTo(8f, 20f)
            lineTo(4f, 20f)
            close()
            moveTo(13f, 7f)
            lineTo(17f, 11f)
        }
    val Return =
        icon("Return") {
            moveTo(10f, 5f)
            lineTo(4f, 11f)
            lineTo(10f, 17f)
            moveTo(4f, 11f)
            lineTo(15f, 11f)
            curveTo(22f, 11f, 22f, 21f, 15f, 21f)
        }
}
