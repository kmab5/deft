package com.kmab.deft.feature.readersettings

import android.content.Context
import android.graphics.Typeface
import android.net.Uri
import android.provider.OpenableColumns
import com.kmab.deft.core.datastore.ReaderSettingsRepository
import com.kmab.deft.core.model.ReaderFont
import com.kmab.deft.core.model.ReaderFontFace
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.File
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ReaderFontRepository
    @Inject
    constructor(
        @ApplicationContext private val context: Context,
        private val settings: ReaderSettingsRepository,
    ) {
        private val store = ImportedFontStore(File(context.filesDir, "reader-fonts"))
        private val mutableFonts = MutableStateFlow(ReaderFontCatalog.bundled)
        val fonts: StateFlow<List<ReaderFont>> = mutableFonts.asStateFlow()
        private val ready = CompletableDeferred<Unit>()
        private val mutex = Mutex()

        init {
            CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
                try {
                    mutex.withLock {
                        refresh()
                        settings.update { preferences ->
                            if (fonts.value.any { it.id == preferences.fontFamily }) {
                                preferences
                            } else {
                                preferences.copy(fontFamily = "Literata")
                            }
                        }
                    }
                } catch (_: IOException) {
                    // DataStore retries on the next change; the bundled catalog remains usable.
                } catch (_: SecurityException) {
                    // A revoked provider cannot prevent the bundled fonts from opening.
                } finally {
                    ready.complete(Unit)
                }
            }
        }

        suspend fun awaitReady(): List<ReaderFont> {
            ready.await()
            return fonts.value
        }

        suspend fun importFont(uri: Uri): ReaderFont =
            withContext(Dispatchers.IO) {
                require(uri.scheme == "content") { "Choose a font using the document picker." }
                ready.await()
                mutex.withLock {
                    val name = displayName(uri)
                    val font =
                        context.contentResolver.openInputStream(uri)?.use { input ->
                            store.importFont(input, name, ::validateAndroidFont)
                        } ?: throw IOException("The selected font could not be read.")
                    refresh()
                    font
                }
            }

        suspend fun removeFont(id: String) =
            withContext(Dispatchers.IO) {
                ready.await()
                mutex.withLock {
                    require(fonts.value.any { it.id == id && it.imported }) { "Only imported fonts can be removed." }
                    settings.update { if (it.fontFamily == id) it.copy(fontFamily = "Literata") else it }
                    store.remove(id)
                    refresh()
                }
            }

        private fun displayName(uri: Uri): String =
            context.contentResolver
                .query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)
                ?.use { cursor ->
                    if (cursor.moveToFirst()) cursor.getString(0) else null
                } ?: "imported font"

        private fun refresh() {
            mutableFonts.value = ReaderFontCatalog.bundled + store.load()
        }
    }

@Suppress("TooGenericExceptionCaught") // Typeface.createFromFile documents RuntimeException for rejected fonts.
private fun validateAndroidFont(file: File) {
    try {
        Typeface.createFromFile(file)
    } catch (error: RuntimeException) {
        throw IOException("Android could not open this font. Choose another TTF or OTF file.", error)
    }
}

object ReaderFontCatalog {
    private val names =
        listOf(
            "Literata" to "literata",
            "Source Serif 4" to "sourceserif4",
            "PT Serif" to "ptserif",
            "EB Garamond" to "ebgaramond",
            "Crimson Pro" to "crimsonpro",
            "Bitter" to "bitter",
            "Lato" to "lato",
            "Open Sans" to "opensans",
            "Inter" to "inter",
            "Atkinson Hyperlegible" to "atkinsonhyperlegible",
            "OpenDyslexic" to "opendyslexic",
        )

    val bundled: List<ReaderFont> =
        listOf(ReaderFont("original", "original", emptyList())) +
            names.map { (name, slug) ->
                ReaderFont(
                    name,
                    name,
                    listOf(
                        ReaderFontFace(assetPath = "reader-fonts/$slug-regular.woff2"),
                        ReaderFontFace(assetPath = "reader-fonts/$slug-italic.woff2", italic = true),
                        ReaderFontFace(assetPath = "reader-fonts/$slug-bold.woff2", weight = BOLD_WEIGHT),
                    ),
                )
            }

    fun previewAsset(id: String): String? =
        names
            .firstOrNull { it.first == id }
            ?.let { "reader-font-previews/${it.second}.ttf" }

    private const val BOLD_WEIGHT = 700
}
