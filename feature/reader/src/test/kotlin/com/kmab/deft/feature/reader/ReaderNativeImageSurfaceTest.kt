package com.kmab.deft.feature.reader

import android.graphics.BitmapFactory
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.PixelMap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipe
import com.github.takahirom.roborazzi.ExperimentalRoborazziApi
import com.github.takahirom.roborazzi.RoborazziOptions
import com.github.takahirom.roborazzi.RoborazziTaskType
import com.github.takahirom.roborazzi.captureRoboImage
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.readium.r2.shared.publication.Link
import org.readium.r2.shared.publication.Manifest
import org.readium.r2.shared.publication.Metadata
import org.readium.r2.shared.publication.Publication
import org.readium.r2.shared.util.Url
import org.readium.r2.shared.util.mediatype.MediaType
import org.readium.r2.shared.util.resource.SingleResourceContainer
import org.readium.r2.shared.util.resource.StringResource
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], qualifiers = "w393dp-h852dp-mdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class ReaderNativeImageSurfaceTest {
    @get:Rule val compose = createComposeRule()

    @Test
    fun publicationSvgDecodesAndPaintsInTheActualDialogAndCloses() {
        var closed = 0
        show(
            "<svg xmlns=\"http://www.w3.org/2000/svg\" width=\"400\" height=\"600\">" +
                "<rect width=\"400\" height=\"600\" fill=\"white\"/>" +
                "<rect x=\"150\" y=\"250\" width=\"100\" height=\"100\" fill=\"#ff00ff\"/></svg>",
        ) {
            closed++
        }
        compose.waitUntil(10_000) {
            compose.onAllNodesWithContentDescription("Book image").fetchSemanticsNodes().isNotEmpty()
        }
        val pixels = captureSurface("image-viewer-native")
        val center = pixels[pixels.width / 2, pixels.height / 2]
        assertTrue(center.red > 0.9f && center.blue > 0.9f && center.green < 0.1f)
        assertNativePinchAndPan()
        compose.onNodeWithContentDescription("Close image").assertIsDisplayed().performClick()
        assertEquals(1, closed)
    }

    @Test fun corruptImageHasVisibleRecoveryAndAnAvailableCloseAction() {
        show("<svg corrupt") {}
        compose.waitUntil(10_000) {
            compose
                .onAllNodes(
                    androidx.compose.ui.test
                        .hasText("This image could not be opened."),
                ).fetchSemanticsNodes()
                .isNotEmpty()
        }
        compose.onNodeWithText("This image could not be opened.").assertIsDisplayed()
        compose.onNodeWithContentDescription("Close image").assertIsDisplayed()
    }

    private fun assertNativePinchAndPan() {
        val surface = compose.onNodeWithTag("reader-image-surface")
        val initial = captureSurface("image-viewer-before-zoom").magentaPixels()
        surface.performTouchInput {
            down(0, center + Offset(-40f, 0f))
            down(1, center + Offset(40f, 0f))
            moveTo(0, center + Offset(-100f, 0f))
            moveTo(1, center + Offset(100f, 0f))
            up(0)
            up(1)
        }
        assertTrue(captureSurface("image-viewer-zoom").magentaPixels() > initial * 1.2)
        surface.performTouchInput { swipe(center, center + Offset(180f, 180f)) }
        val panned = captureSurface("image-viewer-pan")
        assertTrue(panned[panned.width / 2, panned.height / 2].green > 0.9f)
    }

    @OptIn(ExperimentalRoborazziApi::class)
    private fun captureSurface(name: String): PixelMap {
        val output = File("build/reports/screenshots/$name.png").apply { parentFile?.mkdirs() }
        compose.onNodeWithTag("reader-image-surface").captureRoboImage(
            output.path,
            roborazziOptions = RoborazziOptions(taskType = RoborazziTaskType.Record),
        )
        return requireNotNull(BitmapFactory.decodeFile(output.path)).asImageBitmap().toPixelMap()
    }

    private fun PixelMap.magentaPixels(): Int {
        var count = 0
        for (y in 0 until height step 4) {
            for (x in 0 until width step 4) {
                val color = this[x, y]
                if (color.red > 0.9f && color.blue > 0.9f && color.green < 0.1f) count++
            }
        }
        return count
    }

    private fun show(
        svg: String,
        close: () -> Unit,
    ) {
        val href = requireNotNull(Url("fixture.svg"))
        val publication =
            Publication(
                Manifest(metadata = Metadata(), resources = listOf(Link(href, mediaType = MediaType.SVG))),
                SingleResourceContainer(href, StringResource(svg)),
            )
        compose.setContent { ReaderImageViewer(publication, href, close) }
    }
}
