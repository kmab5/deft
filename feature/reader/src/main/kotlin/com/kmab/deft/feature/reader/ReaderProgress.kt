package com.kmab.deft.feature.reader

import org.readium.r2.shared.publication.Locator

/** EPUB locations are stable text positions, not screen-sized or printed pages. */
internal fun progressTarget(
    positions: List<Locator>,
    progress: Float,
): Locator? =
    positions.lastOrNull { (it.locations.totalProgression ?: 0.0) <= progress.toDouble() }
        ?: positions.firstOrNull()

internal fun progressLabel(
    progress: Float,
    positions: List<Locator>,
    showLocations: Boolean,
): String {
    if (showLocations && positions.isNotEmpty()) {
        val location = progressTarget(positions, progress)?.locations?.position ?: 1
        return "location $location / ${positions.size}"
    }
    return "${(progress.coerceIn(0f, 1f) * PERCENT).toInt()}%"
}

internal fun ReaderSession.progressPositions(): List<Locator> {
    val publication = opened.publication
    val names = publication.tableOfContents.flattenLinks().associate { it.url().removeFragment() to it.title }
    return positions.map { locator ->
        if (locator.title.isNullOrBlank()) {
            val title =
                names[locator.href.removeFragment()]
                    ?: "section ${publication.readingOrder.indexOfFirst {
                        it.url().removeFragment() == locator.href.removeFragment()
                    } + 1}"
            locator.copy(title = title)
        } else {
            locator
        }
    }
}

private fun List<org.readium.r2.shared.publication.Link>.flattenLinks(
    depth: Int = 0,
): List<org.readium.r2.shared.publication.Link> =
    flatMap { listOf(it) + if (depth < MAX_DEPTH) it.children.flattenLinks(depth + 1) else emptyList() }

private const val PERCENT = 100
private const val MAX_DEPTH = 12
