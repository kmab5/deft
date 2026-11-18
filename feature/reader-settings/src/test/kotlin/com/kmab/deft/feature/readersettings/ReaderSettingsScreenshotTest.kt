package com.kmab.deft.feature.readersettings

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import com.github.takahirom.roborazzi.ExperimentalRoborazziApi
import com.github.takahirom.roborazzi.RoborazziOptions
import com.github.takahirom.roborazzi.RoborazziTaskType
import com.github.takahirom.roborazzi.captureRoboImage
import com.kmab.deft.core.designsystem.DeftTheme
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

    @OptIn(ExperimentalRoborazziApi::class)
    private fun capture(
        dark: Boolean,
        scale: Float,
        name: String,
    ) {
        RuntimeEnvironment.setFontScale(scale)
        compose.setContent {
            DeftTheme(dark) {
                Surface(Modifier.fillMaxSize()) {
                    ReaderPreferencesContent(
                        ReaderPreferences(),
                        emptyList(),
                        ReaderFontCatalog.bundled,
                        {},
                        {},
                        {},
                        {},
                        Modifier.fillMaxSize(),
                        manageAssets = true,
                    )
                }
            }
        }
        val output = File("build/reports/screenshots/$name.png").apply { parentFile?.mkdirs() }
        compose.onRoot().captureRoboImage(
            output.path,
            roborazziOptions = RoborazziOptions(taskType = RoborazziTaskType.Record),
        )
    }
}
