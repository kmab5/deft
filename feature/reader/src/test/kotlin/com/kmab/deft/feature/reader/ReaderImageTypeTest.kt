package com.kmab.deft.feature.reader

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ReaderImageTypeTest {
    @Test fun acceptsImageSignaturesRatherThanUntrustedFilenameExtensions() {
        assertEquals("image/png", imageMediaType("\u0089PNG\r\n\u001a\n".toByteArray(Charsets.ISO_8859_1)))
        assertEquals("image/jpeg", imageMediaType(byteArrayOf(-1, -40, -1, 0)))
        assertEquals(
            "image/svg+xml",
            imageMediaType("<?xml version=\"1.0\"?><svg xmlns=\"http://www.w3.org/2000/svg\"/>".toByteArray()),
        )
        assertNull(imageMediaType("<html><script>alert(1)</script></html>".toByteArray()))
        assertNull(imageMediaType(byteArrayOf()))
    }
}
