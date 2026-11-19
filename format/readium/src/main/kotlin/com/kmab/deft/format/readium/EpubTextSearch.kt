package com.kmab.deft.format.readium

import com.kmab.deft.core.model.SearchPattern
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import org.jsoup.Jsoup
import org.readium.r2.shared.publication.Locator
import org.readium.r2.shared.publication.Publication
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
        val resultLimit = limit.coerceIn(1, MAX_SEARCH_RESULTS)
        for (chapterIndex in readingOrder.indices) {
            coroutineContext.ensureActive()
            val chapter = searchChapter(chapterIndex, pattern, resultLimit - results.size)
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
): EpubSearchResult {
    val link = readingOrder[chapterIndex]
    val text = searchChapterText(link)
    val base = locatorFromLink(link)
    if (text == null || base == null) return EpubSearchResult(emptyList(), incomplete = true)
    val results =
        pattern.ranges(text, limit).map { range ->
            val progression = range.first.toDouble() / text.length.coerceAtLeast(1)
            val quoteEnd = (range.first + MAX_HIGHLIGHT_LENGTH).coerceAtMost(range.last + 1)
            base.copy(
                locations =
                    Locator.Locations(
                        progression = progression,
                        totalProgression = (chapterIndex + progression) / readingOrder.size.coerceAtLeast(1),
                    ),
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

private suspend fun Publication.searchChapterText(link: org.readium.r2.shared.publication.Link): String? =
    try {
        val bytes = get(link)?.use { it.read(0L..MAX_SEARCH_BYTES).getOrNull() }
        if (bytes == null || bytes.size > MAX_SEARCH_BYTES) {
            null
        } else {
            bytes.inputStream().use { Jsoup.parse(it, null, "").body().text() }
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
