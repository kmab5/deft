package com.kmab.deft

import androidx.activity.compose.setContent
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeLeft
import androidx.compose.ui.test.swipeUp
import androidx.compose.ui.unit.dp
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36], qualifiers = "w393dp-h852dp-mdpi")
class TopLevelGestureTest {
    @get:Rule val compose = createAndroidComposeRule<MainActivity>()

    @Test fun childScrollingOwnsItsGestureAndPageSwipesStillNavigate() {
        val horizontal = ScrollState(0)
        val vertical = ScrollState(0)
        val selected = mutableListOf<Int>()
        compose.runOnUiThread {
            compose.activity.setContent {
                Column(Modifier.fillMaxSize().topLevelSwipe(0) { selected += it }) {
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .height(100.dp)
                            .testTag("horizontal")
                            .horizontalScroll(horizontal),
                    ) {
                        Box(Modifier.width(1600.dp).height(100.dp))
                    }
                    Column(
                        Modifier
                            .fillMaxWidth()
                            .height(300.dp)
                            .testTag("vertical")
                            .verticalScroll(vertical),
                    ) {
                        Box(Modifier.fillMaxWidth().height(2000.dp))
                    }
                    Box(Modifier.fillMaxWidth().height(150.dp).testTag("page"))
                }
            }
        }
        compose.onNodeWithTag("horizontal").performTouchInput { swipeLeft() }
        compose.runOnIdle {
            assertTrue(horizontal.value > 0)
            assertTrue(selected.isEmpty())
        }
        compose.onNodeWithTag("vertical").performTouchInput { swipeUp() }
        compose.runOnIdle {
            assertTrue(vertical.value > 0)
            assertTrue(selected.isEmpty())
        }
        compose.onNodeWithTag("page").performTouchInput { swipeLeft() }
        compose.runOnIdle { assertEquals(listOf(1), selected) }
    }
}
