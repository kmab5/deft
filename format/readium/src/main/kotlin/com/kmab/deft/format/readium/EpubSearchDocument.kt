package com.kmab.deft.format.readium

import org.jsoup.nodes.Element
import org.jsoup.nodes.Node
import org.readium.r2.shared.publication.Link
import java.net.URLDecoder

/** The unmodified searchable text and TOC headings indexed against that same text. */
internal class EpubSearchDocument(
    body: Element,
    resource: Link,
    contents: List<Link>,
) {
    val text: String = body.text()
    private val sections by lazy {
        contents
            .searchSections()
            .filter { it.url().removeFragment() == resource.url().removeFragment() && !it.title.isNullOrBlank() }
            .mapNotNull { link ->
                val fragment = link.url().toString().substringAfter('#', "")
                val offset = if (fragment.isEmpty()) 0 else body.anchorOffset(fragment, text.length)
                offset?.let { it to requireNotNull(link.title) }
            }.sortedBy { it.first }
    }

    fun titleAt(offset: Int): String? = sections.lastOrNull { it.first <= offset }?.second
}

private fun List<Link>.searchSections(depth: Int = 0): List<Link> =
    flatMap { listOf(it) + if (depth < MAX_TOC_DEPTH) it.children.searchSections(depth + 1) else emptyList() }

/** Trim preceding DOM siblings in a private clone; suffix length then identifies the anchor in body.text(). */
private fun Element.anchorOffset(
    fragment: String,
    textLength: Int,
): Int? {
    val id = URLDecoder.decode(fragment.replace("+", "%2B"), Charsets.UTF_8.name())
    val suffix = clone()
    var node: Node =
        suffix.getElementById(id) ?: suffix.getElementsByAttributeValue("name", id).firstOrNull()
            ?: return null
    while (node !== suffix) {
        while (node.previousSibling() != null) node.previousSibling()?.remove()
        node = node.parentNode() ?: break
    }
    return (textLength - suffix.text().length).coerceAtLeast(0)
}

private const val MAX_TOC_DEPTH = 12
