package com.kmab.deft.core.datastore

import android.util.Log
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.kmab.deft.core.model.AppTheme
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AppearanceSettingsRepository
    @Inject
    constructor(
        private val store: DataStore<Preferences>,
    ) {
        val theme =
            store.data
                .map { saved -> AppTheme.entries.firstOrNull { it.name == saved[APP_THEME] } ?: AppTheme.System }
                .catch { failure ->
                    if (failure !is IOException) throw failure
                    Log.w("AppearanceSettings", "Couldn't load the app theme.", failure)
                    emit(AppTheme.System)
                }.distinctUntilChanged()

        suspend fun setTheme(theme: AppTheme) {
            store.edit { it[APP_THEME] = theme.name }
        }

        private companion object {
            val APP_THEME = stringPreferencesKey("appearance_app_theme")
        }
    }
