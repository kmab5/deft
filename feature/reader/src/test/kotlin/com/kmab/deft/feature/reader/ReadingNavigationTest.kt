package com.kmab.deft.feature.reader

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ReadingNavigationTest {
    @Test fun initialRestoreAndProgrammaticJumpsAreNotReadingTurns() {
        val navigation = ReadingNavigation<String>()
        assertNull(navigation.arrived("initial", 0))
        navigation.userInput(scroll = false, atMillis = 100)
        navigation.jumpFrom("initial")
        assertNull(navigation.arrived("toc chapter", 200))
        assertEquals("initial", navigation.takeReturnPoint())
        assertNull(navigation.takeReturnPoint())
    }

    @Test fun emitsOnlyChangedLocationsAfterRecentUserInput() {
        val navigation = ReadingNavigation<String>()
        navigation.arrived("one", 0)
        navigation.userInput(scroll = false, atMillis = 100)
        assertNull(navigation.arrived("one", 200))
        assertEquals(ReadingMotion.PAGE, navigation.arrived("two", 300))
        assertNull(navigation.arrived("three", 400))
        navigation.userInput(scroll = true, atMillis = 500)
        assertEquals(ReadingMotion.SCROLL, navigation.arrived("four", 600))
        navigation.userInput(scroll = false, atMillis = 700)
        assertNull(navigation.arrived("five", 5000))
    }
}
