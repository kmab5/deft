package com.kmab.deft.core.datastore

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.kmab.deft.core.model.ReaderPreferences
import com.kmab.deft.core.model.ReaderTheme
import com.kmab.deft.core.model.ReaderThemes
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ReaderSettingsRepository
    @Inject
    constructor(
        private val store: DataStore<Preferences>,
    ) {
        val preferences =
            store.data
                .map { settings ->
                    references(
                        ReaderSettingsCodec.decode(settings[PREFERENCES]),
                        ReaderSettingsCodec.themes(settings[THEMES]),
                    )
                }.distinctUntilChanged()
        val customThemes = store.data.map { ReaderSettingsCodec.themes(it[THEMES]) }.distinctUntilChanged()

        suspend fun update(transform: (ReaderPreferences) -> ReaderPreferences) {
            store.edit { settings ->
                val themes = ReaderSettingsCodec.themes(settings[THEMES])
                val current = references(ReaderSettingsCodec.decode(settings[PREFERENCES]), themes)
                settings[PREFERENCES] = ReaderSettingsCodec.encode(references(transform(current).normalized(), themes))
            }
        }

        suspend fun upsertTheme(theme: ReaderTheme) {
            require(ReaderThemes.validationError(theme) == null) { ReaderThemes.validationError(theme).orEmpty() }
            val normalized =
                theme.copy(
                    name = theme.name.trim(),
                    background = theme.background.uppercase(Locale.ROOT),
                    text = theme.text.uppercase(Locale.ROOT),
                    accent = theme.accent.uppercase(Locale.ROOT),
                )
            store.edit { settings ->
                val themes = ReaderSettingsCodec.themes(settings[THEMES])
                require(themes.any { it.id == theme.id } || themes.size < MAX_THEMES) { "Keep up to 32 custom themes." }
                settings[THEMES] = ReaderSettingsCodec.encodeThemes(themes.filterNot { it.id == theme.id } + normalized)
            }
        }

        suspend fun deleteTheme(id: String) {
            require(ReaderThemes.builtIn.none { it.id == id }) { "Built-in themes cannot be deleted." }
            store.edit { settings ->
                val themes = ReaderSettingsCodec.themes(settings[THEMES]).filterNot { it.id == id }
                settings[THEMES] = ReaderSettingsCodec.encodeThemes(themes)
                settings[PREFERENCES] =
                    ReaderSettingsCodec.encode(references(ReaderSettingsCodec.decode(settings[PREFERENCES]), themes))
            }
        }

        private fun references(
            preferences: ReaderPreferences,
            themes: List<ReaderTheme>,
        ): ReaderPreferences =
            preferences.copy(
                themeId = preferences.themeId.takeIf { ReaderThemes.find(it, themes) != null } ?: "light",
                lightThemeId = preferences.lightThemeId.takeIf { ReaderThemes.find(it, themes) != null } ?: "light",
                darkThemeId = preferences.darkThemeId.takeIf { ReaderThemes.find(it, themes) != null } ?: "dark",
            )

        private companion object {
            val PREFERENCES = stringPreferencesKey("reader_preferences")
            val THEMES = stringPreferencesKey("reader_custom_themes")
            const val MAX_THEMES = 32
        }
    }
