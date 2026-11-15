package com.kmab.deft.core.designsystem.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.PathBuilder
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

/**
 * Lucide-derived outline grammar: 24dp, square caps and joins, 1.75dp stroke. Paths are literal drawing
 * coordinates.
 */
@Suppress("MagicNumber")
object DeftIcons {
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

    val Books =
        icon("Books") {
            moveTo(4f, 3f)
            lineTo(4f, 21f)
            lineTo(20f, 21f)
            lineTo(20f, 3f)
            close()
            moveTo(8f, 3f)
            lineTo(8f, 21f)
        }
    val Categories =
        icon("Categories") {
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
    val Authors =
        icon("Authors") {
            moveTo(16f, 21f)
            lineTo(16f, 18f)
            curveTo(16f, 14f, 4f, 14f, 4f, 18f)
            lineTo(4f, 21f)
            moveTo(10f, 3f)
            curveTo(4f, 3f, 4f, 11f, 10f, 11f)
            curveTo(16f, 11f, 16f, 3f, 10f, 3f)
            moveTo(20f, 21f)
            lineTo(20f, 17f)
            lineTo(18f, 15f)
        }
    val Notes =
        icon("Notes") {
            moveTo(5f, 3f)
            lineTo(19f, 3f)
            lineTo(19f, 21f)
            lineTo(5f, 21f)
            close()
            moveTo(9f, 7f)
            lineTo(15f, 7f)
            moveTo(9f, 12f)
            lineTo(15f, 12f)
            moveTo(9f, 17f)
            lineTo(13f, 17f)
        }
    val Stats =
        icon("Stats") {
            moveTo(4f, 20f)
            lineTo(4f, 14f)
            moveTo(12f, 20f)
            lineTo(12f, 4f)
            moveTo(20f, 20f)
            lineTo(20f, 9f)
        }
    val Search =
        icon("Search") {
            moveTo(16f, 16f)
            lineTo(21f, 21f)
            moveTo(11f, 4f)
            curveTo(1f, 4f, 1f, 18f, 11f, 18f)
            curveTo(21f, 18f, 21f, 4f, 11f, 4f)
        }
    val Filter =
        icon("Filter") {
            moveTo(3f, 5f)
            lineTo(21f, 5f)
            moveTo(6f, 12f)
            lineTo(18f, 12f)
            moveTo(10f, 19f)
            lineTo(14f, 19f)
        }
    val Lock =
        icon("Lock") {
            moveTo(5f, 10f)
            lineTo(19f, 10f)
            lineTo(19f, 21f)
            lineTo(5f, 21f)
            close()
            moveTo(8f, 10f)
            lineTo(8f, 6f)
            curveTo(8f, 1f, 16f, 1f, 16f, 6f)
            lineTo(16f, 10f)
        }
    val Heart =
        icon("Heart") {
            moveTo(12f, 21f)
            lineTo(3f, 12f)
            curveTo(-2f, 5f, 7f, -1f, 12f, 6f)
            curveTo(17f, -1f, 26f, 5f, 21f, 12f)
            close()
        }
    val Star =
        icon("Star") {
            moveTo(12f, 2f)
            lineTo(15f, 8f)
            lineTo(22f, 9f)
            lineTo(17f, 14f)
            lineTo(18f, 21f)
            lineTo(12f, 18f)
            lineTo(6f, 21f)
            lineTo(7f, 14f)
            lineTo(2f, 9f)
            lineTo(9f, 8f)
            close()
        }
}
