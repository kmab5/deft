package com.kmab.deft.feature.reader

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.kmab.deft.core.designsystem.DeftTheme
import com.kmab.deft.core.model.SearchPattern
import com.kmab.deft.format.readium.searchText
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.readium.r2.shared.publication.Link
import org.readium.r2.shared.publication.Locator
import org.readium.r2.shared.publication.Manifest
import org.readium.r2.shared.publication.Metadata
import org.readium.r2.shared.publication.Publication
import org.readium.r2.shared.publication.services.PositionsService
import org.readium.r2.shared.publication.services.positionsServiceFactory
import org.readium.r2.shared.util.Url
import org.readium.r2.shared.util.mediatype.MediaType
import org.readium.r2.shared.util.resource.SingleResourceContainer
import org.readium.r2.shared.util.resource.StringResource
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class ReaderSearchLocationTest {
    @get:Rule val compose = createComposeRule()

    @Test fun resultsUseTocNamesAndActualUnevenPublicationPositions() =
        runBlocking {
            val href = requireNotNull(Url("second.xhtml"))
            val positions =
                listOf(
                    location("first.xhtml", 1, 0.0, 0.0),
                    location("second.xhtml", 2, 0.0, 0.1),
                    location("second.xhtml", 3, 0.5, 0.55),
                )
            val services =
                Publication.ServicesBuilder().apply {
                    positionsServiceFactory = {
                        object : PositionsService {
                            override suspend fun positionsByReadingOrder() =
                                listOf(positions.take(1), positions.drop(1))
                        }
                    }
                }
            val publication =
                Publication(
                    Manifest(
                        metadata = Metadata(),
                        readingOrder = listOf(Link(requireNotNull(Url("first.xhtml"))), Link(href, MediaType.XHTML)),
                        tableOfContents = listOf(Link(requireNotNull(Url("second.xhtml#start")), title = "Night sky")),
                    ),
                    SingleResourceContainer(
                        href,
                        StringResource("<html><body id='start'>" + "x".repeat(60) + " moon</body></html>"),
                    ),
                    services,
                )
            try {
                val result = publication.searchText(SearchPattern.compile("moon")).locators.single()
                assertEquals("Night sky", result.title)
                assertEquals(3, result.locations.position)
                assertTrue(result.locations.totalProgression!! > 0.9)
                assertEquals(
                    "page 3 / 3",
                    progressLabel(result.locations.totalProgression!!.toFloat(), positions, true),
                )
                assertEquals("moon", result.text.highlight)
                assertEquals(href, result.href)
            } finally {
                publication.close()
            }
        }

    @Test fun matchesInOneResourceUseTheirOwnTocSection() =
        runBlocking {
            val href = requireNotNull(Url("book.xhtml"))
            val publication =
                Publication(
                    Manifest(
                        metadata = Metadata(),
                        readingOrder = listOf(Link(href, MediaType.XHTML, title = "Whole book")),
                        tableOfContents =
                            listOf(
                                Link(requireNotNull(Url("book.xhtml#one")), title = "First night"),
                                Link(requireNotNull(Url("book.xhtml#two")), title = "Second night"),
                            ),
                    ),
                    SingleResourceContainer(
                        href,
                        StringResource(
                            "<html><body><section><h2 id='one'>First</h2><p>The moon rose.</p></section>" +
                                "<section><h2 id='two'>Second</h2><p>The moon rose.</p></section></body></html>",
                        ),
                    ),
                )
            try {
                val results = publication.searchText(SearchPattern.compile("moon")).locators
                assertEquals(listOf("First night", "Second night"), results.map { it.title })
                assertEquals(listOf("moon", "moon"), results.map { it.text.highlight })
                assertTrue(results.all { it.href == href })
                assertTrue(results[0].locations.progression!! < results[1].locations.progression!!)
                assertEquals("First The ", results[0].text.before)
                assertEquals("First The moon rose. Second The ", results[1].text.before)
            } finally {
                publication.close()
            }
        }

    @Test fun resultShowsChapterAndPageWhileSelectingOriginalPassage() {
        val locator =
            location("night.xhtml", 42, 0.4, 0.7).copy(
                title = "Night sky",
                text = Locator.Text(highlight = "moon"),
            )
        var selected: Locator? = null
        compose.setContent {
            DeftTheme { ReaderSearchPanel(ReaderSearchState(results = listOf(locator)), {}, { selected = it }) }
        }
        compose.onNodeWithText("Night sky · page 42").performClick()
        assertEquals(locator, selected)
    }

    private fun location(
        href: String,
        page: Int,
        progression: Double,
        total: Double,
    ) = Locator(
        href = requireNotNull(Url(href)),
        mediaType = MediaType.XHTML,
        locations = Locator.Locations(position = page, progression = progression, totalProgression = total),
    )
}
