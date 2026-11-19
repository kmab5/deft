package com.kmab.deft.feature.reader

import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
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
        assertEquals("location 4 / 5", progressLabel(0.7f, positions, true))
        assertEquals("70%", progressLabel(0.7f, positions, false))
        assertEquals("location 5 / 5", progressLabel(1f, positions, true))
        assertEquals("0%", progressLabel(0f, emptyList(), true))
    }
}
