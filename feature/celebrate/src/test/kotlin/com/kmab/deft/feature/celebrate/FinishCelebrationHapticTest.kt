package com.kmab.deft.feature.celebrate

import android.view.HapticFeedbackConstants
import android.view.View
import android.view.ViewGroup
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.test.junit4.StateRestorationTester
import androidx.compose.ui.test.junit4.createComposeRule
import com.kmab.deft.core.designsystem.DeftTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotSame
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowDialog

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class FinishCelebrationHapticTest {
    @get:Rule val compose = createComposeRule()

    @Test fun burstUsesDialogWindowAndDoesNotReplayAfterSavedStateRestoration() {
        val restoration = StateRestorationTester(compose)
        lateinit var activityView: View
        compose.mainClock.autoAdvance = false
        restoration.setContent {
            activityView = LocalView.current
            DeftTheme {
                FinishCelebration(
                    1,
                    "Earthsea",
                    "Le Guin",
                    120,
                    2,
                    1,
                    true,
                    null,
                    false,
                    {},
                    {},
                    {},
                    animationsEnabled = true,
                )
            }
        }
        compose.mainClock.advanceTimeBy(3000)
        compose.runOnIdle {
            val firstDialogRoot = requireNotNull(ShadowDialog.getLatestDialog().window).decorView
            assertNotSame(activityView.rootView, firstDialogRoot)
            assertEquals(1, confirmationViews(firstDialogRoot).size)
            assertTrue(confirmationViews(activityView.rootView).isEmpty())
            // StateRestorationTester may retain the native dialog. Reset its recorded
            // effect so this assertion detects a new pulse, not the original burst.
            confirmationViews(firstDialogRoot).forEach { it.performHapticFeedback(HapticFeedbackConstants.NO_HAPTICS) }
            assertTrue(confirmationViews(firstDialogRoot).isEmpty())
        }
        restoration.emulateSavedInstanceStateRestore()
        compose.mainClock.advanceTimeBy(5000)
        compose.runOnIdle {
            val restoredRoot = requireNotNull(ShadowDialog.getLatestDialog().window).decorView
            assertTrue(confirmationViews(restoredRoot).isEmpty())
        }
    }

    private fun confirmationViews(root: View): List<View> =
        buildList {
            if (shadowOf(root).lastHapticFeedbackPerformed() == HapticFeedbackConstants.CONFIRM) add(root)
            if (root is ViewGroup) {
                for (index in 0 until root.childCount) addAll(confirmationViews(root.getChildAt(index)))
            }
        }
}
