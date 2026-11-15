package com.kmab.deft.core.datastore

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import com.kmab.deft.core.model.BookFormat
import com.kmab.deft.core.model.BookSort
import kotlinx.coroutines.flow.map
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LibrarySettingsRepository
    @Inject
    constructor(
        private val store: DataStore<Preferences>,
    ) {
        val settings =
            store.data.map { p ->
                LibrarySettings(
                    enabledFormats =
                        p[FORMATS]?.mapNotNull { name -> BookFormat.entries.find { it.name == name } }?.toSet()
                            ?: BookFormat.entries.toSet(),
                    showMissing = p[MISSING] ?: false,
                    sort = BookSort.entries.find { it.name == p[SORT] } ?: BookSort.TITLE,
                    gridView = p[GRID] ?: true,
                    lastScanAt = p[LAST_SCAN]?.let(Instant::ofEpochMilli),
                )
            }

        suspend fun setFormatEnabled(
            f: BookFormat,
            enabled: Boolean,
        ) {
            store.edit { p ->
                val current = p[FORMATS] ?: BookFormat.entries.map { it.name }.toSet()
                p[FORMATS] = if (enabled) current + f.name else current - f.name
            }
        }

        suspend fun setShowMissing(value: Boolean) {
            store.edit { it[MISSING] = value }
        }

        suspend fun setSort(value: BookSort) {
            store.edit { it[SORT] = value.name }
        }

        suspend fun setGridView(value: Boolean) {
            store.edit { it[GRID] = value }
        }

        suspend fun setLastScanAt(value: Instant) {
            store.edit { it[LAST_SCAN] = value.toEpochMilli() }
        }

        private companion object {
            val FORMATS = stringSetPreferencesKey("enabled_formats")
            val MISSING = booleanPreferencesKey("show_missing")
            val SORT = stringPreferencesKey("book_sort")
            val GRID = booleanPreferencesKey("grid_view")
            val LAST_SCAN = longPreferencesKey("last_scan_at")
        }
    }
