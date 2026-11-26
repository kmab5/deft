package com.kmab.deft

import android.content.Context
import android.content.pm.PackageManager
import androidx.core.content.FileProvider
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.xmlpull.v1.XmlPullParser
import java.io.File

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class NotesShareProviderTest {
    @Test fun mergedProviderIsPrivateAndScopesGrantsToTheNotesShareCacheDirectory() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val authority = "${context.packageName}.notes-share"
        val packages = context.packageManager
        val provider = requireNotNull(packages.resolveContentProvider(authority, PackageManager.GET_META_DATA))
        assertFalse(provider.exported)
        assertTrue(provider.grantUriPermissions)
        requireNotNull(provider.loadXmlMetaData(packages, "android.support.FILE_PROVIDER_PATHS")).use { paths ->
            while (paths.next() != XmlPullParser.START_TAG) {
                check(paths.eventType != XmlPullParser.END_DOCUMENT) { "Provider paths XML is empty" }
            }
            assertEquals("paths", paths.name)
            assertEquals(XmlPullParser.START_TAG, paths.nextTag())
            assertEquals("cache-path", paths.name)
            assertEquals("notes", paths.getAttributeValue(null, "name"))
            assertEquals("notes-share/", paths.getAttributeValue(null, "path"))
            assertEquals(XmlPullParser.END_TAG, paths.nextTag())
            assertEquals(XmlPullParser.END_TAG, paths.nextTag())
            assertEquals("paths", paths.name)
        }
    }

    @Test fun markdownIsReadableThroughTheScopedPrivateProvider() {
        // AndroidX FileProvider compares canonical paths using '/' as on Android. Robolectric uses host File paths.
        assumeTrue("FileProvider's canonical path check requires Android-style separators", File.separatorChar == '/')
        val context = ApplicationProvider.getApplicationContext<Context>()
        val authority = "${context.packageName}.notes-share"
        val share =
            File(context.cacheDir, "notes-share/fixture/deft-notes.md").apply {
                parentFile?.mkdirs()
                writeText("# Book\n\n> Passage\n")
            }
        val privateFile = File(context.cacheDir, "private.md").apply { writeText("outside the share root") }
        try {
            val uri = FileProvider.getUriForFile(context, authority, share)
            assertEquals("content", uri.scheme)
            assertEquals("deft-notes.md", uri.lastPathSegment)
            assertEquals(
                share.readText(),
                context.contentResolver
                    .openInputStream(uri)
                    ?.bufferedReader()
                    ?.use { it.readText() },
            )
            assertThrows(
                IllegalArgumentException::class.java,
            ) { FileProvider.getUriForFile(context, authority, privateFile) }
        } finally {
            privateFile.delete()
            share.delete()
            share.parentFile?.delete()
        }
    }
}
