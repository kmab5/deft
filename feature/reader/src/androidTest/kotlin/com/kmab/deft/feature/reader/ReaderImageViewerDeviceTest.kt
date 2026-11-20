package com.kmab.deft.feature.reader

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Color
import android.os.Bundle
import android.os.SystemClock
import android.view.accessibility.AccessibilityNodeInfo
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.After
import org.junit.Assert.assertTrue
import org.junit.Test
import org.readium.r2.shared.publication.Link
import org.readium.r2.shared.publication.Manifest
import org.readium.r2.shared.publication.Metadata
import org.readium.r2.shared.publication.Publication
import org.readium.r2.shared.util.Url
import org.readium.r2.shared.util.mediatype.MediaType
import org.readium.r2.shared.util.resource.InMemoryResource
import org.readium.r2.shared.util.resource.Resource
import org.readium.r2.shared.util.resource.SingleResourceContainer
import org.readium.r2.shared.util.resource.StringResource

/** Real native Compose image rendering, without Espresso's removed Android 37 hidden input API. */
class ReaderImageViewerDeviceTest {
    private val instrumentation = InstrumentationRegistry.getInstrumentation()
    private var activity: ReaderImageDeviceActivity? = null

    @After fun closeActivity() {
        instrumentation.runOnMainSync { activity?.finish() }
    }

    @Test fun localSvgActuallyPaintsInsideTheDialog() {
        show(
            "<svg xmlns=\"http://www.w3.org/2000/svg\" width=\"400\" height=\"600\">" +
                "<rect width=\"400\" height=\"600\" fill=\"#ff00ff\"/></svg>",
        )
        assertEventually("The native Android dialog must paint the local image") {
            val screenshot = instrumentation.uiAutomation.takeScreenshot()
            try {
                screenshot != null && screenshot.containsMagenta()
            } finally {
                screenshot?.recycle()
            }
        }
    }

    @Test fun largeLocalRasterActuallyPaintsInsideTheDialog() {
        val random = java.util.Random(42)
        val pixels = IntArray(1024 * 1024) { Color.rgb(random.nextInt(256), random.nextInt(256), random.nextInt(256)) }
        val bitmap = Bitmap.createBitmap(pixels, 1024, 1024, Bitmap.Config.ARGB_8888)
        android.graphics.Canvas(bitmap).drawRect(
            256f,
            256f,
            768f,
            768f,
            android.graphics.Paint().apply {
                color =
                    Color.MAGENTA
            },
        )
        val bytes =
            java.io.ByteArrayOutputStream().use {
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, it)
                it.toByteArray()
            }
        bitmap.recycle()
        assertTrue("Fixture exercises large-image transport", bytes.size > 2_000_000)
        show(InMemoryResource(bytes), MediaType.PNG)
        assertEventually("A large raster must paint without embedding megabytes in a data URL") {
            val screenshot = instrumentation.uiAutomation.takeScreenshot()
            try {
                screenshot != null && screenshot.containsMagenta()
            } finally {
                screenshot?.recycle()
            }
        }
    }

    @Test fun undecodableImageShowsRecoveryInsteadOfAnEmptyBlackDialog() {
        show("<svg this is not a decodable image")
        assertEventually("An undecodable image must show recovery text") {
            instrumentation.uiAutomation.rootInActiveWindow?.containsText("This image could not be opened.") == true
        }
    }

    private fun show(svg: String) = show(StringResource(svg), MediaType.SVG)

    private fun show(
        resource: Resource,
        mediaType: MediaType,
    ) {
        val href = ReaderImageDeviceActivity.href
        ReaderImageDeviceActivity.publication =
            Publication(
                Manifest(metadata = Metadata(), resources = listOf(Link(href, mediaType = mediaType))),
                SingleResourceContainer(href, resource),
            )
        activity =
            instrumentation.startActivitySync(
                Intent(instrumentation.targetContext, ReaderImageDeviceActivity::class.java)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
            ) as ReaderImageDeviceActivity
    }

    private fun assertEventually(
        message: String,
        condition: () -> Boolean,
    ) {
        val deadline = SystemClock.uptimeMillis() + 15_000
        while (SystemClock.uptimeMillis() < deadline) {
            if (condition()) return
            SystemClock.sleep(100)
        }
        assertTrue(message, condition())
    }

    private fun Bitmap.containsMagenta(): Boolean {
        var matches = 0
        for (y in height / 4 until height * 3 / 4 step 10) {
            for (x in width / 4 until width * 3 / 4 step 10) {
                val pixel = getPixel(x, y)
                if (Color.red(pixel) > 220 && Color.blue(pixel) > 220 && Color.green(pixel) < 40) matches++
            }
        }
        return matches > 100
    }

    private fun AccessibilityNodeInfo.containsText(value: String): Boolean =
        text?.toString() == value || (0 until childCount).any { getChild(it)?.containsText(value) == true }
}

class ReaderImageDeviceActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { ReaderImageViewer(requireNotNull(publication), href, ::finish) }
    }

    companion object {
        val href: Url = requireNotNull(Url("image.svg"))
        var publication: Publication? = null
    }
}
