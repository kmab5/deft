package com.kmab.deft.format.readium

import android.content.res.AssetManager
import android.webkit.MimeTypeMap
import android.webkit.WebResourceResponse
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import org.readium.r2.shared.publication.Publication
import org.readium.r2.shared.util.Url
import org.readium.r2.shared.util.use
import java.net.URI

/** Every request gets a local response, including failures. WebView never falls through to a network loader. */
class ContinuousReaderResources(
    private val publication: Publication,
    private val assets: AssetManager,
    private val onMissingChapter: (String) -> Unit,
) {
    // Each origin/path boundary exits immediately with a non-null denial; no network fallback.
    @Suppress("ReturnCount")
    fun response(value: String): WebResourceResponse {
        if (value == HOST_URL) {
            val chapters = publication.readingOrder.map { it.href.toString() to it.title.orEmpty() }
            return response("text/html", ContinuousReaderDocument.html(chapters).toByteArray())
        }
        val uri = runCatching { URI(value) }.getOrNull() ?: return denied()
        if (uri.scheme != "https") return denied()
        if (uri.rawAuthority == "readium_assets") {
            val path = uri.rawPath.orEmpty().removePrefix("/")
            val permitted = path in TRUSTED_READER_SCRIPTS || path.startsWith("reader-fonts/")
            if (!permitted || !PublicationPolicy.isLocalResource(path)) return denied()
            return runCatching { response(mime(path), assets.open(path).use { it.readBytes() }) }.getOrElse { denied() }
        }
        val href = localPublicationUrl(value) ?: return denied()
        val link = publication.linkWithHref(href)
        val bytes =
            runBlocking(Dispatchers.IO) {
                runCatching { publication.get(href)?.use { it.read().getOrNull() } }.getOrNull()
            }
        if (bytes !=
            null
        ) {
            return response(link?.mediaType?.toString()?.substringBefore(';') ?: mime(href.toString()), bytes)
        }
        if (publication.readingOrder.any { it.href.toString().substringBefore('#') == href.toString() }) {
            onMissingChapter(href.toString())
            return response(
                "text/html",
                (
                    "<html><body><h2>This chapter is unavailable</h2>" +
                        "<p>Continue scrolling, or choose another chapter from Contents.</p></body></html>"
                ).toByteArray(),
            )
        }
        return denied()
    }

    private fun response(
        mime: String,
        bytes: ByteArray,
    ) = WebResourceResponse(mime, "UTF-8", bytes.inputStream())

    private fun denied() =
        WebResourceResponse("text/plain", "UTF-8", HTTP_FORBIDDEN, "Blocked", emptyMap(), byteArrayOf().inputStream())

    private fun mime(path: String): String =
        MimeTypeMap.getSingleton().getMimeTypeFromExtension(path.substringAfterLast('.').lowercase())
            ?: "application/octet-stream"

    companion object {
        const val HOST_URL = "https://readium_package/__deft_continuous/index.html"
    }
}

/** Resolves only Readium's publication origin; early guards reject remote or app-private paths. */
@Suppress("ReturnCount")
fun localPublicationUrl(value: String): Url? {
    val uri = runCatching { URI(value) }.getOrNull() ?: return null
    val relative =
        if (uri.isAbsolute) {
            if (uri.scheme != "https" || uri.rawAuthority != "readium_package") return null
            uri.rawPath.orEmpty().removePrefix("/")
        } else {
            value.substringBefore('#').substringBefore('?')
        }
    return relative.takeIf(PublicationPolicy::isLocalResource)?.let { Url(it) }
}

private const val HTTP_FORBIDDEN = 403
private val TRUSTED_READER_SCRIPTS = setOf("deft-reader/continuous.js", "deft-reader/annotations.js")
