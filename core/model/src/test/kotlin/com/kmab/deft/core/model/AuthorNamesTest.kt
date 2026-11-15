package com.kmab.deft.core.model

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.util.Locale

class AuthorNamesTest {
    @Test
    fun `display collapses whitespace and flips only one comma`() {
        mapOf(
            "Sanderson, Brandon" to "Brandon Sanderson",
            "  Brandon   Sanderson " to "Brandon Sanderson",
            "Le Guin, Ursula K." to "Ursula K. Le Guin",
            "Smith, Jr., John" to "Smith, Jr., John",
            "\tBrandon\u00a0 Sanderson\n" to "Brandon Sanderson",
        ).forEach { (raw, expected) -> assertEquals(expected, AuthorNames.display(raw)) }
    }

    @Test
    fun `normalized keys combine equivalent spellings while preserving Unicode letters`() {
        listOf("Brandon Sanderson", "SANDERSON, Brandon", "Brandon  Sanderson")
            .forEach { assertEquals("brandon sanderson", AuthorNames.normalizedKey(it)) }
        assertEquals("jose saramago", AuthorNames.normalizedKey("José Saramago"))
        assertEquals("jose saramago", AuthorNames.normalizedKey("Jose Saramago"))
        assertEquals("陳 小明", AuthorNames.normalizedKey("陳 小明"))
        assertEquals("александр пушкин", AuthorNames.normalizedKey("Александр Пушкин"))
        assertEquals("o neil 2", AuthorNames.normalizedKey("O’Neil--2"))
    }

    @Test
    fun `keys are independent of default locale`() {
        val original = Locale.getDefault()
        try {
            Locale.setDefault(Locale.forLanguageTag("tr-TR"))
            assertEquals("isaac asimov", AuthorNames.normalizedKey("Isaac Asimov"))
        } finally {
            Locale.setDefault(original)
        }
    }

    @Test
    fun `sort names put the last word first`() {
        assertEquals("Sanderson, Brandon", AuthorNames.sortName("Brandon Sanderson"))
        assertEquals("Plato", AuthorNames.sortName("Plato"))
    }
}
