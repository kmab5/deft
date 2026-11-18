package com.kmab.deft.core.datastore

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import com.kmab.deft.core.model.BookDisplaySize
import com.kmab.deft.core.model.BookFormat
import com.kmab.deft.core.model.BookSort
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.job
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.time.Instant

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class LibrarySettingsRepositoryTest {
    @get:Rule val temp = TemporaryFolder()

    @Test fun defaultsAndEverySettingPersist() =
        runBlocking {
            val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
            val file = temp.newFolder().resolve("library.preferences_pb")
            val store = PreferenceDataStoreFactory.create(scope = scope, produceFile = { file })
            val repository = LibrarySettingsRepository(store)
            assertEquals(LibrarySettings(), repository.settings.first())
            assertEquals(
                9,
                repository.settings
                    .first()
                    .enabledFormats.size,
            )
            repository.setFormatEnabled(BookFormat.PDF, false)
            repository.setFormatFilter(BookFormat.TXT, false)
            repository.setShowMissing(true)
            repository.setSort(BookSort.DATE_ADDED)
            repository.setGridView(false)
            repository.setLastScanAt(Instant.EPOCH)
            repository.setReverseSort(true)
            repository.setGridSize(BookDisplaySize.SMALL)
            repository.setListSize(BookDisplaySize.LARGE)
            scope.coroutineContext.job.cancelAndJoin()
            val secondScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
            try {
                val reopened =
                    LibrarySettingsRepository(
                        PreferenceDataStoreFactory.create(scope = secondScope, produceFile = { file }),
                    )
                assertEquals(
                    LibrarySettings(
                        BookFormat.entries.toSet() - BookFormat.PDF,
                        true,
                        BookSort.DATE_ADDED,
                        false,
                        Instant.EPOCH,
                        reverseSort = true,
                        gridSize = BookDisplaySize.SMALL,
                        listSize = BookDisplaySize.LARGE,
                        formatFilter = BookFormat.entries.toSet() - BookFormat.TXT,
                    ),
                    reopened.settings.first(),
                )
            } finally {
                secondScope.cancel()
            }
        }

    @Test fun emptyViewFilterAndSortSurviveReopeningWithoutChangingScannerFormats() =
        runBlocking {
            val file = temp.newFolder().resolve("library.preferences_pb")
            val firstScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
            val first = LibrarySettingsRepository(PreferenceDataStoreFactory.create(scope = firstScope) { file })
            try {
                kotlinx.coroutines.coroutineScope {
                    BookFormat.entries.forEach { format ->
                        launch { first.setFormatFilter(format, false) }
                    }
                }
                first.setSort(BookSort.LAST_READ)
                first.setReverseSort(true)
            } finally {
                firstScope.coroutineContext.job.cancelAndJoin()
            }
            val secondScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
            try {
                val reopened =
                    LibrarySettingsRepository(PreferenceDataStoreFactory.create(scope = secondScope) { file })
                val saved = reopened.settings.first()
                assertEquals(emptySet<BookFormat>(), saved.formatFilter)
                assertEquals(BookFormat.entries.toSet(), saved.enabledFormats)
                assertEquals(BookSort.LAST_READ, saved.sort)
                assertEquals(true, saved.reverseSort)
            } finally {
                secondScope.coroutineContext.job.cancelAndJoin()
            }
        }
}
