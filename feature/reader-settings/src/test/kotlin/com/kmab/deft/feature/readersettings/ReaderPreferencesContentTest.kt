package com.kmab.deft.feature.readersettings

import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performSemanticsAction
import com.kmab.deft.core.designsystem.DeftTheme
import com.kmab.deft.core.model.PageAnimation
import com.kmab.deft.core.model.ReaderAlignment
import com.kmab.deft.core.model.ReaderMargins
import com.kmab.deft.core.model.ReaderPreferences
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ReaderPreferencesContentTest {
    @get:Rule val compose = createComposeRule()

    @After fun resetScale() {
        RuntimeEnvironment.setFontScale(1f)
    }

    @Test fun textAndPageChangesRemainReachableAtLargeFontScale() {
        RuntimeEnvironment.setFontScale(1.5f)
        val state = mutableStateOf(ReaderPreferences())
        compose.setContent {
            DeftTheme {
                ReaderPreferencesContent(
                    preferences = state.value,
                    themes = emptyList(),
                    fonts = ReaderFontCatalog.bundled,
                    update = { state.value = it(state.value).normalized() },
                    onChooseFont = {},
                    onEditTheme = {},
                    onDeleteTheme = {},
                )
            }
        }
        compose.onNodeWithContentDescription("increase text size").performClick()
        compose.runOnIdle { assertEquals(1.1, state.value.fontSize, 0.001) }
        compose.onNodeWithText("page", useUnmergedTree = true).performClick()
        compose.onNodeWithText("scroll").performClick()
        compose.onNodeWithText("volume keys turn pages").performScrollTo().performClick()
        compose.runOnIdle {
            assertTrue(state.value.scroll)
            assertTrue(state.value.volumeKeys)
        }
    }

    @Test fun themeChoiceAndSystemBrightnessUseSharedPreferences() {
        val state = mutableStateOf(ReaderPreferences())
        compose.setContent {
            DeftTheme {
                ReaderPreferencesContent(
                    preferences = state.value,
                    themes = emptyList(),
                    fonts = ReaderFontCatalog.bundled,
                    update = { state.value = it(state.value).normalized() },
                    onChooseFont = {},
                    onEditTheme = {},
                    onDeleteTheme = {},
                )
            }
        }
        compose.onNodeWithText("theme", useUnmergedTree = true).performClick()
        compose.onNodeWithText("sepia").performClick()
        compose.onNodeWithText("follow system theme").performScrollTo().performClick()
        compose.runOnIdle {
            assertEquals("sepia", state.value.themeId)
            assertTrue(state.value.followSystem)
        }
        compose.onNodeWithText("use system brightness").performScrollTo().performClick()
        compose
            .onNodeWithContentDescription("reader brightness")
            .performScrollTo()
            .performSemanticsAction(SemanticsActions.SetProgress) { it(0.7f) }
        compose.runOnIdle {
            assertEquals(false, state.value.useSystemBrightness)
            assertEquals(0.7f, state.value.brightness, 0.001f)
        }
    }

    @Test fun layoutAndPageControlsChangeTheirOwnFields() {
        val state = mutableStateOf(ReaderPreferences())
        compose.setContent {
            DeftTheme {
                ReaderPreferencesContent(
                    state.value,
                    emptyList(),
                    ReaderFontCatalog.bundled,
                    { state.value = it(state.value).normalized() },
                    {},
                    {},
                    {},
                )
            }
        }
        compose.onNodeWithContentDescription("increase line height").performClick()
        compose.onNodeWithText("large").performScrollTo().performClick()
        compose.onNodeWithText("justify").performScrollTo().performClick()
        compose.onNodeWithText("hyphenation").performScrollTo().performClick()
        compose.onNodeWithText("page", useUnmergedTree = true).performClick()
        compose.onNodeWithText("none").performClick()
        compose.onNodeWithText("show status line").performScrollTo().performClick()
        compose.onNodeWithText("keep screen on").performScrollTo().performClick()
        compose.runOnIdle {
            assertEquals(
                ReaderPreferences(
                    lineHeight = 1.6,
                    margins = ReaderMargins.LARGE,
                    alignment = ReaderAlignment.JUSTIFY,
                    hyphenation = false,
                    pageAnimation = PageAnimation.NONE,
                    showStatusLine = false,
                    keepScreenOn = false,
                ),
                state.value,
            )
        }
    }
}
