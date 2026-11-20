package com.kmab.deft.feature.reader

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.kmab.deft.core.designsystem.icons.DeftActionIcons
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.readium.r2.shared.publication.Publication
import org.readium.r2.shared.util.Url

/** One bounded native image surface. Publication markup and scripts never enter a WebView. */
@Composable
internal fun ReaderImageViewer(
    publication: Publication,
    href: Url,
    close: () -> Unit,
) {
    val result by produceState<ReaderImageResult?>(null, publication, href) {
        value = null
        value = ReaderImageResult(withContext(Dispatchers.IO) { loadReaderImage(publication, href) })
    }
    Dialog(
        onDismissRequest = close,
        properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false),
    ) {
        Box(Modifier.fillMaxSize().background(Color.Black)) {
            val loaded = result
            val bitmap = loaded?.bitmap
            when {
                loaded == null -> CircularProgressIndicator(Modifier.align(Alignment.Center))
                bitmap == null ->
                    Text(
                        "This image could not be opened.",
                        Modifier.align(Alignment.Center).padding(24.dp),
                        color = Color.White,
                    )
                else -> ReaderImageSurface(bitmap)
            }
            IconButton(
                onClick = close,
                Modifier
                    .align(Alignment.TopEnd)
                    .statusBarsPadding()
                    .padding(8.dp)
                    .background(Color.Black.copy(alpha = 0.7f), CircleShape),
            ) {
                Icon(DeftActionIcons.Close, "Close image", tint = Color.White)
            }
        }
    }
}

@Composable
internal fun ReaderImageSurface(bitmap: Bitmap) {
    var transform by remember(bitmap) { mutableStateOf(ReaderImageTransform()) }
    var viewport by remember { mutableStateOf(IntSize.Zero) }
    val imageSize = remember(bitmap) { IntSize(bitmap.width, bitmap.height) }
    BoxWithConstraints(
        Modifier
            .fillMaxSize()
            .clipToBounds()
            .testTag("reader-image-surface")
            .onSizeChanged {
                viewport = it
                transform = transform.change(Offset.Zero, Offset.Zero, 1f, it, imageSize)
            }.pointerInput(bitmap, viewport) {
                detectTransformGestures { centroid, pan, zoom, _ ->
                    transform = transform.change(centroid, pan, zoom, viewport, imageSize)
                }
            },
        contentAlignment = Alignment.Center,
    ) {
        val ratio = bitmap.width.toFloat() / bitmap.height
        val width = minOf(maxWidth, maxHeight * ratio)
        val height = width / ratio
        Image(
            bitmap.asImageBitmap(),
            contentDescription = "Book image",
            modifier =
                Modifier
                    .size(width, height)
                    .graphicsLayer {
                        scaleX = transform.scale
                        scaleY = transform.scale
                        translationX = transform.offset.x
                        translationY = transform.offset.y
                    }.background(Color.White),
        )
    }
}

private data class ReaderImageResult(
    val bitmap: Bitmap?,
)
