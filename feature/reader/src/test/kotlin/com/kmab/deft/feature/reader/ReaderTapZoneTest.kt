package com.kmab.deft.feature.reader

import org.junit.Assert.assertEquals
import org.junit.Test

class ReaderTapZoneTest {
    @Test fun outerFortyPercentTurnsAndMiddleTwentyPercentTogglesControls() {
        val cases =
            listOf(
                0f to ReaderTapAction.LEFT,
                199f to ReaderTapAction.LEFT,
                200f to ReaderTapAction.CHROME,
                250f to ReaderTapAction.CHROME,
                300f to ReaderTapAction.CHROME,
                301f to ReaderTapAction.RIGHT,
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
