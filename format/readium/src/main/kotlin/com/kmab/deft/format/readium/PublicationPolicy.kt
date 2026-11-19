package com.kmab.deft.format.readium

import org.jsoup.Jsoup
import org.jsoup.nodes.Comment
import org.jsoup.nodes.Document
import org.jsoup.nodes.DocumentType
import org.jsoup.nodes.Entities
import org.jsoup.nodes.XmlDeclaration
import org.jsoup.parser.Parser
import java.net.URI
import java.net.URLDecoder

/** Applied to publication bytes before Readium injects its own trusted pagination script. */
object PublicationPolicy {
    fun isLocalResource(value: String): Boolean =
        runCatching {
            val uri = URI(value)
            val decoded = URLDecoder.decode(uri.rawPath.orEmpty(), "UTF-8")
            !uri.isAbsolute && uri.rawAuthority == null && !decoded.startsWith("/") &&
                !decoded.contains('\\') &&
                decoded
                    .substringBefore('#')
                    .substringBefore('?')
                    .split('/')
                    .none { it == ".." }
        }.getOrDefault(false)

    fun sanitizeHtml(html: String): String = sanitizeDocument(Jsoup.parse(html))

    fun sanitizeHtml(bytes: ByteArray): String =
        bytes.inputStream().use {
            sanitizeDocument(Jsoup.parse(it, null, ""))
        }

    private fun sanitizeDocument(document: Document): String {
        removeDeclarations(document)
        document
            .outputSettings()
            .charset(Charsets.UTF_8)
            .prettyPrint(
                false,
            ).syntax(Document.OutputSettings.Syntax.xml)
            .escapeMode(Entities.EscapeMode.xhtml)
        document
            .select(
                "script, iframe, frame, frameset, object, embed, applet, base, form, " +
                    "input, button, textarea, select, meta[http-equiv], meta[charset]",
            ).remove()
        cleanElements(document)
        document.head().prependElement("meta").attr("charset", "utf-8")
        document
            .head()
            .prependElement("meta")
            .attr("http-equiv", "Content-Security-Policy")
            .attr("content", CSP)
        return document.outerHtml()
    }

    fun sanitizeSvg(bytes: ByteArray): String =
        bytes.inputStream().use {
            val document = Jsoup.parse(it, null, "", Parser.xmlParser())
            removeDeclarations(document)
            document.outputSettings().charset(Charsets.UTF_8)
            cleanElements(document)
            document.outerHtml()
        }

    private fun removeDeclarations(document: Document) {
        document
            .childNodes()
            .filter {
                when (it) {
                    is DocumentType, is XmlDeclaration -> true
                    is Comment -> it.isXmlDeclaration
                    else -> false
                }
            }.forEach { it.remove() }
    }

    private fun cleanElements(document: org.jsoup.nodes.Document) {
        document.allElements.forEach { element ->
            if (element.normalName().substringAfter(':').lowercase() in
                setOf("script", "foreignobject", "iframe", "object", "embed", "animate", "set")
            ) {
                element.remove()
                return@forEach
            }
            element.attributes().asList().forEach { attribute ->
                val key = attribute.key.lowercase()
                val value = attribute.value.trim()
                val isReference = key in setOf("href", "src", "xlink:href", "action", "poster", "data", "background")
                val unsafeReference = isReference && !safeHtmlReference(value, element.normalName(), key)
                if (key.startsWith("on") || key in setOf("srcdoc", "srcset", "ping", "autofocus") ||
                    unsafeReference
                ) {
                    element.removeAttr(attribute.key)
                }
            }
        }
    }

    private fun safeHtmlReference(
        value: String,
        tag: String,
        key: String,
    ): Boolean =
        isRelativeReference(value) ||
            (tag == "a" && key == "href" && (value.startsWith("https://") || value.startsWith("http://"))) ||
            (tag == "img" && key == "src" && value.startsWith("data:image/") && !value.startsWith("data:image/svg"))

    private fun isRelativeReference(value: String): Boolean =
        runCatching {
            val uri = URI(value)
            !uri.isAbsolute && uri.rawAuthority == null && !value.contains('\\')
        }.getOrDefault(false)

    // Readium's synthetic host names contain underscores, which are invalid CSP host sources.
    // 'self' admits publication resources; path-scoped sources admit the bundled Readium assets.
    // The navigator intercepts these requests locally. OfflineContainer and blockNetworkLoads
    // still reject remote resources, including remote URLs using one of these asset paths.
    private const val CSP =
        "default-src 'none'; script-src https://*/readium/scripts/; " +
            "style-src 'unsafe-inline' 'self' https://*/readium/readium-css/; " +
            "img-src 'self' data:; " +
            "font-src 'self' https://*/reader-fonts/ https://*/readium/fonts/ https://*/readium/readium-css/fonts/; " +
            "media-src 'none'; connect-src 'none'; frame-src 'none'; object-src 'none'; " +
            "base-uri 'none'; form-action 'none'"
}
