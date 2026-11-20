package com.kmab.deft.feature.readersettings

import androidx.compose.foundation.layout.height
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertHeightIsEqualTo
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.junit4.StateRestorationTester
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipe
import androidx.compose.ui.test.swipeLeft
import androidx.compose.ui.test.swipeRight
import androidx.compose.ui.test.swipeUp
import androidx.compose.ui.unit.dp
import com.kmab.deft.core.designsystem.DeftTheme
import com.kmab.deft.core.model.ReaderPreferences
import org.junit.After
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36], qualifiers = "w320dp-h640dp-mdpi")
class ReaderSettingsPagerTest {
    @get:Rule val compose = createComposeRule()
    private val preferences = mutableStateOf(ReaderPreferences())

    @After fun resetScale() {
        RuntimeEnvironment.setFontScale(1f)
    }

    @Test fun fullSettingsSwipesAndTabClicksStaySynchronized() {
        show(manageAssets = true)
        assertSwipeNavigation()
    }

    @Test fun quickSettingsSwipesStayBoundedAtLargeText() {
        RuntimeEnvironment.setFontScale(1.5f)
        show(manageAssets = false)
        assertSwipeNavigation()
        compose.onNodeWithTag("settings-panel").assertHeightIsEqualTo(480.dp)
        compose.onNodeWithText("hyphenation").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("text").assertIsDisplayed().assertIsSelected()
    }

    @Test fun quickPanelWithoutAnExplicitHeightStillUsesItsCap() {
        show(manageAssets = false, fixedHeight = false)
        compose.onNodeWithTag("settings-panel").assertHeightIsEqualTo(560.dp)
        compose.onNodeWithText("page").performClick()
        compose.onNodeWithText("keep screen on").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("page").assertIsDisplayed().assertIsSelected()
    }

    @Test fun sliderDragChangesBrightnessWithoutSwitchingTabs() {
        preferences.value = ReaderPreferences(useSystemBrightness = false, brightness = 0.2f)
        show(manageAssets = false)
        compose.onNodeWithText("theme").performClick()
        compose.onNodeWithContentDescription("reader brightness").performScrollTo().performTouchInput {
            swipe(Offset(width * 0.2f, centerY), Offset(width * 0.8f, centerY), 500)
        }
        compose.onNodeWithText("theme").assertIsSelected()
        compose.runOnIdle { assertTrue(preferences.value.brightness > 0.5f) }
    }

    @Test fun verticalScrollAndPageSwitchPreserveEachTabsPosition() {
        show(manageAssets = true)
        compose.onNodeWithText("page").performClick()
        compose.onNodeWithTag("settings-panel").performTouchInput { swipeUp() }
        compose.onNodeWithText("page").assertIsSelected()
        compose.onNodeWithText("keep screen on").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("text").performClick()
        compose.onNodeWithText("page").performClick()
        compose.onNodeWithText("keep screen on").assertIsDisplayed()
    }

    @Test fun selectedPageAndScrollPositionSurviveStateRestoration() {
        val restoration = StateRestorationTester(compose)
        show(manageAssets = true, restoration = restoration)
        compose.onNodeWithText("page").performClick()
        compose.onNodeWithText("keep screen on").performScrollTo()
        restoration.emulateSavedInstanceStateRestore()
        compose.onNodeWithText("page").assertIsSelected()
        compose.onNodeWithText("keep screen on").assertIsDisplayed()
    }

    private fun assertSwipeNavigation() {
        compose.onNodeWithTag("settings-panel").performTouchInput { swipeLeft() }
        compose.onNodeWithText("theme").assertIsSelected()
        compose.onNodeWithText("appearance").assertIsDisplayed()
        compose.onNodeWithTag("settings-panel").performTouchInput { swipeLeft() }
        compose.onNodeWithText("page").assertIsSelected()
        compose.onNodeWithText("navigation").assertIsDisplayed()
        compose.onNodeWithTag("settings-panel").performTouchInput { swipeRight() }
        compose.onNodeWithText("theme").assertIsSelected()
        compose.onNodeWithText("text").performClick()
        compose.onNodeWithText("text").assertIsSelected()
        compose.onNodeWithText("typeface & size").assertIsDisplayed()
    }

    private fun show(
        manageAssets: Boolean,
        restoration: StateRestorationTester = StateRestorationTester(compose),
        fixedHeight: Boolean = true,
    ) {
        restoration.setContent {
            DeftTheme {
                ReaderPreferencesContent(
                    preferences.value,
                    emptyList(),
                    ReaderFontCatalog.bundled,
                    { preferences.value = it(preferences.value).normalized() },
                    {},
                    {},
                    {},
                    (if (fixedHeight) Modifier.height(480.dp) else Modifier).testTag("settings-panel"),
                    manageAssets = manageAssets,
                )
            }
        }
    }
}
