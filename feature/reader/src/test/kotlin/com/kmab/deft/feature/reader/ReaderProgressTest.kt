package com.kmab.deft.feature.reader

import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.readium.r2.shared.publication.Link
import org.readium.r2.shared.publication.Locator
import org.readium.r2.shared.util.Url
import org.readium.r2.shared.util.mediatype.MediaType
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ReaderProgressTest {
    private val positions =
        (0..4).map { index ->
            Locator(
                href = requireNotNull(Url("chapter-$index.xhtml")),
                mediaType = MediaType.XHTML,
                title = "Chapter $index",
                locations = Locator.Locations(position = index + 1, totalProgression = index * 0.2),
            )
        }

    @Test fun scrubPreviewsTargetChapterAndStableLocation() {
        assertEquals("Chapter 3", progressTarget(positions, 0.7f)?.title)
        assertEquals("page 4 / 5", progressLabel(0.7f, positions, true))
        assertEquals("70%", progressLabel(0.7f, positions, false))
        assertEquals("page 5 / 5", progressLabel(1f, positions, true))
        assertEquals("0%", progressLabel(0f, emptyList(), true))
    }

    @Test fun unnamedLocationsUseTheChapterNameInsteadOfSpineSectionNumbers() {
        val point = positions[2].copy(title = null)
        val chapter = Link(point.href, title = "The old house")
        assertEquals("The old house", chapterLabel(point, listOf(chapter), listOf(chapter)))
        assertEquals("Chapter 2", chapterLabel(positions[2], listOf(chapter), listOf(chapter)))
        assertEquals("chapter 1", chapterLabel(point, emptyList(), listOf(chapter.copy(title = null))))
    }

    @Test fun fragmentChapterUsesItsOwnTitleBeforeTheParentResourceTitle() {
        val child = Link(requireNotNull(Url("chapter-2.xhtml#arrival")), title = "Arrival")
        val parent = Link(requireNotNull(Url("chapter-2.xhtml")), title = "Part two", children = listOf(child))
        val point = positions[2].copy(href = child.url(), title = null)
        assertEquals("Arrival", chapterLabel(point, listOf(parent), listOf(parent)))
    }
}
