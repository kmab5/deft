package com.kmab.deft.feature.organization

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import com.kmab.deft.core.designsystem.privateCoverFile
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import javax.inject.Inject

internal class CategoryCoverStore
    @Inject
    constructor(
        @param:ApplicationContext private val context: Context,
    ) {
        private val folder get() = File(context.filesDir, "category-covers")

        suspend fun importImage(uri: Uri): String =
            withContext(Dispatchers.IO) {
                if (uri.scheme != "content") throw IOException("Invalid image source.")
                if (!folder.isDirectory && !folder.mkdirs()) throw IOException("Couldn't create cover storage.")
                val source = File.createTempFile("input-", ".tmp", folder)
                try {
                    copyBounded(uri, source)
                    saveThumbnail(source)
                } finally {
                    source.delete()
                }
            }

        suspend fun remove(path: String?) {
            withContext(Dispatchers.IO) {
                privateCoverFile(path, folder)?.delete()
            }
        }

        private fun copyBounded(
            uri: Uri,
            source: File,
        ) {
            val input = context.contentResolver.openInputStream(uri) ?: throw IOException("Couldn't read image.")
            input.use { bytes -> source.outputStream().use { copyImageBytes(bytes, it) } }
        }

        private fun copyImageBytes(
            input: InputStream,
            output: OutputStream,
        ) {
            val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
            var total = 0
            while (true) {
                val count = input.read(buffer)
                if (count < 0) break
                total += count
                if (total > MAX_BYTES) throw IOException("Image is too large.")
                output.write(buffer, 0, count)
            }
        }

        private fun decodeThumbnail(source: File): Bitmap {
            val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeFile(source.path, options)
            if (options.outWidth !in 1..MAX_DIMENSION || options.outHeight !in 1..MAX_DIMENSION) {
                throw IOException("Unsupported image.")
            }
            var sample = 1
            while (maxOf(options.outWidth, options.outHeight) / sample > COVER_DIMENSION) sample *= 2
            return BitmapFactory.decodeFile(source.path, BitmapFactory.Options().apply { inSampleSize = sample })
                ?: throw IOException("Unsupported image.")
        }

        private fun saveThumbnail(source: File): String {
            val bitmap = decodeThumbnail(source)
            val output = File.createTempFile("cover-", ".jpg", folder)
            try {
                output.outputStream().use {
                    if (!bitmap.compress(Bitmap.CompressFormat.JPEG, JPEG_QUALITY, it)) {
                        throw IOException("Couldn't save image.")
                    }
                }
                return output.absolutePath
            } catch (error: IOException) {
                output.delete()
                throw error
            } finally {
                bitmap.recycle()
            }
        }

        private companion object {
            const val MAX_BYTES = 16 * 1024 * 1024
            const val MAX_DIMENSION = 16_384
            const val COVER_DIMENSION = 960
            const val JPEG_QUALITY = 88
        }
    }
