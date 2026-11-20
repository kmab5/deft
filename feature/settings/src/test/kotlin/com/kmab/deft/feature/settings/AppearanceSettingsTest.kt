package com.kmab.deft.feature.settings

import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.test.assertIsNotSelected
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import com.github.takahirom.roborazzi.ExperimentalRoborazziApi
import com.github.takahirom.roborazzi.RoborazziOptions
import com.github.takahirom.roborazzi.RoborazziTaskType
import com.github.takahirom.roborazzi.captureRoboImage
import com.kmab.deft.core.designsystem.DeftTheme
import com.kmab.deft.core.model.AppTheme
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36], qualifiers = "w393dp-h852dp-420dpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@OptIn(ExperimentalRoborazziApi::class)
class AppearanceSettingsTest {
    @get:Rule val compose = createComposeRule()

    @After fun resetScale() = RuntimeEnvironment.setFontScale(1f)

    @Test fun selectingThemeChangesSelectionAndCelebrationsRemainIndependent() {
        val theme = mutableStateOf(AppTheme.System)
        val celebrations = mutableStateOf(true)
        compose.setContent {
            DeftTheme(darkTheme = theme.value.isDark(systemDark = false)) {
                AppearanceSettingsContent(
                    theme = theme.value,
                    celebrations = celebrations.value,
                    onBack = {},
                    onTheme = { theme.value = it },
                    onCelebrations = { celebrations.value = it },
                )
            }
        }
        compose.onNodeWithText("system").assertIsSelected()
        capture("appearance-settings-paper")
        compose.onNodeWithText("dark").performClick()
        assertEquals(AppTheme.Dark, theme.value)
        compose.onNodeWithText("dark").assertIsSelected()
        compose.onNodeWithText("system").assertIsNotSelected()
        assertEquals(true, celebrations.value)
        capture("appearance-settings-ink")
        compose.onNodeWithText("celebrations").performScrollTo().performClick()
        assertEquals(false, celebrations.value)
        assertEquals(AppTheme.Dark, theme.value)
    }

    @Test fun aboutShowsVersionAndOpensSourceOnlyWhenTapped() {
        RuntimeEnvironment.setFontScale(1.5f)
        var openedUri: String? = null
        compose.setContent {
            DeftTheme(darkTheme = true) { AboutContent("0.3.1", {}, { openedUri = it }) }
        }
        capture("settings-about-ink-large")
        compose.onNodeWithText("version 0.3.1").assertExists()
        compose.onNodeWithText("GPL-3.0").performScrollTo().assertExists()
        compose.onNodeWithText("privacy").performScrollTo().assertExists()
        assertEquals(null, openedUri)
        compose.onNodeWithText("source code").performScrollTo().performClick()
        assertEquals("https://github.com/kmab5/deft", openedUri)
    }

    private fun capture(name: String) {
        val output = File("build/reports/screenshots/$name.png").also { it.parentFile?.mkdirs() }
        compose.onRoot().captureRoboImage(
            output.path,
            roborazziOptions = RoborazziOptions(taskType = RoborazziTaskType.Record),
        )
    }
}
