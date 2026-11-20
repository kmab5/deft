@file:OptIn(org.readium.r2.shared.ExperimentalReadiumApi::class)
@file:Suppress("INVISIBLE_MEMBER", "INVISIBLE_REFERENCE")

package com.kmab.deft.feature.reader

import android.content.Context
import android.net.Uri
import android.view.View
import android.widget.FrameLayout
import androidx.fragment.app.FragmentActivity
import androidx.test.core.app.ApplicationProvider
import com.kmab.deft.core.model.ReaderPreferences
import com.kmab.deft.core.model.ReaderThemes
import com.kmab.deft.feature.readersettings.ReaderFontCatalog
import com.kmab.deft.format.readium.OfflineEpubEngine
import com.kmab.deft.format.readium.navigatorConfiguration
import com.kmab.deft.format.readium.toNavigatorPreferences
import kotlinx.coroutines.runBlocking
import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.readium.r2.navigator.epub.EpubNavigatorFactory
import org.readium.r2.navigator.epub.EpubNavigatorFragment
import org.readium.r2.navigator.epub.css.FontFamilyDeclaration
import org.readium.r2.shared.publication.Publication
import org.readium.r2.shared.util.Url
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import java.io.ByteArrayOutputStream
import java.io.File
import java.security.MessageDigest
import java.util.zip.CRC32
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class EpubNavigatorAttachmentTest {
    @Test fun realEpubAndBundledCatalogAttachAnActualReadiumNavigator() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val bytes = epubBytes()
        val uri = Uri.parse("content://navigator-smoke/book.epub")
        shadowOf(context.contentResolver).registerInputStream(uri, bytes.inputStream())
        val hash = MessageDigest.getInstance("SHA-256").digest(bytes).joinToString("") { "%02x".format(it) }
        val opened = runBlocking { OfflineEpubEngine(context).open(uri, hash, ReaderFontCatalog.bundled) }
        val controller = Robolectric.buildActivity(FragmentActivity::class.java)
        try {
            val activity = controller.setup().visible().get()
            val container = FrameLayout(activity).apply { id = View.generateViewId() }
            activity.setContentView(container)
            val navigator = createNavigator(activity, opened.publication)

            activity.supportFragmentManager
                .beginTransaction()
                .replace(container.id, navigator)
                .commitNow()

            assertEquals(2, opened.publication.readingOrder.size)
            assertTrue(navigator.isAdded)
            assertNotNull(navigator.view)
            assertSame(container, navigator.requireView().parent)
            exportActualNavigatorFonts(navigator, opened.publication)
            activity.supportFragmentManager
                .beginTransaction()
                .remove(navigator)
                .commitNow()
        } finally {
            controller.pause().stop().destroy()
            opened.close()
        }
    }

    private fun exportActualNavigatorFonts(
        navigator: EpubNavigatorFragment,
        publication: Publication,
    ) {
        val directory = File("build/browser-fonts").apply { mkdirs() }
        val configuration =
            navigator.javaClass
                .getDeclaredField("config")
                .apply { isAccessible = true }
                .get(navigator) as EpubNavigatorFragment.Configuration

        @Suppress("UNCHECKED_CAST")
        val declarations =
            configuration.javaClass
                .getDeclaredField("fontFamilyDeclarations")
                .apply {
                    isAccessible =
                        true
                }.get(configuration) as List<FontFamilyDeclaration>
        val assets = requireNotNull(Url("https://readium_assets/"))
        val css = declarations.flatMap { it.fontFaces }.joinToString("\n") { face -> face.toCss { assets.resolve(it) } }
        val families =
            ReaderFontCatalog.bundled.filter { it.faces.isNotEmpty() }.map { font ->
                JSONObject().put("id", font.id).put(
                    "family",
                    ReaderPreferences(
                        fontFamily = font.id,
                    ).toNavigatorPreferences(ReaderThemes.builtIn.first()).fontFamily?.name,
                )
            }
        File(
            directory,
            "fonts.json",
        ).writeText(JSONObject().put("css", css).put("families", JSONArray(families)).toString())
        runBlocking {
            publication.resources.filter { it.href.toString().startsWith("__deft_fonts/") }.forEach { link ->
                val resource = requireNotNull(publication.get(link.url()))
                try {
                    File(directory, link.href.toString()).apply {
                        parentFile?.mkdirs()
                        writeBytes(requireNotNull(resource.read().getOrNull()))
                    }
                } finally {
                    resource.close()
                }
            }
        }
        assertEquals(ReaderFontCatalog.bundled.count { it.faces.isNotEmpty() } + 1, declarations.size)
    }

    private fun createNavigator(
        activity: FragmentActivity,
        publication: org.readium.r2.shared.publication.Publication,
    ): EpubNavigatorFragment {
        val theme = ReaderThemes.builtIn.first()
        val factory =
            EpubNavigatorFactory(publication).createFragmentFactory(
                initialLocator = null,
                initialPreferences = ReaderPreferences().toNavigatorPreferences(theme),
                configuration = navigatorConfiguration(ReaderFontCatalog.bundled, theme),
            )
        return factory.instantiate(
            activity.classLoader,
            EpubNavigatorFragment::class.java.name,
        ) as EpubNavigatorFragment
    }

    private fun epubBytes(): ByteArray {
        val output = ByteArrayOutputStream()
        ZipOutputStream(output).use { zip ->
            val entries =
                listOf(
                    "META-INF/container.xml",
                    "EPUB/package.opf",
                    "EPUB/one.xhtml",
                    "EPUB/two.xhtml",
                    "EPUB/nav.xhtml",
                )
            val mimetype = "application/epub+zip".toByteArray()
            zip.putNextEntry(
                ZipEntry("mimetype").apply {
                    method = ZipEntry.STORED
                    size = mimetype.size.toLong()
                    compressedSize = size
                    crc = CRC32().apply { update(mimetype) }.value
                },
            )
            zip.write(mimetype)
            zip.closeEntry()
            entries.forEach { name ->
                zip.putNextEntry(ZipEntry(name))
                requireNotNull(javaClass.classLoader?.getResourceAsStream("navigator-epub/$name")).use {
                    it.copyTo(zip)
                }
                zip.closeEntry()
            }
        }
        return output.toByteArray()
    }
}
