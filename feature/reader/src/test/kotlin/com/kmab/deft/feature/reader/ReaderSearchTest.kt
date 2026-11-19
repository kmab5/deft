@file:OptIn(org.readium.r2.shared.ExperimentalReadiumApi::class)

package com.kmab.deft.feature.reader

import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.readium.r2.shared.publication.Link
import org.readium.r2.shared.publication.Manifest
import org.readium.r2.shared.publication.Metadata
import org.readium.r2.shared.publication.Publication
import org.readium.r2.shared.publication.services.search.StringSearchService
import org.readium.r2.shared.util.Url
import org.readium.r2.shared.util.mediatype.MediaType
import org.readium.r2.shared.util.resource.SingleResourceContainer
import org.readium.r2.shared.util.resource.StringResource
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class ReaderSearchTest {
    @Test fun searchesActualPublicationTextAndReturnsNavigableSnippets() =
        runBlocking {
            val publication = publication()
            try {
                val search = ReaderSearch(this)
                search.query("moon", publication)
                val result = withTimeout(10_000) { search.state.first { !it.searching } }
                assertEquals(2, result.results.size)
                assertTrue(result.results.all { it.href.toString() == "chapter.xhtml" })
                assertTrue(result.results.all { it.text.highlight?.lowercase() == "moon" })
                assertTrue(
                    result.results.all {
                        it.text.before
                            .orEmpty()
                            .isNotEmpty() ||
                            it.text.after
                                .orEmpty()
                                .isNotEmpty()
                    },
                )
                search.query("missing", publication)
                assertTrue(withTimeout(10_000) { search.state.first { !it.searching } }.results.isEmpty())
            } finally {
                publication.close()
            }
        }

    @Test fun blankQueryCancelsPendingSearchAndClearsResults() =
        runBlocking {
            val publication = publication()
            try {
                val search = ReaderSearch(this)
                search.query("moon", publication)
                search.query("  ", publication)
                assertFalse(search.state.value.searching)
                assertTrue(
                    search.state.value.results
                        .isEmpty(),
                )
                search.query("moon", publication)
                search.cancel()
                assertFalse(search.state.value.searching)
                assertTrue(
                    search.state.value.results
                        .isEmpty(),
                )
            } finally {
                publication.close()
            }
        }

    private fun publication(): Publication {
        val href = requireNotNull(Url("chapter.xhtml"))
        val link = Link(href, mediaType = MediaType.XHTML, title = "Night")
        return Publication(
            Manifest(metadata = Metadata(), readingOrder = listOf(link), tableOfContents = listOf(link)),
            SingleResourceContainer(
                href,
                StringResource("<html><body><p>The moon rose. A bright moon.</p></body></html>"),
            ),
            Publication.ServicesBuilder(search = StringSearchService.createDefaultFactory()),
        )
    }
}
