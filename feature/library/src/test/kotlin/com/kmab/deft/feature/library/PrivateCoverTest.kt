package com.kmab.deft.feature.library

import android.content.Context
import android.graphics.Bitmap
import androidx.test.core.app.ApplicationProvider
import coil3.ImageLoader
import coil3.request.ImageRequest
import coil3.request.SuccessResult
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class PrivateCoverTest {
    private val context = ApplicationProvider.getApplicationContext<Context>()
    @Test fun urlAndOutsideFilesNeverEnterImageLoader() {
        assertNull(privateCover("https://example.test/cover.jpg", context.filesDir))
        assertNull(privateCover(File(context.cacheDir, "outside.png").absolutePath, context.filesDir))
        assertEquals(File(context.filesDir, "cover.png"), privateCover(File(context.filesDir, "cover.png").absolutePath, context.filesDir))
    }
    @Test fun actualPrivatePngDecodesThroughNoNetworkCoil() = runBlocking {
        val file = File(context.filesDir, "cover.png")
        file.parentFile?.mkdirs()
        file.outputStream().use { Bitmap.createBitmap(20, 30, Bitmap.Config.ARGB_8888).compress(Bitmap.CompressFormat.PNG, 100, it) }
        val loader = ImageLoader.Builder(context).build()
        try {
            val result = loader.execute(ImageRequest.Builder(context).data(requireNotNull(privateCover(file.absolutePath, context.filesDir))).build())
            assertTrue(result is SuccessResult)
        } finally { loader.shutdown() }
    }
}
