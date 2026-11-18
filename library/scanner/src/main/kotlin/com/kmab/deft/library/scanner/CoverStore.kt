package com.kmab.deft.library.scanner

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import com.kmab.deft.core.model.ContentHash
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import java.io.File
import java.io.IOException
import kotlin.math.roundToInt

interface CoverStore {
    suspend fun save(
        hash: ContentHash,
        image: ByteArray,
    ): String?
}

/** [dir] is the app-private files directory; covers are replaceable derived data. */
class FileCoverStore(
    private val dir: File,
) : CoverStore {
    override suspend fun save(
        hash: ContentHash,
        image: ByteArray,
    ): String? =
        withContext(Dispatchers.IO) {
            ensureActive()
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeByteArray(image, 0, image.size, bounds)
            if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return@withContext null
            var sample = 1
            val longest = maxOf(bounds.outWidth, bounds.outHeight)
            while (longest.toLong() > MAX_DECODE_EDGE.toLong() * sample) sample *= 2
            ensureActive()
            val options = BitmapFactory.Options().apply { inSampleSize = sample }
            val decoded = BitmapFactory.decodeByteArray(image, 0, image.size, options) ?: return@withContext null
            var scaled = decoded
            try {
                val longEdge = maxOf(decoded.width, decoded.height)
                if (longEdge > MAX_EDGE) {
                    val ratio = MAX_EDGE.toDouble() / longEdge
                    scaled =
                        Bitmap.createScaledBitmap(
                            decoded,
                            (decoded.width * ratio).roundToInt().coerceAtLeast(1),
                            (decoded.height * ratio).roundToInt().coerceAtLeast(1),
                            true,
                        )
                }
                ensureActive()
                val covers = File(dir, "covers")
                if (!covers.isDirectory && !covers.mkdirs()) throw IOException("Cannot create cover directory")
                val target = File(covers, "${hash.hex}.jpg")
                target.outputStream().use {
                    if (!scaled.compress(
                            Bitmap.CompressFormat.JPEG,
                            JPEG_QUALITY,
                            it,
                        )
                    ) {
                        throw IOException("Cannot encode cover")
                    }
                }
                target.absolutePath
            } finally {
                if (scaled !== decoded) scaled.recycle()
                decoded.recycle()
            }
        }

    private companion object {
        const val MAX_DECODE_EDGE = 1200
        const val MAX_EDGE = 600
        const val JPEG_QUALITY = 85
    }
}
