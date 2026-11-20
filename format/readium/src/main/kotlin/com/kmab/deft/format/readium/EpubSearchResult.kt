package com.kmab.deft.format.readium

import org.readium.r2.shared.publication.Locator
import org.readium.r2.shared.publication.Publication

data class EpubSearchResult(
    val locators: List<Locator>,
    val incomplete: Boolean,
)

/** Keep the exact quote/progression for navigation while deriving labels from the publication's positions. */
internal fun Publication.searchLocation(
    base: Locator,
    positions: List<Locator>,
    progression: Double,
    chapterIndex: Int,
): Locator {
    val fallbackProgress = (chapterIndex + progression) / readingOrder.size.coerceAtLeast(1)
    return base.copy(
        title = base.title?.takeIf(String::isNotBlank) ?: "chapter ${chapterIndex + 1}",
        locations = searchPositions(base, positions, progression, fallbackProgress),
    )
}

private fun searchPositions(
    base: Locator,
    positions: List<Locator>,
    progression: Double,
    fallbackProgress: Double,
): Locator.Locations {
    val resourcePositions = positions.filter { it.href.removeFragment() == base.href.removeFragment() }
    val anchor =
        resourcePositions.lastOrNull { (it.locations.progression ?: 0.0) <= progression }
            ?: resourcePositions.firstOrNull()
    val next = anchor?.let { positions.getOrNull(positions.indexOf(it) + 1) }
    val start = anchor?.locations?.progression ?: 0.0
    val end =
        next
            ?.takeIf { it.href.removeFragment() == base.href.removeFragment() }
            ?.locations
            ?.progression ?: 1.0
    val total =
        anchor?.locations?.totalProgression?.let { startTotal ->
            val endTotal = next?.locations?.totalProgression ?: 1.0
            val fraction = ((progression - start) / (end - start).coerceAtLeast(MIN_POSITION_SPAN)).coerceIn(0.0, 1.0)
            startTotal + (endTotal - startTotal) * fraction
        } ?: fallbackProgress
    return base.locations.copy(
        progression = progression,
        totalProgression = total,
        position = anchor?.locations?.position,
    )
}

private const val MIN_POSITION_SPAN = 0.000001
