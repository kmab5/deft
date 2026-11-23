package com.kmab.deft.feature.organization

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.kmab.deft.core.database.entity.CategoryTypeEntity
import com.kmab.deft.core.datastore.LibrarySettings
import com.kmab.deft.core.designsystem.DeftTypography
import com.kmab.deft.core.designsystem.LocalDeftColors
import com.kmab.deft.core.designsystem.icons.DeftActionIcons
import com.kmab.deft.library.organization.CategorySummary

@Composable
internal fun CategoriesScreen(
    state: OrganizationUiState,
    model: OrganizationViewModel,
    open: (Long) -> Unit,
) {
    val snapshot =
        state.snapshot ?: return OrganizationEmpty(
            if (state.loadFailed) "Couldn't load categories." else "loading",
            !state.loadFailed,
        )
    var create by rememberSaveable { mutableStateOf(false) }
    var manageTypes by rememberSaveable { mutableStateOf(false) }
    var query by rememberSaveable { mutableStateOf("") }
    var selected by rememberSaveable { mutableStateOf(emptyList<Long>()) }
    var action by rememberSaveable { mutableStateOf<CategoryBulkAction?>(null) }
    var actionIds by rememberSaveable { mutableStateOf(emptyList<Long>()) }
    val busy by model.busy.collectAsState()
    val completion by model.categorySelectionCompletion.collectAsState()
    var handledCompletion by rememberSaveable { mutableStateOf(completion) }
    LaunchedEffect(completion) {
        if (handledCompletion != completion) {
            selected = emptyList()
            action = null
            handledCompletion = completion
        }
    }
    val prefs = state.preferences
    val visibleFilter = visibleCategoryFilter(prefs, snapshot.types)
    val categories = browseCategories(snapshot.categories, prefs.copy(categoryTypeFilter = visibleFilter), query)
    val visibleIds = categories.map { it.category.id }.toSet()
    LaunchedEffect(visibleIds, busy) {
        if (!busy) selected = selected.filter { it in visibleIds }
    }
    BackHandler(selected.isNotEmpty() && action == null) { if (!busy) selected = emptyList() }
    val toggle: (Long) -> Unit = { id ->
        selected = toggledCategoryIds(selected, id, busy)
    }
    Column {
        AnimatedVisibility(selected.isNotEmpty()) {
            CategorySelectionBar(selected.size, busy, { selected = emptyList() }, { selected = visibleIds.toList() }) {
                actionIds = selected.toList()
                action = it
            }
        }
        BoxWithConstraints(Modifier.padding(horizontal = 16.dp)) {
            val columns = categoryColumns(prefs.categoryGridView, maxWidth, LocalDensity.current.fontScale)
            val groups =
                groupCategories(
                    categories,
                    snapshot.types,
                    prefs.groupCategoriesByType && visibleFilter == null,
                )
            LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                item {
                    CategoryListControls(
                        prefs,
                        snapshot.types,
                        visibleFilter,
                        model,
                        { create = true },
                        { manageTypes = true },
                        query,
                        { query = it },
                    )
                }
                if (categories.isEmpty()) item { OrganizationEmpty("No categories here yet.") }
                categoryRows(groups, columns) { category ->
                    val id = category.category.id
                    CategoryCard(
                        category,
                        prefs.categoryGridView,
                        Modifier.weight(1f),
                        id in selected,
                        enabled = !busy,
                        onLongClick = { toggle(id) },
                    ) {
                        if (selected.isEmpty()) open(id) else toggle(id)
                    }
                }
            }
        }
    }
    action?.let {
        CategoryBulkActions(it, actionIds.toSet(), snapshot, model, { action = null }, model::completeCategorySelection)
    }
    if (create) CategoryEditor(null, snapshot.types, model, { create = false })
    if (manageTypes) CategoryTypes(snapshot.types, model) { manageTypes = false }
}

private fun visibleCategoryFilter(
    preferences: LibrarySettings,
    types: List<CategoryTypeEntity>,
): Long? = preferences.categoryTypeFilter?.takeIf { id -> id == 0L || types.any { it.id == id } }

private fun toggledCategoryIds(
    selected: List<Long>,
    id: Long,
    busy: Boolean,
): List<Long> =
    when {
        busy -> selected
        id in selected -> selected - id
        else -> selected + id
    }

private fun categoryColumns(
    grid: Boolean,
    width: Dp,
    fontScale: Float,
): Int =
    when {
        !grid || width / fontScale < 300.dp -> 1
        width >= 620.dp -> WIDE_GRID_COLUMNS
        else -> 2
    }

private fun groupCategories(
    categories: List<CategorySummary>,
    types: List<CategoryTypeEntity>,
    grouped: Boolean,
): List<Pair<String?, List<CategorySummary>>> =
    if (grouped) {
        types.map { type -> type.name to categories.filter { category -> category.types.any { it.id == type.id } } } +
            ("untyped" to categories.filter { it.types.isEmpty() })
    } else {
        listOf(null to categories)
    }

@Composable
private fun CategoryListControls(
    preferences: LibrarySettings,
    types: List<CategoryTypeEntity>,
    filter: Long?,
    model: OrganizationViewModel,
    create: () -> Unit,
    manageTypes: () -> Unit,
    query: String,
    onQuery: (String) -> Unit,
) {
    var filters by rememberSaveable { mutableStateOf(false) }
    Column(Modifier.padding(vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(Modifier.fillMaxWidth().height(IntrinsicSize.Min), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OrganizationButton(
                "new category",
                create,
                Modifier.weight(1f).fillMaxHeight(),
                DeftActionIcons.Added,
                primary = true,
            )
            OrganizationButton("types", manageTypes, Modifier.fillMaxHeight(), icon = DeftActionIcons.Folder)
        }
        HorizontalDivider(Modifier.padding(vertical = 2.dp), color = LocalDeftColors.current.line)
        BrowseSearch(query, onQuery, "find categories")
        val active =
            listOf(
                filter != null,
                preferences.categoryReview != "all",
                preferences.categoryState != null,
                preferences.groupCategoriesByType,
            ).count { it }
        BrowseSort(
            preferences.categorySort,
            preferences.categoryReverse,
            categorySortOptions,
            { model.preference { setCategorySort(it) } },
            { model.preference { toggleCategoryReverse() } },
            active,
            { filters = true },
        ) {
            DisplayAction(preferences.categoryGridView, "category") {
                model.preference { setCategoryGridView(!preferences.categoryGridView) }
            }
        }
    }
    if (filters) CategoryFilters(preferences, types, filter, model) { filters = false }
}

private fun LazyListScope.categoryRows(
    groups: List<Pair<String?, List<CategorySummary>>>,
    columns: Int,
    card: @Composable RowScope.(CategorySummary) -> Unit,
) {
    groups.filter { it.second.isNotEmpty() }.forEach { (label, members) ->
        if (label != null) {
            item { Text(label, style = DeftTypography.title, color = LocalDeftColors.current.muted) }
        }
        items(members.chunked(columns)) { row ->
            Row(Modifier.fillMaxWidth().animateItem(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                row.forEach { category ->
                    card(category)
                }
                repeat(columns - row.size) { Column(Modifier.weight(1f)) {} }
            }
        }
    }
}

private const val WIDE_GRID_COLUMNS = 3

@Composable
internal fun CategoryCard(
    category: CategorySummary,
    grid: Boolean,
    modifier: Modifier = Modifier,
    selected: Boolean = false,
    enabled: Boolean = true,
    onLongClick: (() -> Unit)? = null,
    open: () -> Unit,
) {
    Box(modifier) {
        OrganizationPanel(
            Modifier
                .testTag("category_${category.category.id}")
                .semantics { this.selected = selected }
                .combinedClickable(
                    enabled = enabled,
                    onClick = open,
                    onLongClick = onLongClick,
                    onLongClickLabel = "select category",
                ),
            selected = selected,
        ) {
            if (grid) {
                CategoryCover(category, Modifier.fillMaxWidth().aspectRatio(CATEGORY_COVER_RATIO))
                CategoryCardInfo(category)
            } else {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    CategoryCover(category, Modifier.size(74.dp))
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        CategoryCardInfo(category)
                    }
                }
            }
        }
        if (selected) {
            Icon(
                DeftActionIcons.Check,
                null,
                Modifier.align(Alignment.TopEnd).padding(8.dp).size(20.dp),
                tint = LocalDeftColors.current.accentText,
            )
        }
    }
}

@Composable
private fun CategoryCardInfo(category: CategorySummary) {
    Text(category.category.name, style = DeftTypography.title, maxLines = 2, overflow = TextOverflow.Ellipsis)
    Text(category.types.joinToString(" · ") { it.name }.ifEmpty { "untyped" }, style = DeftTypography.mono)
    OrganizationProgress(category.stats, legend = false)
    val rating = ratingLabel(category.rating)
    Text(
        "${category.stats.finished}/${category.stats.total} finished · ★ $rating" +
            if (category.category.ratingOverride != null) " · yours" else "",
        style = DeftTypography.mono,
    )
    if (category.needsReview) Text("● review", style = DeftTypography.mono, color = LocalDeftColors.current.pausedText)
}

private const val CATEGORY_COVER_RATIO = 0.9f
