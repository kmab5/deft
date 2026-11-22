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

    @Test fun independentBrowseUpdatesDoNotOverwriteEachOtherAndResetKeepsSort() =
        runBlocking {
            val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
            val file = temp.newFolder().resolve("browse.preferences_pb")
            val repository = LibrarySettingsRepository(PreferenceDataStoreFactory.create(scope = scope) { file })
            try {
                kotlinx.coroutines.coroutineScope {
                    launch { repository.setCategorySort("created") }
                    launch { repository.setCategoryReview("review") }
                    launch { repository.setCategoryState(com.kmab.deft.core.model.BookState.READING) }
                    launch { repository.setAuthorSort("time") }
                    launch { repository.setAuthorRating("rated") }
                    launch { repository.setAuthorState(com.kmab.deft.core.model.BookState.FINISHED) }
                }
                repository.toggleCategoryReverse()
                val saved = repository.settings.first()
                assertEquals("created", saved.categorySort)
                assertEquals("review", saved.categoryReview)
                assertEquals(com.kmab.deft.core.model.BookState.READING, saved.categoryState)
                assertEquals("time", saved.authorSort)
                assertEquals("rated", saved.authorRating)
                assertEquals(com.kmab.deft.core.model.BookState.FINISHED, saved.authorState)
                repository.resetCategoryFilters()
                repository.resetAuthorFilters()
                val reset = repository.settings.first()
                assertEquals("created", reset.categorySort)
                assertEquals(true, reset.categoryReverse)
                assertEquals("all", reset.categoryReview)
                assertEquals(null, reset.categoryState)
                assertEquals("time", reset.authorSort)
                assertEquals("all", reset.authorRating)
                assertEquals(null, reset.authorState)
            } finally {
                scope.coroutineContext.job.cancelAndJoin()
            }
        }

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
            repository.setAdvancedSearch(true)
            repository.setShowMissing(true)
            repository.setSort(BookSort.DATE_ADDED)
            repository.setGridView(false)
            repository.setLastScanAt(Instant.EPOCH)
            repository.setReverseSort(true)
            repository.setGridSize(BookDisplaySize.SMALL)
            repository.setListSize(BookDisplaySize.LARGE)
            repository.setSeriesAutoFill(false)
            repository.setCategoryFilter(42L)
            repository.setCategoryTypeFilter(7L)
            repository.setGroupCategoriesByType(true)
            repository.setCategoryGridView(true)
            repository.setCategoryBrowse("created", true, "review", com.kmab.deft.core.model.BookState.READING)
            repository.setAuthorBrowse("time", true, "rated", com.kmab.deft.core.model.BookState.FINISHED)
            scope.coroutineContext.job.cancelAndJoin()
            val secondScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
            try {
                val reopened =
                    LibrarySettingsRepository(
                        PreferenceDataStoreFactory.create(scope = secondScope, produceFile = { file }),
                    )
                assertEquals(
                    configuredSettings(),
                    reopened.settings.first(),
                )
            } finally {
                secondScope.cancel()
            }
        }

    private fun configuredSettings() =
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
            advancedSearch = true,
            seriesAutoFill = false,
            categoryFilter = 42L,
            categoryTypeFilter = 7L,
            groupCategoriesByType = true,
            categoryGridView = true,
            categorySort = "created",
            categoryReverse = true,
            categoryReview = "review",
            categoryState = com.kmab.deft.core.model.BookState.READING,
            authorSort = "time",
            authorReverse = true,
            authorRating = "rated",
            authorState = com.kmab.deft.core.model.BookState.FINISHED,
        )

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
                first.setStateFilter(com.kmab.deft.core.model.BookState.ABANDONED, false)
                first.setFavoritesOnly(true)
                first.setMinimumRating(3.5)
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
                assertEquals(
                    com.kmab.deft.core.model.BookState.entries
                        .toSet() - com.kmab.deft.core.model.BookState.ABANDONED,
                    saved.stateFilter,
                )
                assertEquals(true, saved.favoritesOnly)
                assertEquals(3.5, saved.minimumRating)
                assertEquals(true, saved.reverseSort)
            } finally {
                secondScope.coroutineContext.job.cancelAndJoin()
            }
        }
}
