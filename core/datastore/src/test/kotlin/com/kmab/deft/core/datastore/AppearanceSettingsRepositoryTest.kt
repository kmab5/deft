package com.kmab.deft.core.datastore

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.kmab.deft.core.model.AppTheme
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.job
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class AppearanceSettingsRepositoryTest {
    @get:Rule val temp = TemporaryFolder()

    @Test fun defaultAndUnknownThemeFollowSystemAndSavedThemeSurvivesRestart() =
        runBlocking {
            val file = temp.newFolder().resolve("appearance.preferences_pb")
            val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
            try {
                val store = PreferenceDataStoreFactory.create(scope = scope) { file }
                val repository = AppearanceSettingsRepository(store)
                assertEquals(AppTheme.System, repository.theme.first())
                store.edit { it[stringPreferencesKey("appearance_app_theme")] = "future-theme" }
                assertEquals(AppTheme.System, repository.theme.first())
                TrackingSettingsRepository(store).update { it.copy(celebrations = false) }
                repository.setTheme(AppTheme.Dark)
                assertEquals(AppTheme.Dark, repository.theme.first())
            } finally {
                scope.coroutineContext.job.cancelAndJoin()
            }

            val reopenedScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
            try {
                val reopened = PreferenceDataStoreFactory.create(scope = reopenedScope) { file }
                assertEquals(AppTheme.Dark, AppearanceSettingsRepository(reopened).theme.first())
                assertEquals(false, TrackingSettingsRepository(reopened).preferences.first().celebrations)
            } finally {
                reopenedScope.coroutineContext.job.cancelAndJoin()
            }
        }

    @Test fun explicitThemeOverridesBothSystemModes() {
        assertEquals(false, AppTheme.System.isDark(systemDark = false))
        assertEquals(true, AppTheme.System.isDark(systemDark = true))
        assertEquals(false, AppTheme.Light.isDark(systemDark = true))
        assertEquals(true, AppTheme.Dark.isDark(systemDark = false))
    }
}
