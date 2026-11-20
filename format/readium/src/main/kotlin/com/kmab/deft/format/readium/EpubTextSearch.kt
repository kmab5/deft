package com.kmab.deft.format.readium

import com.kmab.deft.core.model.SearchPattern
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import org.jsoup.Jsoup
import org.readium.r2.shared.publication.Locator
import org.readium.r2.shared.publication.Publication
import org.readium.r2.shared.publication.services.positions
import org.readium.r2.shared.util.use
import kotlin.coroutines.coroutineContext

/** Searches only local reading-order text, one bounded chapter at a time. */
suspend fun Publication.searchText(
    pattern: SearchPattern,
    limit: Int = 500,
): EpubSearchResult =
    withContext(Dispatchers.Default) {
        val results = mutableListOf<Locator>()
        var incomplete = false
        val positions = positions()
        val resultLimit = limit.coerceIn(1, MAX_SEARCH_RESULTS)
        for (chapterIndex in readingOrder.indices) {
            coroutineContext.ensureActive()
            val chapter = searchChapter(chapterIndex, pattern, resultLimit - results.size, positions)
            results += chapter.locators
            incomplete = incomplete || chapter.incomplete
            if (results.size >= resultLimit) {
                incomplete = true
                break
            }
        }
        EpubSearchResult(results, incomplete)
    }

private suspend fun Publication.searchChapter(
    chapterIndex: Int,
    pattern: SearchPattern,
    limit: Int,
    positions: List<Locator>,
): EpubSearchResult {
    val link = readingOrder[chapterIndex]
    val document = searchChapterText(link)
    val base = locatorFromLink(link)
    if (document == null || base == null) return EpubSearchResult(emptyList(), incomplete = true)
    val text = document.text
    val results =
        pattern.ranges(text, limit).map { range ->
            val progression = range.first.toDouble() / text.length.coerceAtLeast(1)
            val quoteEnd = (range.first + MAX_HIGHLIGHT_LENGTH).coerceAtMost(range.last + 1)
            val titled = base.copy(title = document.titleAt(range.first) ?: base.title)
            searchLocation(titled, positions, progression, chapterIndex).copy(
                text =
                    Locator.Text(
                        before = text.substring((range.first - SNIPPET_LENGTH).coerceAtLeast(0), range.first),
                        highlight = text.substring(range.first, quoteEnd),
                        after = text.substring(quoteEnd, (quoteEnd + SNIPPET_LENGTH).coerceAtMost(text.length)),
                    ),
            )
        }
    return EpubSearchResult(results, incomplete = text.length > SearchPattern.MAX_TEXT_LENGTH)
}

private suspend fun Publication.searchChapterText(link: org.readium.r2.shared.publication.Link): EpubSearchDocument? =
    try {
        val bytes = get(link)?.use { it.read(0L..MAX_SEARCH_BYTES).getOrNull() }
        if (bytes == null || bytes.size > MAX_SEARCH_BYTES) {
            null
        } else {
            bytes.inputStream().use { EpubSearchDocument(Jsoup.parse(it, null, "").body(), link, tableOfContents) }
        }
    } catch (cancel: CancellationException) {
        throw cancel
    } catch (_: Exception) {
        // A malformed resource must not hide matches in healthy chapters.
        null
    }

private const val MAX_SEARCH_BYTES = 8_000_000L
private const val SNIPPET_LENGTH = 80

// At most 500 * (80 + 160 + 80) = 160,000 retained result-text characters.
private const val MAX_HIGHLIGHT_LENGTH = 160
private const val MAX_SEARCH_RESULTS = 500
