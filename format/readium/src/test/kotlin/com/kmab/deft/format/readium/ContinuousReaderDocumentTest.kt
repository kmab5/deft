package com.kmab.deft.format.readium

import org.jsoup.Jsoup
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ContinuousReaderDocumentTest {
    @Test fun manifestCannotBreakOutIntoMarkupOrAnExecutableScript() {
        val html = ContinuousReaderDocument.html(listOf("OPS/one.xhtml" to "</script><script>alert(1)</script>\n\"&"))
        val document = Jsoup.parse(html)
        val scripts = document.select("script")
        assertEquals(2, scripts.size)
        assertEquals("application/json", scripts.first()!!.attr("type"))
        assertTrue(scripts.first()!!.data().contains("\\u003c/script\\u003e"))
        assertTrue(scripts.first()!!.data().contains("\\u000a\\\"\\u0026"))
        assertFalse(html.contains("<script>alert"))
        assertEquals("https://readium_assets/deft-reader/continuous.js", scripts.last()!!.attr("src"))
    }

    @Test fun hostRestrictsFramesToOriginalPublicationOriginAndScriptsToTrustedAssets() {
        val document = Jsoup.parse(ContinuousReaderDocument.html(listOf("OPS/chapter%201.xhtml" to "One")))
        val policy = document.selectFirst("meta[http-equiv=Content-Security-Policy]")!!.attr("content")
        assertTrue(policy.contains("default-src 'none'"))
        assertTrue(policy.contains("frame-src 'self'"))
        assertTrue(policy.contains("script-src https://*/deft-reader/"))
        assertTrue(document.selectFirst("#deft-chapters")!!.data().contains("OPS/chapter%201.xhtml"))
    }

    @Test fun emptyPublicationProducesValidEmptyManifest() {
        assertEquals(
            "[]",
            Jsoup.parse(ContinuousReaderDocument.html(emptyList())).selectFirst("#deft-chapters")!!.data(),
        )
    }
}
