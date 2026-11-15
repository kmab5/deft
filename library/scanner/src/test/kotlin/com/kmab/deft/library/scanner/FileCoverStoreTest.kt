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
