package com.kmab.deft.core.datastore

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import com.kmab.deft.core.model.TrackingPreferences
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
class TrackingSettingsRepositoryTest {
    @get:Rule val temp = TemporaryFolder()

    @Test fun defaultsAndOffSurviveFreshStore() =
        runBlocking {
            val file = temp.newFolder().resolve("tracking.preferences_pb")
            val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
            val repository = TrackingSettingsRepository(PreferenceDataStoreFactory.create(scope = scope) { file })
            assertEquals(TrackingPreferences(), repository.preferences.first())
            assertEquals(30, repository.preferences.first().minimumSessionSeconds)
            val changed = TrackingPreferences(null, 60, true, false, minimumSessionSeconds = 300)
            repository.update { changed }
            scope.coroutineContext.job.cancelAndJoin()
            val reopenedScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
            try {
                val reopened =
                    TrackingSettingsRepository(PreferenceDataStoreFactory.create(scope = reopenedScope) { file })
                assertEquals(changed, reopened.preferences.first())
            } finally {
                reopenedScope.coroutineContext.job.cancelAndJoin()
            }
        }
}
