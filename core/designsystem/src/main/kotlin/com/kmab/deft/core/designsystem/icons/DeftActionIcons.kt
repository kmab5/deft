package com.kmab.deft.core.designsystem.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.PathBuilder
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

/** Literal outline drawing coordinates, matching Deft's existing icon grammar. */
@Suppress("MagicNumber")
object DeftActionIcons {
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

    val More =
        icon("More") {
            moveTo(11f, 4f)
            lineTo(13f, 4f)
            lineTo(13f, 6f)
            lineTo(11f, 6f)
            close()
            moveTo(11f, 11f)
            lineTo(13f, 11f)
            lineTo(13f, 13f)
            lineTo(11f, 13f)
            close()
            moveTo(11f, 18f)
            lineTo(13f, 18f)
            lineTo(13f, 20f)
            lineTo(11f, 20f)
            close()
        }
    val Back =
        icon("Back") {
            moveTo(20f, 12f)
            lineTo(4f, 12f)
            moveTo(11f, 5f)
            lineTo(4f, 12f)
            lineTo(11f, 19f)
        }
    val Close =
        icon("Close") {
            moveTo(6f, 6f)
            lineTo(18f, 18f)
            moveTo(18f, 6f)
            lineTo(6f, 18f)
        }
    val Check =
        icon("Check") {
            moveTo(4f, 12f)
            lineTo(9f, 17f)
            lineTo(20f, 6f)
        }
    val ChevronRight =
        icon("ChevronRight") {
            moveTo(9f, 5f)
            lineTo(16f, 12f)
            lineTo(9f, 19f)
        }
    val Refresh =
        icon("Refresh") {
            moveTo(20f, 10f)
            curveTo(18f, 2f, 6f, 2f, 4f, 10f)
            moveTo(20f, 4f)
            lineTo(20f, 10f)
            lineTo(14f, 10f)
            moveTo(4f, 14f)
            curveTo(6f, 22f, 18f, 22f, 20f, 14f)
            moveTo(4f, 20f)
            lineTo(4f, 14f)
            lineTo(10f, 14f)
        }

    private fun PathBuilder.folder() {
        moveTo(3f, 5f)
        lineTo(9f, 5f)
        lineTo(11f, 8f)
        lineTo(21f, 8f)
        lineTo(21f, 20f)
        lineTo(3f, 20f)
        close()
    }

    val Folder = icon("Folder") { folder() }
    val FolderPlus =
        icon("FolderPlus") {
            folder()
            moveTo(8f, 14f)
            lineTo(16f, 14f)
            moveTo(12f, 10f)
            lineTo(12f, 18f)
        }
    val FolderMinus =
        icon("FolderMinus") {
            folder()
            moveTo(8f, 14f)
            lineTo(16f, 14f)
        }
    val Trash =
        icon("Trash") {
            moveTo(3f, 6f)
            lineTo(21f, 6f)
            moveTo(9f, 6f)
            lineTo(9f, 3f)
            lineTo(15f, 3f)
            lineTo(15f, 6f)
            moveTo(5f, 6f)
            lineTo(6f, 21f)
            lineTo(18f, 21f)
            lineTo(19f, 6f)
            moveTo(10f, 10f)
            lineTo(10f, 17f)
            moveTo(14f, 10f)
            lineTo(14f, 17f)
        }
    val Warning =
        icon("Warning") {
            moveTo(12f, 3f)
            lineTo(22f, 21f)
            lineTo(2f, 21f)
            close()
            moveTo(12f, 9f)
            lineTo(12f, 14f)
            moveTo(12f, 17f)
            lineTo(12f, 18f)
        }
    val Info =
        icon("Info") {
            moveTo(12f, 2f)
            curveTo(25f, 2f, 25f, 22f, 12f, 22f)
            curveTo(-1f, 22f, -1f, 2f, 12f, 2f)
            moveTo(12f, 11f)
            lineTo(12f, 17f)
            moveTo(12f, 7f)
            lineTo(12f, 8f)
        }
}
