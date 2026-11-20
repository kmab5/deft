package com.kmab.deft.feature.reader

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import com.kmab.deft.core.model.ReaderThemes
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], qualifiers = "w393dp-h852dp-mdpi")
class ReaderProgressGeometryTest {
    @get:Rule val compose = createComposeRule()

    @Test fun progressThumbAndTrackShareTheSameVerticalCenter() {
        compose.setContent {
            ReaderTheme(ReaderThemes.builtIn.first()) {
                ReaderChrome(
                    true,
                    ReaderChromeState("Book", 0.35f, "Chapter", emptyList(), false, true, false),
                    {},
                    {},
                    {},
                    {},
                    {},
                    {},
                )
            }
        }
        val thumb =
            compose
                .onNodeWithTag("reader-progress-thumb", useUnmergedTree = true)
                .fetchSemanticsNode()
                .boundsInRoot
        val track =
            compose
                .onNodeWithTag("reader-progress-track", useUnmergedTree = true)
                .fetchSemanticsNode()
                .boundsInRoot
        assertEquals("Track and circular thumb must share a center", track.center.y, thumb.center.y, 0.01f)
    }
}
