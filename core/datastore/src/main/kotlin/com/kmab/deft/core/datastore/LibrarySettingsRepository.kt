package com.kmab.deft.core.datastore

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.doublePreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import com.kmab.deft.core.model.BookDisplaySize
import com.kmab.deft.core.model.BookFormat
import com.kmab.deft.core.model.BookSort
import com.kmab.deft.core.model.BookState
import kotlinx.coroutines.flow.map
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton

@Suppress("TooManyFunctions") // Independent settings retain explicit typed update methods.
@Singleton
class LibrarySettingsRepository
    @Inject
    constructor(
        private val store: DataStore<Preferences>,
    ) {
        val settings =
            store.data.map { p ->
                LibrarySettings(
                    seriesAutoFill = p[SERIES_AUTO_FILL] ?: true,
                    categoryFilter = p[CATEGORY_FILTER]?.takeIf { it > 0 },
                    categoryTypeFilter = p[CATEGORY_TYPE_FILTER]?.takeIf { it >= 0 },
                    groupCategoriesByType = p[GROUP_CATEGORIES] ?: false,
                    categoryGridView = p[CATEGORY_GRID] ?: false,
                    categorySort = p[CATEGORY_SORT]?.takeIf { it in CATEGORY_SORTS } ?: "name",
                    categoryReverse = p[CATEGORY_REVERSE] ?: false,
                    categoryReview = p[CATEGORY_REVIEW]?.takeIf { it in REVIEWS } ?: "all",
                    categoryState = BookState.entries.find { it.name == p[CATEGORY_STATE] },
                    authorSort = p[AUTHOR_SORT]?.takeIf { it in AUTHOR_SORTS } ?: "name",
                    authorReverse = p[AUTHOR_REVERSE] ?: false,
                    authorRating = p[AUTHOR_RATING]?.takeIf { it in RATINGS } ?: "all",
                    authorState = BookState.entries.find { it.name == p[AUTHOR_STATE] },
                    enabledFormats =
                        p[FORMATS]?.mapNotNull { name -> BookFormat.entries.find { it.name == name } }?.toSet()
                            ?: BookFormat.entries.toSet(),
                    advancedSearch = p[ADVANCED_SEARCH] ?: false,
                    stateFilter =
                        p[STATES]?.mapNotNull { name -> BookState.entries.find { it.name == name } }?.toSet()
                            ?: BookState.entries.toSet(),
                    favoritesOnly = p[FAVORITES] ?: false,
                    minimumRating =
                        p[MIN_RATING]?.takeIf {
                            it in MIN_RATING_VALUE..MAX_RATING_VALUE &&
                                it * 2 == (it * 2).toInt().toDouble()
                        },
                    showMissing = p[MISSING] ?: false,
                    sort = BookSort.entries.find { it.name == p[SORT] } ?: BookSort.TITLE,
                    gridView = p[GRID] ?: true,
                    lastScanAt = p[LAST_SCAN]?.let(Instant::ofEpochMilli),
                    reverseSort = p[REVERSE_SORT] ?: false,
                    gridSize = BookDisplaySize.entries.find { it.name == p[GRID_SIZE] } ?: BookDisplaySize.MEDIUM,
                    listSize = BookDisplaySize.entries.find { it.name == p[LIST_SIZE] } ?: BookDisplaySize.MEDIUM,
                    formatFilter =
                        p[FILTER_FORMATS]?.mapNotNull { name -> BookFormat.entries.find { it.name == name } }?.toSet()
                            ?: BookFormat.entries.toSet(),
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

        suspend fun setFormatFilter(
            format: BookFormat,
            enabled: Boolean,
        ) {
            store.edit { p ->
                val current = p[FILTER_FORMATS] ?: BookFormat.entries.map { it.name }.toSet()
                p[FILTER_FORMATS] = if (enabled) current + format.name else current - format.name
            }
        }

        suspend fun setStateFilter(
            state: BookState,
            enabled: Boolean,
        ) {
            store.edit { p ->
                val current = p[STATES] ?: BookState.entries.map { it.name }.toSet()
                p[STATES] = if (enabled) current + state.name else current - state.name
            }
        }

        suspend fun setFavoritesOnly(value: Boolean) {
            store.edit { it[FAVORITES] = value }
        }

        suspend fun setMinimumRating(value: Double?) {
            require(
                value == null ||
                    value in MIN_RATING_VALUE..MAX_RATING_VALUE && value * 2 == (value * 2).toInt().toDouble(),
            )
            store.edit { if (value == null) it.remove(MIN_RATING) else it[MIN_RATING] = value }
        }

        suspend fun setAdvancedSearch(value: Boolean) {
            store.edit { it[ADVANCED_SEARCH] = value }
        }

        suspend fun setSeriesAutoFill(value: Boolean) {
            store.edit { it[SERIES_AUTO_FILL] = value }
        }

        suspend fun setCategoryFilter(value: Long?) {
            require(value == null || value > 0)
            store.edit { if (value == null) it.remove(CATEGORY_FILTER) else it[CATEGORY_FILTER] = value }
        }

        suspend fun setCategoryTypeFilter(value: Long?) {
            require(value == null || value >= 0)
            store.edit { if (value == null) it.remove(CATEGORY_TYPE_FILTER) else it[CATEGORY_TYPE_FILTER] = value }
        }

        suspend fun setGroupCategoriesByType(value: Boolean) {
            store.edit { it[GROUP_CATEGORIES] = value }
        }

        suspend fun setCategoryGridView(value: Boolean) {
            store.edit { it[CATEGORY_GRID] = value }
        }

        suspend fun setCategoryBrowse(
            sort: String = "name",
            reverse: Boolean = false,
            review: String = "all",
            state: BookState? = null,
        ) {
            require(sort in CATEGORY_SORTS && review in REVIEWS)
            store.edit {
                it[CATEGORY_SORT] = sort
                it[CATEGORY_REVERSE] = reverse
                it[CATEGORY_REVIEW] = review
                if (state == null) it.remove(CATEGORY_STATE) else it[CATEGORY_STATE] = state.name
            }
        }

        suspend fun setAuthorBrowse(
            sort: String = "name",
            reverse: Boolean = false,
            rating: String = "all",
            state: BookState? = null,
        ) {
            require(sort in AUTHOR_SORTS && rating in RATINGS)
            store.edit {
                it[AUTHOR_SORT] = sort
                it[AUTHOR_REVERSE] = reverse
                it[AUTHOR_RATING] = rating
                if (state == null) it.remove(AUTHOR_STATE) else it[AUTHOR_STATE] = state.name
            }
        }

        suspend fun setCategorySort(value: String) {
            require(value in CATEGORY_SORTS)
            store.edit { it[CATEGORY_SORT] = value }
        }

        suspend fun toggleCategoryReverse() {
            store.edit { it[CATEGORY_REVERSE] = !(it[CATEGORY_REVERSE] ?: false) }
        }

        suspend fun setCategoryReview(value: String) {
            require(value in REVIEWS)
            store.edit { it[CATEGORY_REVIEW] = value }
        }

        suspend fun setCategoryState(value: BookState?) {
            store.edit { if (value == null) it.remove(CATEGORY_STATE) else it[CATEGORY_STATE] = value.name }
        }

        suspend fun resetCategoryFilters() {
            store.edit {
                it.remove(CATEGORY_TYPE_FILTER)
                it.remove(CATEGORY_REVIEW)
                it.remove(CATEGORY_STATE)
                it.remove(GROUP_CATEGORIES)
            }
        }

        suspend fun setAuthorSort(value: String) {
            require(value in AUTHOR_SORTS)
            store.edit { it[AUTHOR_SORT] = value }
        }

        suspend fun toggleAuthorReverse() {
            store.edit { it[AUTHOR_REVERSE] = !(it[AUTHOR_REVERSE] ?: false) }
        }

        suspend fun setAuthorRating(value: String) {
            require(value in RATINGS)
            store.edit { it[AUTHOR_RATING] = value }
        }

        suspend fun setAuthorState(value: BookState?) {
            store.edit { if (value == null) it.remove(AUTHOR_STATE) else it[AUTHOR_STATE] = value.name }
        }

        suspend fun resetAuthorFilters() {
            store.edit {
                it.remove(AUTHOR_RATING)
                it.remove(AUTHOR_STATE)
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

        suspend fun setReverseSort(value: Boolean) {
            store.edit { it[REVERSE_SORT] = value }
        }

        suspend fun setGridSize(value: BookDisplaySize) {
            store.edit { it[GRID_SIZE] = value.name }
        }

        suspend fun setListSize(value: BookDisplaySize) {
            store.edit { it[LIST_SIZE] = value.name }
        }

        suspend fun setLastScanAt(value: Instant) {
            store.edit { it[LAST_SCAN] = value.toEpochMilli() }
        }

        private companion object {
            val CATEGORY_SORTS = setOf("name", "created", "book count", "rating")
            val AUTHOR_SORTS = setOf("name", "book count", "time", "rating")
            val REVIEWS = setOf("all", "review", "reviewed")
            val RATINGS = setOf("all", "rated", "unrated")
            val CATEGORY_SORT = stringPreferencesKey("category_sort")
            val CATEGORY_REVERSE = booleanPreferencesKey("category_reverse")
            val CATEGORY_REVIEW = stringPreferencesKey("category_review")
            val CATEGORY_STATE = stringPreferencesKey("category_state")
            val AUTHOR_SORT = stringPreferencesKey("author_sort")
            val AUTHOR_REVERSE = booleanPreferencesKey("author_reverse")
            val AUTHOR_RATING = stringPreferencesKey("author_rating")
            val AUTHOR_STATE = stringPreferencesKey("author_state")
            val SERIES_AUTO_FILL = booleanPreferencesKey("series_auto_fill")
            val CATEGORY_FILTER = longPreferencesKey("book_category_filter")
            val CATEGORY_TYPE_FILTER = longPreferencesKey("category_type_filter")
            val GROUP_CATEGORIES = booleanPreferencesKey("group_categories_by_type")
            val CATEGORY_GRID = booleanPreferencesKey("category_grid_view")
            const val MIN_RATING_VALUE = 0.5
            const val MAX_RATING_VALUE = 5.0
            val STATES = stringSetPreferencesKey("book_state_filter")
            val FAVORITES = booleanPreferencesKey("book_favorites_only")
            val MIN_RATING = doublePreferencesKey("book_minimum_rating")
            val FORMATS = stringSetPreferencesKey("enabled_formats")
            val FILTER_FORMATS = stringSetPreferencesKey("book_format_filter")
            val ADVANCED_SEARCH = booleanPreferencesKey("advanced_search")
            val MISSING = booleanPreferencesKey("show_missing")
            val SORT = stringPreferencesKey("book_sort")
            val GRID = booleanPreferencesKey("grid_view")
            val REVERSE_SORT = booleanPreferencesKey("book_sort_reversed")
            val GRID_SIZE = stringPreferencesKey("book_grid_size")
            val LIST_SIZE = stringPreferencesKey("book_list_size")
            val LAST_SCAN = longPreferencesKey("last_scan_at")
        }
    }
