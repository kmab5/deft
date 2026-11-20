package com.kmab.deft.feature.reader

import android.util.Xml
import org.xmlpull.v1.XmlPullParser

/** Reject active content, custom entities and non-fragment references before the native SVG parser. */
internal fun safeReaderSvg(bytes: ByteArray): String? {
    val parser =
        Xml.newPullParser().apply {
            setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, true)
            setInput(bytes.inputStream(), null)
        }
    var elements = 0
    val references = ReaderSvgReferences()
    val stylesheet = SvgStylesheet()
    while (parser.eventType != XmlPullParser.END_DOCUMENT) {
        val tokenSafe = safeSvgToken(parser, elements)
        if (!tokenSafe || !stylesheet.accept(parser) || !updateSvgReferences(parser, references)) return null
        if (parser.eventType == XmlPullParser.START_TAG) elements++
        parser.nextToken()
    }
    return bytes.toString(Charsets.UTF_8).takeIf { elements > 0 && references.isBounded() }
}

private fun safeSvgToken(
    parser: XmlPullParser,
    elements: Int,
): Boolean =
    when (parser.eventType) {
        XmlPullParser.DOCDECL, XmlPullParser.PROCESSING_INSTRUCTION -> false
        XmlPullParser.START_TAG ->
            (elements != 0 || parser.name == "svg") &&
                elements < MAX_SVG_ELEMENTS && safeSvgElement(parser)
        else -> true
    }

private fun safeSvgElement(parser: XmlPullParser): Boolean {
    if (parser.name.lowercase() in setOf("script", "foreignobject")) return false
    return (0 until parser.attributeCount).all { index ->
        val name = parser.getAttributeName(index).lowercase()
        val value = parser.getAttributeValue(index).trim()
        when {
            name.startsWith("on") -> false
            name == "href" || name == "src" -> value.startsWith("#")
            else -> safeSvgCss(value)
        }
    }
}

private fun updateSvgReferences(
    parser: XmlPullParser,
    references: ReaderSvgReferences,
): Boolean =
    when (parser.eventType) {
        XmlPullParser.START_TAG ->
            references.begin(parser.name, parser.getAttributeValue(null, "id"), svgReferences(parser))
        XmlPullParser.END_TAG -> {
            references.end()
            true
        }
        else -> true
    }

// Reference-bearing CSS must be inline so every render-graph edge is explicit.
private fun svgReferences(parser: XmlPullParser): List<String> =
    (0 until parser.attributeCount).flatMap { index ->
        val value = parser.getAttributeValue(index).trim()
        if (parser.getAttributeName(index).lowercase() in setOf("href", "src")) {
            listOf(value.removePrefix("#"))
        } else {
            SVG_URL
                .findAll(value)
                .map {
                    it.groupValues[1]
                        .trim()
                        .trim('\'', '"')
                        .removePrefix("#")
                }.toList()
        }
    }

private fun safeSvgCss(value: String): Boolean =
    !value.contains("@import", ignoreCase = true) && !value.contains('\\') &&
        SVG_URL.findAll(value).all {
            it.groupValues[1]
                .trim()
                .trim('\'', '"')
                .startsWith("#")
        }

/** Validate complete CSS for both SAX and AndroidSVG 1.4's entity-skipping pull-parser path. */
private class SvgStylesheet {
    private var content: StringBuilder? = null
    private var withoutEntities: StringBuilder? = null

    fun accept(parser: XmlPullParser): Boolean =
        when (parser.eventType) {
            XmlPullParser.START_TAG -> {
                if (parser.name == "style") {
                    content = StringBuilder()
                    withoutEntities = StringBuilder()
                }
                true
            }
            XmlPullParser.END_TAG -> parser.name != "style" || finish()
            XmlPullParser.TEXT, XmlPullParser.CDSECT -> append(parser.text, entity = false)
            XmlPullParser.ENTITY_REF -> append(parser.text, entity = true)
            else -> true
        }

    private fun finish(): Boolean {
        val variants = listOf(content?.toString().orEmpty(), withoutEntities?.toString().orEmpty())
        content = null
        withoutEntities = null
        return variants.all { safeSvgCss(it) && !SVG_URL.containsMatchIn(it) }
    }

    private fun append(
        text: String?,
        entity: Boolean,
    ): Boolean {
        val target = content ?: return true
        return if (text == null || target.length + text.length > MAX_STYLESHEET_CHARS) {
            false
        } else {
            target.append(text)
            if (!entity) withoutEntities?.append(text)
            true
        }
    }
}

private const val MAX_STYLESHEET_CHARS = 128_000
private val SVG_URL = Regex("url\\s*\\(([^)]*)\\)", RegexOption.IGNORE_CASE)
private const val MAX_SVG_ELEMENTS = 10_000
