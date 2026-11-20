package com.kmab.deft.feature.reader

import org.junit.Assert.assertEquals
import org.junit.Test

class ReaderTapZoneTest {
    @Test fun outerThirtyFivePercentTurnsAndMiddleThirtyPercentTogglesControls() {
        val cases =
            listOf(
                0f to ReaderTapAction.LEFT,
                174f to ReaderTapAction.LEFT,
                175f to ReaderTapAction.CHROME,
                250f to ReaderTapAction.CHROME,
                325f to ReaderTapAction.CHROME,
                326f to ReaderTapAction.RIGHT,
                500f to ReaderTapAction.RIGHT,
            )
        cases.forEach { (x, action) -> assertEquals(action, readerTapAction(x, 500, true)) }
    }

    @Test fun disablingEdgeTurnsShowsControlsEverywhere() {
        listOf(0f, 199f, 250f, 301f, 500f).forEach { x ->
            assertEquals(ReaderTapAction.CHROME, readerTapAction(x, 500, false))
        }
    }
}
