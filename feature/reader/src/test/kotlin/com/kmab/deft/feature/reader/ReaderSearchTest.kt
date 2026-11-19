@file:OptIn(org.readium.r2.shared.ExperimentalReadiumApi::class)

package com.kmab.deft.feature.reader

import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performImeAction
import androidx.compose.ui.test.performTextInput
import com.kmab.deft.core.designsystem.DeftTheme
import com.kmab.deft.format.readium.searchText
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
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
    @get:Rule val compose = createComposeRule()

    @Test fun panelSubmitsOnlyThroughSearchActionOrIme() {
        val state = mutableStateOf(ReaderSearchState())
        var submitted = 0
        compose.setContent {
            DeftTheme {
                ReaderSearchPanel(
                    state.value,
                    { state.value = state.value.copy(query = it) },
                    {},
                    submit = { submitted++ },
                )
            }
        }
        compose.onNodeWithText("word or phrase").performTextInput("moon")
        assertEquals(0, submitted)
        compose.onNodeWithText("Search").performClick()
        assertEquals(1, submitted)
        compose.onNodeWithText("word or phrase").performImeAction()
        assertEquals(2, submitted)
    }

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

    @Test fun editingDoesNotSubmitAndAdvancedSearchUsesActualMatches() =
        runBlocking {
            val publication = publication()
            try {
                val search = ReaderSearch(this)
                search.edit("mo+n")
                assertFalse(search.state.value.searching)
                assertTrue(
                    search.state.value.results
                        .isEmpty(),
                )
                search.options(
                    com.kmab.deft.core.model
                        .SearchOptions(regex = true),
                )
                search.submit(publication)
                val result = withTimeout(10_000) { search.state.first { !it.searching } }
                assertEquals(2, result.results.size)
                assertTrue(result.results.all { it.text.highlight == "moon" })
                search.edit("[")
                search.submit(publication)
                assertTrue(
                    search.state.value.error!!
                        .contains("pattern", ignoreCase = true),
                )
            } finally {
                publication.close()
            }
        }

    @Test fun repeatedPassagesHaveDistinctNavigationLocations() =
        runBlocking {
            val passage = "padding ".repeat(20) + "moon " + "padding ".repeat(20)
            val publication = publication(passage.repeat(3))
            try {
                val result =
                    publication.searchText(
                        com.kmab.deft.core.model.SearchPattern
                            .compile("moon"),
                    )
                assertEquals(3, result.locators.size)
                assertEquals(
                    3,
                    result.locators
                        .map { it.locations.progression }
                        .distinct()
                        .size,
                )
                assertEquals(
                    3,
                    result.locators
                        .map { it.locations.totalProgression }
                        .distinct()
                        .size,
                )
                assertTrue(
                    result.locators.zipWithNext().all {
                        it.first.locations.progression!! < it.second.locations.progression!!
                    },
                )
            } finally {
                publication.close()
            }
        }

    @Test fun broadRegexKeepsBoundedNavigableText() =
        runBlocking {
            val publication = publication("word ".repeat(100_000))
            try {
                val result =
                    publication.searchText(
                        com.kmab.deft.core.model.SearchPattern.compile(
                            ".*",
                            com.kmab.deft.core.model
                                .SearchOptions(regex = true),
                        ),
                    )
                assertEquals(1, result.locators.size)
                val locator = result.locators.single()
                assertTrue(locator.text.highlight!!.length <= 160)
                assertTrue(
                    result.locators.sumOf {
                        it.text.before
                            .orEmpty()
                            .length +
                            it.text.highlight
                                .orEmpty()
                                .length +
                            it.text.after
                                .orEmpty()
                                .length
                    } <= 160_000,
                )
                assertEquals(0.0, locator.locations.progression!!, 0.0)
                assertTrue(locator.text.highlight!!.startsWith("word "))
            } finally {
                publication.close()
            }
        }

    private fun publication(text: String = "The moon rose. A bright moon."): Publication {
        val href = requireNotNull(Url("chapter.xhtml"))
        val link = Link(href, mediaType = MediaType.XHTML, title = "Night")
        return Publication(
            Manifest(metadata = Metadata(), readingOrder = listOf(link), tableOfContents = listOf(link)),
            SingleResourceContainer(
                href,
                StringResource("<html><body><p>$text</p></body></html>"),
            ),
            Publication.ServicesBuilder(search = StringSearchService.createDefaultFactory()),
        )
    }
}
