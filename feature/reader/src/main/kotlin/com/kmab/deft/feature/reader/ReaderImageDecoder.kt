package com.kmab.deft.feature.reader

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import com.caverock.androidsvg.SVG
import kotlinx.coroutines.CancellationException
import org.readium.r2.shared.publication.Publication
import org.readium.r2.shared.util.Url
import org.readium.r2.shared.util.resource.Resource
import kotlin.math.max
import kotlin.math.sqrt

/** Reads a bounded publication resource only; never opens a file or URL supplied by image content. */
internal suspend fun loadReaderImage(
    publication: Publication,
    href: Url,
): Bitmap? =
    try {
        publication.get(href)?.let { readImageBytes(it) }?.let(::decodeReaderImage)
    } catch (cancel: CancellationException) {
        throw cancel
    } catch (_: Exception) {
        null
    } catch (_: OutOfMemoryError) {
        null
    }

private suspend fun readImageBytes(resource: Resource): ByteArray? =
    try {
        if ((resource.length().getOrNull() ?: 0) > MAX_IMAGE_BYTES) {
            null
        } else {
            resource.read(0L..MAX_IMAGE_BYTES).getOrNull()
        }
    } finally {
        resource.close()
    }

internal fun decodeReaderImage(bytes: ByteArray): Bitmap? {
    if (bytes.size > MAX_IMAGE_BYTES) return null
    return try {
        when (imageMediaType(bytes)) {
            null -> null
            "image/svg+xml" -> decodeSvg(bytes)
            else -> decodeRaster(bytes)
        }
    } catch (_: Exception) {
        null
    } catch (_: OutOfMemoryError) {
        null
    }
}

private fun decodeRaster(bytes: ByteArray): Bitmap? {
    val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    BitmapFactory.decodeByteArray(bytes, 0, bytes.size, bounds)
    if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null
    var sample = 1
    while (!fitsBitmap(bounds.outWidth / sample, bounds.outHeight / sample)) sample *= 2
    val options =
        BitmapFactory.Options().apply {
            inSampleSize = sample
            inScaled = false
            inPreferredConfig = Bitmap.Config.ARGB_8888
        }
    return BitmapFactory.decodeByteArray(bytes, 0, bytes.size, options)
}

private fun decodeSvg(bytes: ByteArray): Bitmap? = safeReaderSvg(bytes)?.let { renderSvg(SVG.getFromString(it)) }

private fun renderSvg(svg: SVG): Bitmap? {
    // AndroidSVG has no external resolver registered: images/fonts/CSS cannot trigger I/O.
    val width = svg.documentWidth.takeIf { it > 0f } ?: svg.documentViewBox?.width() ?: SVG_FALLBACK_SIDE
    val height = svg.documentHeight.takeIf { it > 0f } ?: svg.documentViewBox?.height() ?: SVG_FALLBACK_SIDE
    val positiveDimensions = width > 0f && height > 0f
    if (!positiveDimensions || !width.isFinite() || !height.isFinite()) return null
    val factor =
        max(max(width, height) / MAX_BITMAP_SIDE, sqrt(width.toDouble() * height / MAX_BITMAP_PIXELS).toFloat())
            .coerceAtLeast(1f)
    val targetWidth = (width / factor).toInt().coerceAtLeast(1)
    val targetHeight = (height / factor).toInt().coerceAtLeast(1)
    if (svg.documentViewBox == null) svg.setDocumentViewBox(0f, 0f, width, height)
    svg.documentWidth = targetWidth.toFloat()
    svg.documentHeight = targetHeight.toFloat()
    return Bitmap.createBitmap(targetWidth, targetHeight, Bitmap.Config.ARGB_8888).also {
        svg.renderToCanvas(Canvas(it))
    }
}

private fun fitsBitmap(
    width: Int,
    height: Int,
): Boolean = width <= MAX_BITMAP_SIDE && height <= MAX_BITMAP_SIDE && width.toLong() * height <= MAX_BITMAP_PIXELS

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
private const val MAX_BITMAP_PIXELS = 4_194_304L
private const val MAX_BITMAP_SIDE = 4096
private const val SVG_FALLBACK_SIDE = 1024f
private const val HEADER_BYTES = 1024
private const val WEBP_SIGNATURE_OFFSET = 8
