package com.kmab.deft.feature.reader

import android.graphics.Bitmap
import android.graphics.Color
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.unit.IntSize
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.ByteArrayOutputStream

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class ReaderNativeImageTest {
    @Test fun rasterDecodesActualPixelsAndLargeDimensionsAreSampled() {
        val original = Bitmap.createBitmap(6000, 1000, Bitmap.Config.ARGB_8888)
        original.eraseColor(Color.MAGENTA)
        val bytes =
            ByteArrayOutputStream().use {
                original.compress(Bitmap.CompressFormat.PNG, 100, it)
                it.toByteArray()
            }
        original.recycle()
        val decoded = requireNotNull(decodeReaderImage(bytes))
        assertTrue(decoded.width <= 4096)
        assertTrue(decoded.width.toLong() * decoded.height <= 4_194_304)
        assertEquals(Color.MAGENTA, decoded.getPixel(decoded.width / 2, decoded.height / 2))
    }

    @Test fun svgRendersNativePixelsWithItsAspectRatio() {
        val decoded = requireNotNull(decodeReaderImage(svg("<rect width=\"400\" height=\"600\" fill=\"#ff00ff\"/>")))
        assertEquals(2f / 3f, decoded.width.toFloat() / decoded.height, 0.01f)
        assertEquals(Color.MAGENTA, decoded.getPixel(decoded.width / 2, decoded.height / 2))
    }

    @Test fun malformedAndOversizedImagesFailVisiblyInsteadOfProducingAnEmptySurface() {
        assertNull(decodeReaderImage("<svg broken".toByteArray()))
        assertNull(decodeReaderImage(byteArrayOf(1, 2, 3)))
        assertNull(decodeReaderImage(ByteArray(32 * 1024 * 1024 + 1)))
    }

    @Test fun svgNeverLoadsExternalResourcesOrExpandsEntities() {
        listOf(
            "<image href=\"https://example.test/a.png\"/>",
            "<image href=\"file:///private/a.png\"/>",
            "<image href=\"content://private/a.png\"/>",
            "<image href=\"data:image/png;base64,AAAA\"/>",
            "<script>alert(1)</script>",
            "<foreignObject><p>text</p></foreignObject>",
            "<style>@import 'https://example.test/a.css';</style>",
        ).forEach { assertNull(it, decodeReaderImage(svg(it))) }
        assertNull(
            decodeReaderImage(
                (
                    "<!DOCTYPE svg [<!ENTITY e SYSTEM 'file:///private'>]>" +
                        svg("").toString(Charsets.UTF_8)
                ).toByteArray(),
            ),
        )
    }

    @Test fun internalSvgPaintReferencesStillRender() {
        val image =
            decodeReaderImage(
                svg(
                    "<defs><linearGradient id=\"g\"><stop stop-color=\"#ff00ff\"/></linearGradient></defs>" +
                        "<rect width=\"400\" height=\"600\" fill=\"url(#g)\"/>",
                ),
            )
        assertNotNull(image)
    }

    @Test fun cyclicAndDeepSvgReferencesFailBeforeNativeRendering() {
        val self = "<use id=\"loop\" xlink:href=\"#loop\"/>"
        val pair = "<use id=\"a\" xlink:href=\"#b\"/><use id=\"b\" xlink:href=\"#a\"/>"
        val nested = "<g>".repeat(80) + "<rect width=\"10\" height=\"10\"/>" + "</g>".repeat(80)
        val chain =
            (0..80).joinToString("") { "<use id=\"n$it\" xlink:href=\"#n${it + 1}\"/>" } +
                "<g id=\"n81\"><rect width=\"10\" height=\"10\"/></g>"
        listOf(self, pair, nested, chain).forEach { assertNull(decodeReaderImage(svg(it))) }
    }

    @Test fun acyclicSvgReuseHasABoundedExpandedGraph() {
        val body =
            buildString {
                append("<g id=\"n0\"><rect width=\"10\" height=\"10\"/></g>")
                for (index in 1..12) {
                    append("<g id=\"n$index\">")
                    repeat(3) { append("<use xlink:href=\"#n${index - 1}\"/>") }
                    append("</g>")
                }
                append("<use xlink:href=\"#n12\"/>")
            }
        assertNull(decodeReaderImage(svg(body)))
    }

    @Test fun ordinaryInternalSvgReuseStillPaints() {
        val image =
            requireNotNull(
                decodeReaderImage(
                    svg(
                        "<defs><g id=\"box\"><rect width=\"400\" height=\"600\" fill=\"#ff00ff\"/></g></defs>" +
                            "<use xlink:href=\"#box\"/>",
                    ),
                ),
            )
        assertEquals(Color.MAGENTA, image.getPixel(image.width / 2, image.height / 2))
    }

    @Test fun zoomUsesTheFocalPointAndPanCannotLoseTheImage() {
        val viewport = IntSize(400, 600)
        val image = IntSize(400, 600)
        val zoomed = ReaderImageTransform().change(Offset(300f, 300f), Offset.Zero, 2f, viewport, image)
        assertEquals(2f, zoomed.scale)
        assertEquals(-100f, zoomed.offset.x)
        val panned = zoomed.change(Offset.Zero, Offset(10000f, -10000f), 1f, viewport, image)
        assertEquals(200f, panned.offset.x)
        assertEquals(-300f, panned.offset.y)
        val reset = panned.change(Offset.Zero, Offset.Zero, 0.1f, viewport, image)
        assertEquals(ReaderImageTransform(), reset)
    }

    private fun svg(body: String) =
        (
            "<svg xmlns=\"http://www.w3.org/2000/svg\" xmlns:xlink=\"http://www.w3.org/1999/xlink\" " +
                "width=\"400\" height=\"600\">$body</svg>"
        ).toByteArray()
}
