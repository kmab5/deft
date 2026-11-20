package com.kmab.deft.feature.reader

import org.readium.r2.shared.publication.Link
import org.readium.r2.shared.publication.Locator

/** Reflowable page labels use stable EPUB positions so changing typography does not renumber them. */
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
        return "page $location / ${positions.size}"
    }
    return "${(progress.coerceIn(0f, 1f) * PERCENT).toInt()}%"
}

internal fun ReaderSession.progressPositions(): List<Locator> {
    val publication = opened.publication
    return positions.map { locator ->
        locator.copy(title = chapterLabel(locator, publication.tableOfContents, publication.readingOrder))
    }
}

internal fun chapterLabel(
    locator: Locator?,
    contents: List<Link>,
    readingOrder: List<Link>,
): String {
    val href = locator?.href
    val entries = contents.flattenLinks()
    val named =
        entries.firstOrNull { it.url() == href && !it.title.isNullOrBlank() }
            ?: entries.firstOrNull { it.url().removeFragment() == href?.removeFragment() && !it.title.isNullOrBlank() }
    val index = readingOrder.indexOfFirst { it.url().removeFragment() == href?.removeFragment() }
    return locator?.title?.takeIf(String::isNotBlank) ?: named?.title
        ?: readingOrder.getOrNull(index)?.title?.takeIf(String::isNotBlank)
        ?: if (index >= 0) "chapter ${index + 1}" else "reading progress"
}

private fun List<org.readium.r2.shared.publication.Link>.flattenLinks(
    depth: Int = 0,
): List<org.readium.r2.shared.publication.Link> =
    flatMap { listOf(it) + if (depth < MAX_DEPTH) it.children.flattenLinks(depth + 1) else emptyList() }

private const val PERCENT = 100
private const val MAX_DEPTH = 12
