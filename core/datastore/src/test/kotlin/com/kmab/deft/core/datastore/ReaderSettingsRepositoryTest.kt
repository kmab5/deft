package com.kmab.deft.core.datastore

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.kmab.deft.core.model.PageAnimation
import com.kmab.deft.core.model.ReaderAlignment
import com.kmab.deft.core.model.ReaderMargins
import com.kmab.deft.core.model.ReaderOrientation
import com.kmab.deft.core.model.ReaderPreferences
import com.kmab.deft.core.model.ReaderTheme
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.job
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ReaderSettingsRepositoryTest {
    @get:Rule val temp = TemporaryFolder()
    private val custom = ReaderTheme("custom-1", "night", "#000000", "#FFFFFF", "#C373FF")

    @Test fun allControlsAndThemesPersistAcrossStoreRecreation() =
        runBlocking {
            val file = temp.newFolder().resolve("reader.preferences_pb")
            val firstScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
            val first = ReaderSettingsRepository(PreferenceDataStoreFactory.create(scope = firstScope) { file })
            val expected =
                ReaderPreferences(
                    "original",
                    1.4,
                    1.8,
                    ReaderMargins.LARGE,
                    ReaderAlignment.ORIGINAL,
                    false,
                    custom.id,
                    true,
                    "sepia",
                    custom.id,
                    0.7f,
                    false,
                    true,
                    PageAnimation.NONE,
                    false,
                    true,
                    false,
                    openLastBook = true,
                    showPageNumber = true,
                    fullScreen = false,
                    tapToTurn = false,
                    orientation = ReaderOrientation.LANDSCAPE,
                )
            try {
                first.upsertTheme(custom)
                first.update { expected }
            } finally {
                firstScope.coroutineContext.job.cancelAndJoin()
            }
            val secondScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
            try {
                val second = ReaderSettingsRepository(PreferenceDataStoreFactory.create(scope = secondScope) { file })
                assertEquals(expected, second.preferences.first())
                assertEquals(listOf(custom), second.customThemes.first())
                second.deleteTheme(custom.id)
                assertEquals(expected.copy(themeId = "light", darkThemeId = "dark"), second.preferences.first())
                assertTrue(second.customThemes.first().isEmpty())
            } finally {
                secondScope.coroutineContext.job.cancelAndJoin()
            }
        }

    @Test fun concurrentEditsPreserveUnrelatedFieldsAndLibraryPreferences() =
        runBlocking {
            val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
            val store =
                PreferenceDataStoreFactory.create(
                    scope = scope,
                ) { temp.root.resolve("concurrent.preferences_pb") }
            try {
                val reader = ReaderSettingsRepository(store)
                val library = LibrarySettingsRepository(store)
                val font = launch(Dispatchers.IO) { reader.update { it.copy(fontFamily = "Inter") } }
                val layout = launch(Dispatchers.IO) { reader.update { it.copy(scroll = true) } }
                val grid = launch(Dispatchers.IO) { library.setGridView(false) }
                font.join()
                layout.join()
                grid.join()
                assertEquals("Inter", reader.preferences.first().fontFamily)
                assertTrue(reader.preferences.first().scroll)
                assertEquals(false, library.settings.first().gridView)
            } finally {
                scope.coroutineContext.job.cancelAndJoin()
            }
        }

    @Test fun corruptStoredFieldsCannotProduceInvalidReaderPreferences() =
        runBlocking {
            val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
            val store = PreferenceDataStoreFactory.create(scope = scope) { temp.root.resolve("corrupt.preferences_pb") }
            try {
                store.edit {
                    it[stringPreferencesKey("reader_preferences")] =
                        """{"fontSize":"NaN","lineHeight":100,"margins":"UNKNOWN","themeId":"gone"}"""
                    it[stringPreferencesKey("reader_custom_themes")] = "not json"
                }
                val reader = ReaderSettingsRepository(store)
                assertEquals(1.0, reader.preferences.first().fontSize, 0.0)
                assertEquals(2.5, reader.preferences.first().lineHeight, 0.0)
                assertEquals(ReaderMargins.MEDIUM, reader.preferences.first().margins)
                assertEquals("light", reader.preferences.first().themeId)
                assertTrue(reader.customThemes.first().isEmpty())
            } finally {
                scope.coroutineContext.job.cancelAndJoin()
            }
        }
}
