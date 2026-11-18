package com.kmab.deft.feature.readersettings

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.dp
import com.github.takahirom.roborazzi.ExperimentalRoborazziApi
import com.github.takahirom.roborazzi.RoborazziOptions
import com.github.takahirom.roborazzi.RoborazziTaskType
import com.github.takahirom.roborazzi.captureRoboImage
import com.kmab.deft.core.designsystem.DeftTheme
import com.kmab.deft.core.designsystem.LocalDeftColors
import com.kmab.deft.core.model.ReaderPreferences
import org.junit.After
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36], qualifiers = "w393dp-h852dp-mdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class ReaderSettingsScreenshotTest {
    @get:Rule val compose = createComposeRule()

    @After fun resetScale() {
        RuntimeEnvironment.setFontScale(1f)
    }

    @Test fun paper() {
        capture(false, 1f, "reader-settings-paper")
    }

    @Test fun inkLargeText() {
        capture(true, 1.5f, "reader-settings-ink-large")
    }

    @Test fun themeInkLargeText() {
        capture(true, 1.5f, "reader-settings-theme-ink-large", tab = "theme")
    }

    @Test fun pagePaper() {
        capture(false, 1f, "reader-settings-page-paper", tab = "page")
    }

    @Test fun quickPanelPaperLargeText() {
        capture(false, 1.5f, "reader-settings-quick-paper-large", quick = true)
    }

    @OptIn(ExperimentalRoborazziApi::class)
    private fun capture(
        dark: Boolean,
        scale: Float,
        name: String,
        tab: String? = null,
        quick: Boolean = false,
    ) {
        RuntimeEnvironment.setFontScale(scale)
        compose.setContent {
            DeftTheme(dark) {
                Surface(
                    (if (quick) Modifier.fillMaxWidth().height(480.dp) else Modifier.fillMaxSize())
                        .testTag("reader-settings-preview"),
                    color = LocalDeftColors.current.bg,
                ) {
                    ReaderPreferencesContent(
                        ReaderPreferences(),
                        emptyList(),
                        ReaderFontCatalog.bundled,
                        {},
                        {},
                        {},
                        {},
                        Modifier.fillMaxSize(),
                        manageAssets = !quick,
                    )
                }
            }
        }
        if (tab != null) compose.onNodeWithText(tab, useUnmergedTree = true).performClick()
        val output = File("build/reports/screenshots/$name.png").apply { parentFile?.mkdirs() }
        compose.onNodeWithTag("reader-settings-preview").captureRoboImage(
            output.path,
            roborazziOptions = RoborazziOptions(taskType = RoborazziTaskType.Record),
        )
    }
}
