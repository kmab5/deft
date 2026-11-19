package com.kmab.deft.core.model

import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertTimeoutPreemptively
import java.time.Duration

class SearchPatternTest {
    @Test fun literalCaseAndUnicodeWordBoundaries() {
        assertTrue(SearchPattern.compile("moon").contains("Moonlight"))
        assertFalse(SearchPattern.compile("moon", SearchOptions(matchCase = true)).contains("Moon"))
        val word = SearchPattern.compile("moon", SearchOptions(wholeWord = true))
        assertTrue(word.contains("the moon."))
        assertFalse(word.contains("moonlight"))
        assertFalse(word.contains("émoon"))
        assertTrue(SearchPattern.compile("a.b").contains("a.b"))
        assertFalse(SearchPattern.compile("a.b").contains("axb"))
    }

    @Test fun regexIsBoundedAndRejectsUnsupportedSyntax() {
        assertTrue(SearchPattern.compile("mo+n", SearchOptions(regex = true)).contains("moon"))
        assertThrows(IllegalArgumentException::class.java) { SearchPattern.compile("[", SearchOptions(regex = true)) }
        assertThrows(
            IllegalArgumentException::class.java,
        ) { SearchPattern.compile("(?=moon)", SearchOptions(regex = true)) }
        assertTimeoutPreemptively(Duration.ofSeconds(2)) {
            assertFalse(
                SearchPattern.compile("(a+)+$", SearchOptions(regex = true)).contains("a".repeat(100_000) + "!"),
            )
        }
        assertTrue(SearchPattern.compile("a*", SearchOptions(regex = true)).ranges("zzz").isEmpty())
    }
}
