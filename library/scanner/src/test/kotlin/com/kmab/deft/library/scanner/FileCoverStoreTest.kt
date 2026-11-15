package com.kmab.deft.library.scanner

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import com.kmab.deft.core.model.ContentHash
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.ByteArrayOutputStream
import java.io.File

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class FileCoverStoreTest {
    @get:Rule val temporary = TemporaryFolder()
    private val hash = ContentHash("a".repeat(64))

    @Test fun `writes decodable resized jpeg inside private directory`() =
        runBlocking {
            val root = temporary.newFolder()
            val bitmap = Bitmap.createBitmap(1200, 800, Bitmap.Config.ARGB_8888)
            bitmap.eraseColor(android.graphics.Color.RED)
            val bytes =
                ByteArrayOutputStream()
                    .also {
                        bitmap.compress(
                            Bitmap.CompressFormat.PNG,
                            100,
                            it,
                        )
                    }.toByteArray()
            val path = FileCoverStore(root).save(hash, bytes)!!
            assertEquals(File(root, "covers/${hash.hex}.jpg").absolutePath, path)
            val output = File(path).readBytes()
            assertEquals(0xff, output[0].toInt() and 255)
            assertEquals(0xd8, output[1].toInt() and 255)
            val decoded = BitmapFactory.decodeFile(path)
            assertEquals(600, decoded.width)
            assertEquals(400, decoded.height)
        }

    @Test fun `large compressed dimensions are sampled before decoding and invalid bounds rejected`() =
        runBlocking {
            val store = FileCoverStore(temporary.newFolder())
            val path = store.save(hash, solidPng(16384, 16384))!!
            val result = BitmapFactory.decodeFile(path)
            assertEquals(600, result.width)
            assertEquals(600, result.height)
            result.recycle()
            assertNull(store.save(hash, solidPng(0, 1)))
        }

    private fun solidPng(
        width: Int,
        height: Int,
    ): ByteArray {
        val output = ByteArrayOutputStream()
        val data = java.io.DataOutputStream(output)
        data.write(byteArrayOf(137.toByte(), 80, 78, 71, 13, 10, 26, 10))

        fun chunk(
            type: String,
            payload: ByteArray,
        ) {
            val name = type.toByteArray(Charsets.US_ASCII)
            data.writeInt(payload.size)
            data.write(name)
            data.write(payload)
            val crc = java.util.zip.CRC32()
            crc.update(name)
            crc.update(payload)
            data.writeInt(crc.value.toInt())
        }
        val header = ByteArrayOutputStream()
        java.io.DataOutputStream(header).apply {
            writeInt(width)
            writeInt(height)
            write(byteArrayOf(8, 0, 0, 0, 0))
        }
        chunk("IHDR", header.toByteArray())
        val compressed = ByteArrayOutputStream()
        java.util.zip.DeflaterOutputStream(compressed).use { stream ->
            val row = ByteArray(width + 1)
            repeat(height) { stream.write(row) }
        }
        chunk("IDAT", compressed.toByteArray())
        chunk("IEND", byteArrayOf())
        return output.toByteArray()
    }

    @Test fun `small images are not enlarged and invalid data creates no cover`() =
        runBlocking {
            val root = temporary.newFolder()
            val store = FileCoverStore(root)
            assertNull(store.save(hash, byteArrayOf(1, 2, 3)))
            assertFalse(File(root, "covers/${hash.hex}.jpg").exists())
            val bytes =
                ByteArrayOutputStream()
                    .also {
                        Bitmap
                            .createBitmap(
                                20,
                                30,
                                Bitmap.Config.ARGB_8888,
                            ).compress(Bitmap.CompressFormat.PNG, 100, it)
                    }.toByteArray()
            val decoded = BitmapFactory.decodeFile(store.save(hash, bytes))
            assertEquals(20, decoded.width)
            assertEquals(30, decoded.height)
        }
}
