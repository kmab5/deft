package com.kmab.deft.feature.reader

import android.content.ActivityNotFoundException
import android.content.ContextWrapper
import android.content.Intent
import androidx.activity.ComponentActivity
import androidx.activity.ComponentDialog
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.kmab.deft.core.designsystem.DeftTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowDialog

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ReaderExternalLinkTest {
    @get:Rule val compose = createAndroidComposeRule<ComponentActivity>()

    @Test fun onlyExplicitConfirmationStartsBrowser() {
        val shown = mutableStateOf(true)
        compose.setContent {
            DeftTheme {
                if (shown.value) {
                    ReaderExternalLinkDialog("https://example.org/chapter?q=1", { shown.value = false }, {})
                }
            }
        }
        compose.onNodeWithText("Open example.org in your browser?").assertExists()
        assertNull(shadowOf(compose.activity).nextStartedActivity)
        compose.onNodeWithText("open browser").performClick()
        compose.waitForIdle()
        val intent = shadowOf(compose.activity).nextStartedActivity
        assertEquals(Intent.ACTION_VIEW, intent.action)
        assertEquals("https://example.org/chapter?q=1", intent.dataString)
        assertEquals(setOf(Intent.CATEGORY_BROWSABLE), intent.categories)
        assertEquals(Intent.ACTION_MAIN, intent.selector?.action)
        assertEquals(setOf(Intent.CATEGORY_APP_BROWSER), intent.selector?.categories)
        assertNull(shadowOf(compose.activity).nextStartedActivity)
        assertEquals(false, shown.value)
    }

    @Test fun cancelStaysInReaderWithoutIntent() {
        val shown = mutableStateOf(true)
        compose.setContent {
            DeftTheme {
                if (shown.value) ReaderExternalLinkDialog("https://example.org", { shown.value = false }, {})
            }
        }
        compose.onNodeWithText("cancel").performClick()
        compose.waitForIdle()
        assertNull(shadowOf(compose.activity).nextStartedActivity)
        assertEquals(false, shown.value)
        assertEquals(false, compose.activity.isFinishing)
    }

    @Test fun systemBackDismissesPromptWithoutLeavingReader() {
        val shown = mutableStateOf(true)
        compose.setContent {
            DeftTheme {
                if (shown.value) ReaderExternalLinkDialog("https://example.org", { shown.value = false }, {})
            }
        }
        compose.runOnIdle {
            (ShadowDialog.getLatestDialog() as ComponentDialog).onBackPressedDispatcher.onBackPressed()
        }
        compose.waitForIdle()
        assertEquals(false, shown.value)
        assertNull(shadowOf(compose.activity).nextStartedActivity)
        assertEquals(false, compose.activity.isFinishing)
    }

    @Test fun absentBrowserReportsFailureAndKeepsReaderOpen() {
        val shown = mutableStateOf(true)
        var unavailable = false
        val context =
            object : ContextWrapper(compose.activity) {
                override fun startActivity(intent: Intent): Unit = throw ActivityNotFoundException("No browser")
            }
        compose.setContent {
            CompositionLocalProvider(LocalContext provides context) {
                DeftTheme {
                    if (shown.value) {
                        ReaderExternalLinkDialog("https://example.org", { shown.value = false }, { unavailable = true })
                    }
                }
            }
        }
        compose.onNodeWithText("open browser").performClick()
        compose.waitForIdle()
        assertEquals(true, unavailable)
        assertEquals(false, shown.value)
        assertEquals(false, compose.activity.isFinishing)
    }

    @Test fun rejectsNonWebAndMisleadingAddresses() {
        listOf(
            "file:///etc/passwd",
            "content://books/1",
            "javascript:alert(1)",
            "intent://example.org",
            "https://trusted.org@evil.org/",
            "https://example.org\\@evil.org/",
            "//example.org",
            "https://example.org%2Fevil.org",
            "https://example.org\n.evil.org",
            "https://",
            "http:example.org",
        ).forEach { assertNull(it, externalReaderLink(it)) }
        assertEquals("example.org", externalReaderLink("https://EXAMPLE.org/path")?.domain)
        assertEquals("example.org", externalReaderLink("http://example.org/path")?.domain)
        assertEquals("xn--bcher-kva.de", externalReaderLink("https://xn--bcher-kva.de/path")?.domain)
        assertEquals("example.org:8443", externalReaderLink("https://example.org:8443/path")?.domain)
    }

    @Test fun unsupportedSchemeNeverOffersOrLaunchesBrowser() {
        val shown = mutableStateOf(true)
        var unavailable = false
        compose.setContent {
            DeftTheme {
                if (shown.value) {
                    ReaderExternalLinkDialog(
                        "file:///private/book.epub",
                        { shown.value = false },
                        { unavailable = true },
                    )
                }
            }
        }
        compose.waitForIdle()
        assertEquals(true, unavailable)
        assertEquals(false, shown.value)
        assertNull(shadowOf(compose.activity).nextStartedActivity)
    }
}
