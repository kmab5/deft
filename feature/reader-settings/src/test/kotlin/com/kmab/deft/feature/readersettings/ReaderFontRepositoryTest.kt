package com.kmab.deft.feature.readersettings

import android.content.Context
import android.content.ContextWrapper
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.test.core.app.ApplicationProvider
import com.kmab.deft.core.datastore.ReaderSettingsRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.job
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ReaderFontRepositoryTest {
    @get:Rule val temporary = TemporaryFolder()

    @Test fun deletingSelectedImportAndReopeningMissingImportUseLiterata() =
        runBlocking {
            val application = ApplicationProvider.getApplicationContext<Context>()
            val privateDirectory = temporary.newFolder()
            val context =
                object : ContextWrapper(application) {
                    override fun getFilesDir(): File = privateDirectory
                }
            val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
            val settings =
                ReaderSettingsRepository(
                    PreferenceDataStoreFactory.create(scope = scope) {
                        temporary.root.resolve("font.preferences_pb")
                    },
                )
            try {
                val imported =
                    application.assets.open("reader-font-previews/literata.ttf").use {
                        ImportedFontStore(File(privateDirectory, "reader-fonts")).importFont(it, "chosen.ttf")
                    }
                settings.update { it.copy(fontFamily = imported.id) }
                val first = ReaderFontRepository(context, settings)
                assertTrue(first.awaitReady().any { it.id == imported.id })
                first.removeFont(imported.id)
                assertEquals("Literata", settings.preferences.first().fontFamily)
                assertFalse(File(imported.faces.single().privateFilePath!!).exists())
                settings.update { it.copy(fontFamily = imported.id) }
                ReaderFontRepository(context, settings).awaitReady()
                assertEquals("Literata", settings.preferences.first().fontFamily)
            } finally {
                scope.coroutineContext.job.cancelAndJoin()
            }
        }

    @Test fun eachBundledFamilyHasAllFacesAndARealNativePreview() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val families = ReaderFontCatalog.bundled.filter { it.id != "original" }
        assertEquals(11, families.size)
        var bytes = 0L
        families.forEach { font ->
            assertEquals(listOf(400, 400, 700), font.faces.map { it.weight })
            assertEquals(listOf(false, true, false), font.faces.map { it.italic })
            font.faces.forEach { face ->
                context.assets.open(face.assetPath!!).use { bytes += it.readBytes().size }
            }
            val preview = ReaderFontCatalog.previewAsset(font.id)!!
            context.assets.open(preview).use { bytes += it.readBytes().size }
            assertNotNull(android.graphics.Typeface.createFromAsset(context.assets, preview))
        }
        assertTrue(bytes <= 3_000_000)
    }
}
