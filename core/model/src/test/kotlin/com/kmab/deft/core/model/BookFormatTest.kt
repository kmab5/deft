package com.kmab.deft.core.model

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test

class BookFormatTest {
    @Test
    fun `recognizes only supported extensions including compound and alternate extensions`() {
        mapOf(
            "Book.EPUB" to BookFormat.EPUB,
            "a.mobi" to BookFormat.MOBI,
            "a.prc" to BookFormat.MOBI,
            "a.azw" to BookFormat.AZW,
            "a.azw3" to BookFormat.AZW3,
            "a.kf8" to BookFormat.AZW3,
            "a.fb2" to BookFormat.FB2,
            "x.fb2.zip" to BookFormat.FB2,
            "x.FB2.ZIP" to BookFormat.FB2,
            "a.txt" to BookFormat.TXT,
            "a.pdf" to BookFormat.PDF,
            "a.djvu" to BookFormat.DJVU,
            "a.djv" to BookFormat.DJVU,
            "a.cbz" to BookFormat.CBZ,
        ).forEach { (name, expected) -> assertEquals(expected, BookFormat.fromFileName(name), name) }
        listOf("a.rtf", "a.lit", "noext", "epub", "a.zip", "a.epub.bak", "a.notfb2.zip")
            .forEach { assertNull(BookFormat.fromFileName(it), it) }
    }

    @Test
    fun `hash accepts exactly lowercase SHA256 hexadecimal`() {
        val valid = "0123456789abcdef".repeat(4)
        assertEquals(valid, ContentHash(valid).hex)
        listOf("abc", "a".repeat(63), "a".repeat(65), "A".repeat(64), "g".repeat(64))
            .forEach { invalid -> assertThrows(IllegalArgumentException::class.java) { ContentHash(invalid) } }
    }
}
