package com.kmab.deft.feature.celebrate

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.click
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.unit.dp
import com.kmab.deft.core.designsystem.DeftTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class AssessmentControlsTest {
    @get:Rule val compose = createComposeRule()

    @Test fun ratingClearAndHeartShareOneCompactControlRow() {
        var chosen: Double? = 3.5
        compose.setContent {
            var rating by remember { mutableStateOf<Double?>(3.5) }
            DeftTheme {
                Box(Modifier.width(280.dp)) {
                    AssessmentControls(rating, false, {
                        rating = it
                        chosen = it
                    }, {})
                }
            }
        }
        val rating = compose.onNodeWithContentDescription("rating")
        val clear = compose.onNodeWithContentDescription("clear rating")
        val heart = compose.onNodeWithContentDescription("favorite")
        val bounds = rating.fetchSemanticsNode().boundsInRoot
        val clearBounds = clear.fetchSemanticsNode().boundsInRoot
        val heartBounds = heart.fetchSemanticsNode().boundsInRoot
        assertEquals(bounds.center.y, heartBounds.center.y, 1f)
        assertEquals(bounds.center.y, clearBounds.center.y, 1f)
        assertTrue(bounds.right <= clearBounds.left)
        assertTrue(clearBounds.right <= heartBounds.left)
        assertTrue(bounds.height >= 48 * compose.density.density)
        rating.performSemanticsAction(SemanticsActions.SetProgress) { it(4.5f) }
        assertEquals(4.5, chosen)
        rating.performTouchInput { click(Offset(center.x * 1.3f, center.y)) }
        assertEquals(3.5, chosen)
        rating.performTouchInput { click(Offset(center.x * 1.9f, center.y)) }
        assertEquals(5.0, chosen)
        clear.performClick()
        assertNull(chosen)
    }
}
