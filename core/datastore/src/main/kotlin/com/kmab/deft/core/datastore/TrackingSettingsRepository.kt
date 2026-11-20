package com.kmab.deft.core.datastore

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import com.kmab.deft.core.model.TrackingPreferences
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TrackingSettingsRepository
    @Inject
    constructor(
        private val store: DataStore<Preferences>,
    ) {
        val preferences = store.data.map(::decode).distinctUntilChanged()

        suspend fun update(transform: (TrackingPreferences) -> TrackingPreferences) {
            store.edit { saved ->
                val value = transform(decode(saved))
                saved[REFLOWABLE] = value.reflowableIdleMinutes ?: OFF
                saved[FIXED] = value.fixedLayoutIdleMinutes ?: OFF
                saved[INDICATOR] = value.showIndicator
                saved[CELEBRATIONS] = value.celebrations
            }
        }

        private fun decode(saved: Preferences) =
            TrackingPreferences(
                reflowableIdleMinutes = timeout(saved[REFLOWABLE], TrackingPreferences.DEFAULT_REFLOWABLE_MINUTES),
                fixedLayoutIdleMinutes = timeout(saved[FIXED], TrackingPreferences.DEFAULT_FIXED_MINUTES),
                showIndicator = saved[INDICATOR] ?: false,
                celebrations = saved[CELEBRATIONS] ?: true,
            )

        private fun timeout(
            value: Int?,
            fallback: Int,
        ): Int? =
            when (value) {
                OFF -> null
                in 1..TrackingPreferences.MAX_IDLE_MINUTES -> value
                else -> fallback
            }

        private companion object {
            const val OFF = 0
            val REFLOWABLE = intPreferencesKey("tracking_reflowable_idle_minutes")
            val FIXED = intPreferencesKey("tracking_fixed_layout_idle_minutes")
            val INDICATOR = booleanPreferencesKey("tracking_indicator")
            val CELEBRATIONS = booleanPreferencesKey("appearance_celebrations")
        }
    }
