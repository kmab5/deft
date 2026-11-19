package com.kmab.deft.format.readium

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.readium.r2.shared.publication.Link
import org.readium.r2.shared.publication.Manifest
import org.readium.r2.shared.publication.Metadata
import org.readium.r2.shared.publication.Publication
import org.readium.r2.shared.util.Url
import org.readium.r2.shared.util.mediatype.MediaType
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class ContinuousReaderResourcesTest {
    @Test fun remoteRequestsNeverFallThroughEvenWhenTheirPathMatchesATrustedAsset() {
        val resources = resources {}
        listOf(
            "https://evil.test/deft-reader/continuous.js",
            "https://evil.test/readium/scripts/readium-reflowable.js",
            "file:///private/secret",
            "content://private/secret",
            "https://readium_assets/../secret",
            "https://readium_assets/reader-fonts/%2e%2e/secret",
        ).forEach { assertEquals(it, 403, resources.response(it).statusCode) }
    }

    @Test fun aMissingChapterHasInlineRecoveryAndReportsTheOriginalResource() {
        val failures = mutableListOf<String>()
        val response = resources(failures::add).response("https://readium_package/EPUB/missing.xhtml")
        val html = response.data.bufferedReader().readText()
        assertTrue(html.contains("This chapter is unavailable"))
        assertTrue(html.contains("Continue scrolling"))
        assertTrue(html.contains("Contents"))
        assertEquals(listOf("EPUB/missing.xhtml"), failures)
    }

    @Test fun urlResolutionRejectsRemoteHostsCredentialsPortsAndTraversal() {
        listOf(
            "https://readium_package.evil.test/image.png",
            "https://reader@readium_package/image.png",
            "https://readium_package:8443/image.png",
            "https://readium_package/%2e%2e/private",
            "//evil.test/image.png",
        ).forEach { assertNull(it, localPublicationUrl(it)) }
        assertEquals("EPUB/image.png", localPublicationUrl("https://readium_package/EPUB/image.png#part").toString())
    }

    private fun resources(onFailure: (String) -> Unit): ContinuousReaderResources =
        ContinuousReaderResources(
            Publication(
                Manifest(
                    metadata = Metadata(),
                    readingOrder = listOf(Link(requireNotNull(Url("EPUB/missing.xhtml")), MediaType.XHTML)),
                ),
            ),
            ApplicationProvider.getApplicationContext<Context>().assets,
            onFailure,
        )
}
