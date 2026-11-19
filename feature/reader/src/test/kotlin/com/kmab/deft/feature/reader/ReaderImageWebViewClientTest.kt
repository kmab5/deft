package com.kmab.deft.feature.reader

import android.content.Context
import android.net.Uri
import android.webkit.WebResourceRequest
import android.webkit.WebView
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class ReaderImageWebViewClientTest {
    @Test fun embeddedRasterAndSvgImagesReachTheWebViewDecoderWhileOtherRequestsAreBlocked() {
        val view = WebView(ApplicationProvider.getApplicationContext<Context>())
        val client = ReaderImageWebViewClient()
        try {
            listOf("data:image/png;base64,iVBORw0KGgo=", "data:image/svg+xml;base64,PHN2Zy8+").forEach {
                assertNull(
                    "Image decoding must not receive an empty response",
                    client.shouldInterceptRequest(view, request(it)),
                )
            }
            listOf(
                "https://example.test/image.png",
                "file:///secret.png",
                "content://private/image",
                "data:text/html,script",
            ).forEach {
                assertNotNull(
                    "Untrusted requests must never fall through",
                    client.shouldInterceptRequest(view, request(it)),
                )
            }
        } finally {
            view.destroy()
        }
    }

    private fun request(value: String) =
        object : WebResourceRequest {
            override fun getUrl(): Uri = Uri.parse(value)

            override fun isForMainFrame() = false

            override fun isRedirect() = false

            override fun hasGesture() = false

            override fun getMethod() = "GET"

            override fun getRequestHeaders(): Map<String, String> = emptyMap()
        }
}
