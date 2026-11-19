package com.kmab.deft.format.readium

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.xml.sax.InputSource
import java.io.StringReader
import javax.xml.parsers.DocumentBuilderFactory

class PublicationPolicyTest {
    @Test fun truncatedMarkupStillProducesAReadableWellFormedChapter() {
        val html =
            PublicationPolicy.sanitizeHtml(
                "<html><head><title>Damaged</title><body><h1>Chapter<p>Text before truncation &amp;",
            )
        val document =
            DocumentBuilderFactory
                .newInstance()
                .newDocumentBuilder()
                .parse(html.toByteArray().inputStream())
        assertTrue(document.documentElement.textContent.contains("Text before truncation &"))
        assertTrue(html.contains("</body>"))
    }

    @Test fun sanitizedXhtmlRemainsWellFormedXmlIncludingVoidTagsAndEntities() {
        val html =
            PublicationPolicy.sanitizeHtml(
                """<html xmlns="http://www.w3.org/1999/xhtml">
                    <head><link rel="stylesheet" href="../styles.css"/></head>
                    <body><p>A&nbsp;B &amp; C<br/><img src="../cover.jpg"/></p></body></html>""",
            )
        val document =
            DocumentBuilderFactory
                .newInstance()
                .apply {
                    isNamespaceAware = true
                    setFeature("http://apache.org/xml/features/disallow-doctype-decl", true)
                }.newDocumentBuilder()
                .parse(InputSource(StringReader(html)))
        assertTrue(document.documentElement.textContent.contains("A\u00a0B & C"))
        assertTrue(html.contains("../cover.jpg"))
        assertTrue(html.contains("../styles.css"))
    }

    @Test fun utf16BookTextIsNormalizedToValidUtf8Xhtml() {
        val source =
            """
            <?xml version="1.0" encoding="UTF-16"?>
            <html xmlns="http://www.w3.org/1999/xhtml"><head><title>日本語</title></head>
            <body><p>日本語 &amp; café</p></body></html>
            """.trimIndent().toByteArray(Charsets.UTF_16)
        val html = PublicationPolicy.sanitizeHtml(source)
        assertTrue(html.contains("日本語 &amp; café"))
        assertFalse(html.contains("UTF-16"))
        DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(html.toByteArray().inputStream())
    }

    @Test fun rejectsRemoteAndTraversalResources() {
        listOf(
            "https://example.com/font",
            "//example.com/image",
            "file:///secret",
            "../secret",
            "a/%2e%2e/secret",
            "data:text/html,hi",
        ).forEach {
            assertFalse(it, PublicationPolicy.isLocalResource(it))
        }
        assertTrue(PublicationPolicy.isLocalResource("EPUB/chapter.xhtml"))
        assertTrue(PublicationPolicy.isLocalResource("EPUB/chapter.xhtml#one"))
    }

    @Test fun removesBookScriptsAndActiveContentButPreservesTextAndLocalStyles() {
        val html =
            PublicationPolicy.sanitizeHtml(
                """
                <html><head><script src="evil.js"></script><base href="https://evil.test/">
                <link rel="stylesheet" href="styles/main.css"></head><body onload="attack()">
                <h1>Chapter one</h1><p>Readable text</p><iframe src="secret"></iframe>
                <a href="java&#x73;cript:attack()">bad link</a><img src="https://evil.test/tracker">
                <svg><script>alert(1)</script><image href="https://evil.test/a"/></svg>
                </body></html>
                """.trimIndent(),
            )
        assertFalse(html.contains("<script"))
        assertFalse(html.contains("onload"))
        assertFalse(html.contains("<iframe"))
        assertFalse(html.contains("evil.test"))
        assertTrue(html.contains("Readable text"))
        assertTrue(html.contains("styles/main.css"))
        assertTrue(html.contains("https://*/readium/scripts/"))
        assertTrue(html.contains("img-src 'self' data:"))
        assertFalse(html.contains("https://readium_assets"))
        assertFalse(html.contains("https://readium_package"))
        assertTrue(html.contains("connect-src 'none'"))
    }
}
