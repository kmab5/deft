package com.kmab.deft.feature.reader

import android.util.Base64
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.kmab.deft.core.designsystem.icons.DeftActionIcons
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.readium.r2.shared.publication.Publication
import org.readium.r2.shared.util.Url
import java.io.ByteArrayInputStream

/** Dedicated zoom surface. It renders one bounded local image, never publication scripts or links. */
@Composable
internal fun ReaderImageViewer(
    publication: Publication,
    href: Url,
    close: () -> Unit,
) {
    val image by produceState<ImageContent?>(null, publication, href) {
        value = withContext(Dispatchers.IO) { loadImage(publication, href) }
    }
    var webView by remember { mutableStateOf<WebView?>(null) }
    DisposableEffect(Unit) { onDispose { webView?.destroy() } }
    Dialog(
        onDismissRequest = close,
        properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false),
    ) {
        Box(Modifier.fillMaxSize().background(Color.Black)) {
            val content = image
            when {
                content == null -> CircularProgressIndicator(Modifier.align(Alignment.Center))
                content.html == null ->
                    Text(
                        "This image could not be opened.",
                        Modifier.align(Alignment.Center).padding(24.dp),
                        color = Color.White,
                    )
                else ->
                    AndroidView(
                        factory = { context ->
                            WebView(context).apply {
                                webView = this
                                setBackgroundColor(android.graphics.Color.BLACK)
                                settings.apply {
                                    javaScriptEnabled = false
                                    allowFileAccess = false
                                    allowContentAccess = false
                                    blockNetworkLoads = true
                                    builtInZoomControls = true
                                    displayZoomControls = false
                                    useWideViewPort = true
                                    loadWithOverviewMode = true
                                    setSupportZoom(true)
                                }
                                webViewClient =
                                    object : WebViewClient() {
                                        override fun shouldOverrideUrlLoading(
                                            view: WebView,
                                            request: WebResourceRequest,
                                        ) = true

                                        override fun shouldInterceptRequest(
                                            view: WebView,
                                            request: WebResourceRequest,
                                        ) = WebResourceResponse(
                                            "text/plain",
                                            "UTF-8",
                                            ByteArrayInputStream(byteArrayOf()),
                                        )
                                    }
                                loadDataWithBaseURL(null, content.html, "text/html", "UTF-8", null)
                            }
                        },
                        modifier = Modifier.fillMaxSize(),
                    )
            }
            IconButton(onClick = close, Modifier.align(Alignment.TopEnd).statusBarsPadding().padding(8.dp)) {
                Icon(DeftActionIcons.Close, "Close image", tint = Color.White)
            }
        }
    }
}

private data class ImageContent(
    val html: String?,
)

// Reject missing, oversized or unsupported untrusted resources before constructing a viewer document.
@Suppress("ReturnCount")
private suspend fun loadImage(
    publication: Publication,
    href: Url,
): ImageContent {
    try {
        val resource = publication.get(href) ?: return ImageContent(null)
        val bytes =
            try {
                if ((resource.length().getOrNull() ?: 0) > MAX_IMAGE_BYTES) return ImageContent(null)
                resource.read(0L..MAX_IMAGE_BYTES).getOrNull() ?: return ImageContent(null)
            } finally {
                resource.close()
            }
        if (bytes.size > MAX_IMAGE_BYTES) return ImageContent(null)
        val type = imageMediaType(bytes) ?: return ImageContent(null)
        val encoded = Base64.encodeToString(bytes, Base64.NO_WRAP)
        return ImageContent(
            """
            <!doctype html><meta name="viewport" content="width=device-width, initial-scale=1, maximum-scale=8">
            <meta http-equiv="Content-Security-Policy" content="default-src 'none'; img-src data:; style-src 'unsafe-inline'">
            <style>
            html,body{margin:0;background:#000;width:100%;height:100%}
            body{display:flex;align-items:center;justify-content:center}
            img{max-width:100%;max-height:100%;object-fit:contain}
            </style>
            <img alt="Book image" src="data:$type;base64,$encoded">
            """.trimIndent(),
        )
    } catch (cancel: CancellationException) {
        throw cancel
    } catch (_: Exception) {
        return ImageContent(null)
    }
}

internal fun imageMediaType(bytes: ByteArray): String? {
    val header = bytes.take(HEADER_BYTES).toByteArray().toString(Charsets.ISO_8859_1)
    return when {
        header.startsWith("\u0089PNG\r\n\u001a\n") -> "image/png"
        header.startsWith("\u00ff\u00d8\u00ff") -> "image/jpeg"
        header.startsWith("GIF87a") || header.startsWith("GIF89a") -> "image/gif"
        header.startsWith("RIFF") && header.drop(WEBP_SIGNATURE_OFFSET).startsWith("WEBP") -> "image/webp"
        Regex("<svg[\\s>]", RegexOption.IGNORE_CASE).containsMatchIn(header) -> "image/svg+xml"
        else -> null
    }
}

private const val MAX_IMAGE_BYTES = 32L * 1024 * 1024
private const val HEADER_BYTES = 1024

private const val WEBP_SIGNATURE_OFFSET = 8
