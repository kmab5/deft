package com.kmab.deft.core.model

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.util.Locale

class SortTitleTest {
    @Test
    fun `sort titles remove only leading English articles`() {
        mapOf(
            "The Way of Kings" to "way of kings",
            "A Memory of Light" to "memory of light",
            "An Ember" to "ember",
            "Theory" to "theory",
            "Another" to "another",
            "The The Book" to "the book",
        ).forEach { (title, expected) -> assertEquals(expected, sortTitle(title)) }
    }

    @Test
    fun `sort titles use locale independent lowercase`() {
        val original = Locale.getDefault()
        try {
            Locale.setDefault(Locale.forLanguageTag("tr-TR"))
            assertEquals("invisible cities", sortTitle("The Invisible Cities"))
        } finally {
            Locale.setDefault(original)
        }
    }
}
