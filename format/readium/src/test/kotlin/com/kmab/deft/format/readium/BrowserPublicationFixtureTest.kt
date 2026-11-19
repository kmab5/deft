@file:OptIn(org.readium.r2.shared.ExperimentalReadiumApi::class)
@file:Suppress("INVISIBLE_MEMBER", "INVISIBLE_REFERENCE")

package com.kmab.deft.format.readium

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.readium.r2.navigator.epub.css.ReadiumCss
import org.readium.r2.shared.util.Url
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File

/** Exports production-sanitized content and the dependency's real assets for Chromium verification. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class BrowserPublicationFixtureTest {
    @Test fun exportSanitizedPublicationThroughTheActualReadiumCssInjector() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val directory = File("build/browser-fixture").apply { mkdirs() }
        copyAssets(context, "readium", File(directory, "assets/readium"))
        File(directory, "__deft_fonts/probe.font").apply {
            parentFile?.mkdirs()
            writeBytes(context.assets.open("readium/fonts/OpenDyslexic-Regular.otf").use { it.readBytes() })
        }
        val original =
            """
            <html xmlns="http://www.w3.org/1999/xhtml"><head><title>Rendering regression</title>
            <style>@font-face {font-family:Probe;src:url('https://readium_package/__deft_fonts/probe.font')}</style>
            <script>window.bookScriptRan=true</script></head><body onload="window.bookHandlerRan=true">
            <h1>Chapter one</h1><p id="sample">A readable paragraph with a local image.</p>
            <img id="local-image" src="image.svg"/><img id="remote-image" src="https://example.test/tracker.png"/>
            ${(1..40).joinToString("") { "<p>A long chapter paragraph $it for pagination and typography.</p>" }}
            </body></html>
            """.trimIndent()
        val sanitized = PublicationPolicy.sanitizeHtml(original)
        val injected =
            ReadiumCss(assetsBaseHref = requireNotNull(Url("https://readium_assets/")))
                .injectHtml(sanitized)
                .replace(
                    "</head>",
                    "<script src=\"https://readium_assets/readium/scripts/readium-reflowable.js\"></script></head>",
                )
        File(directory, "chapter.xhtml").writeText(injected)
        File(directory, "image.svg").writeText(
            "<svg xmlns=\"http://www.w3.org/2000/svg\" width=\"48\" height=\"32\">" +
                "<rect width=\"48\" height=\"32\" fill=\"red\"/></svg>",
        )
        assertTrue(injected.contains("ReadiumCSS-after.css"))
        assertTrue(injected.contains("readium-reflowable.js"))
        assertTrue(File(directory, "assets/readium/scripts/readium-reflowable.js").length() > 1000)
    }

    private fun copyAssets(
        context: Context,
        path: String,
        target: File,
    ) {
        val children = context.assets.list(path).orEmpty()
        if (children.isEmpty()) {
            target.parentFile?.mkdirs()
            context.assets.open(path).use { source -> target.outputStream().use(source::copyTo) }
        } else {
            children.forEach { copyAssets(context, "$path/$it", File(target, it)) }
        }
    }
}
