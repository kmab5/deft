package com.kmab.deft.feature.settings

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import com.github.takahirom.roborazzi.ExperimentalRoborazziApi
import com.github.takahirom.roborazzi.RoborazziOptions
import com.github.takahirom.roborazzi.RoborazziTaskType
import com.github.takahirom.roborazzi.captureRoboImage
import com.kmab.deft.core.database.model.BookListItem
import com.kmab.deft.core.designsystem.DeftTheme
import com.kmab.deft.core.model.BookFormat
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File
import java.time.Instant

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36], qualifiers = "w393dp-h852dp-420dpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@OptIn(ExperimentalRoborazziApi::class)
class SettingsNavigationContentTest {
    @get:Rule val compose = createComposeRule()

    @Test fun chooserOpensEachRealSectionAndReturns() {
        var selected = ""
        compose.setContent {
            DeftTheme {
                SettingsHomeScreen(
                    onBack = { selected = "back" },
                    onLibrary = { selected = "library" },
                    onReader = { selected = "reader" },
                    onAppearance = { selected = "appearance" },
                    onAbout = { selected = "about" },
                )
            }
        }
        capture("settings-home")
        compose.onNodeWithText("tracking").assertExists()
        compose.onNodeWithText("appearance").assertExists()
        compose.onNodeWithText("library").performClick()
        assertEquals("library", selected)
        compose.onNodeWithText("reader").performClick()
        assertEquals("reader", selected)
        compose.onNodeWithText("appearance").performScrollTo().performClick()
        assertEquals("appearance", selected)
        compose.onNodeWithText("about").performScrollTo().performClick()
        assertEquals("about", selected)
        compose.onNodeWithContentDescription("back").performClick()
        assertEquals("back", selected)
    }

    @Test fun ignoredBookCanBeInspectedOrRestoredIndependently() {
        var opened = 0L
        var restored = 0L
        val book =
            BookListItem(
                7L,
                "Earthsea",
                "earthsea",
                "Ursula K. Le Guin",
                BookFormat.EPUB,
                null,
                Instant.EPOCH,
                missing = true,
                drmProtected = false,
            )
        compose.setContent {
            DeftTheme { IgnoredBooksContent(listOf(book), {}, { opened = it }, { restored = it }) }
        }
        capture("settings-ignored")
        compose.onNodeWithText("file missing").assertExists()
        compose.onNodeWithText("Earthsea").performClick()
        assertEquals(7L, opened)
        assertEquals(0L, restored)
        compose.onNodeWithContentDescription("un-ignore Earthsea").performClick()
        assertEquals(7L, restored)
    }

    private fun capture(name: String) {
        val output = File("build/reports/screenshots/$name.png").also { it.parentFile?.mkdirs() }
        compose.onRoot().captureRoboImage(
            output.path,
            roborazziOptions = RoborazziOptions(taskType = RoborazziTaskType.Record),
        )
    }
}
