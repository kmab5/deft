package com.kmab.deft.format.epub

import com.kmab.deft.core.model.ExtractedMetadata
import com.kmab.deft.core.model.MetadataException
import org.kxml2.io.KXmlParser
import org.w3c.dom.Document
import org.w3c.dom.Element
import org.xmlpull.v1.XmlPullParser
import javax.xml.parsers.DocumentBuilderFactory

internal fun parseXml(bytes: ByteArray): Document {
    val parser =
        KXmlParser().apply {
            setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, true)
            setInput(bytes.inputStream(), null)
        }
    val document = DocumentBuilderFactory.newInstance().newDocumentBuilder().newDocument()
    var current: Element? = null
    while (parser.nextToken() != XmlPullParser.END_DOCUMENT) {
        when (parser.eventType) {
            XmlPullParser.DOCDECL ->
                throw MetadataException("EPUB XML declarations and entity references are unsupported")
            XmlPullParser.ENTITY_REF -> {
                val text = resolvedEntity(parser)
                current?.appendChild(document.createTextNode(text))
            }
            XmlPullParser.START_TAG -> {
                val element = document.createElementNS(parser.namespace, parser.name)
                repeat(parser.attributeCount) { index ->
                    element.setAttributeNS(
                        parser.getAttributeNamespace(index).takeIf(String::isNotEmpty),
                        parser.getAttributeName(index),
                        parser.getAttributeValue(index),
                    )
                }
                (current ?: document).appendChild(element)
                current = element
            }
            XmlPullParser.END_TAG -> current = current?.parentNode as? Element
            XmlPullParser.TEXT, XmlPullParser.CDSECT -> current?.appendChild(document.createTextNode(parser.text))
        }
    }
    if (current != null || document.documentElement == null) throw MetadataException("Incomplete EPUB XML")
    return document
}

private fun resolvedEntity(parser: KXmlParser): String =
    parser.text ?: throw MetadataException("Unresolved EPUB XML entity")

internal fun Document.elements(
    namespace: String,
    name: String,
): List<Element> {
    val nodes = getElementsByTagNameNS(namespace, name)
    return (0 until nodes.length).map { nodes.item(it) as Element }
}

internal class OpfParser(
    bytes: ByteArray,
) {
    private val document = parseXml(bytes)
    private val metas = document.elements(OPF_NS, "meta")

    private fun dc(name: String) =
        document.elements(DC_NS, name).mapNotNull {
            it.textContent.trim().takeIf(String::isNotEmpty)
        }

    private fun named(name: String) =
        metas
            .firstOrNull {
                it.getAttribute("name") == name
            }?.getAttribute("content")
            ?.takeIf(String::isNotBlank)

    private fun refinement(
        id: String,
        property: String,
    ) = metas
        .firstOrNull {
            it.getAttribute("refines") == "#$id" && it.getAttribute("property") == property
        }?.textContent
        ?.trim()

    private val collection =
        metas.firstOrNull {
            it.getAttribute("property") == "belongs-to-collection" &&
                refinement(it.getAttribute("id"), "collection-type") == "series"
        }
    private val identifiers = dc("identifier").map { it.removePrefix("urn:isbn:").replace("-", "").replace(" ", "") }
    private val coverId = named("cover")
    val coverHref =
        document
            .elements(OPF_NS, "item")
            .firstOrNull {
                "cover-image" in it.getAttribute("properties").split(Regex("\\s+")) ||
                    (coverId != null && it.getAttribute("id") == coverId)
            }?.getAttribute("href")
            ?.takeIf(String::isNotBlank)
    val metadata =
        ExtractedMetadata.EMPTY.copy(
            title = dc("title").firstOrNull(),
            authors = dc("creator"),
            seriesName = collection?.textContent?.trim() ?: named("calibre:series"),
            seriesIndex =
                (
                    collection?.let { refinement(it.getAttribute("id"), "group-position") }
                        ?: named("calibre:series_index")
                )?.toDoubleOrNull()?.takeIf(Double::isFinite),
            language = dc("language").firstOrNull(),
            publisher = dc("publisher").firstOrNull(),
            publishedDate = dc("date").firstOrNull(),
            description = dc("description").firstOrNull()?.replace(Regex("<[^>]*>"), "")?.trim(),
            isbn10 = identifiers.firstOrNull { it.matches(Regex("[0-9]{9}[0-9Xx]")) },
            isbn13 = identifiers.firstOrNull { it.matches(Regex("[0-9]{13}")) },
        )

    private companion object {
        const val OPF_NS = "http://www.idpf.org/2007/opf"
        const val DC_NS = "http://purl.org/dc/elements/1.1/"
    }
}
