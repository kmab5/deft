package com.kmab.deft.feature.organization

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavType
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.kmab.deft.core.designsystem.DeftNoticeKind
import com.kmab.deft.core.designsystem.DeftScreenMenu
import com.kmab.deft.core.designsystem.DeftTopBar
import com.kmab.deft.core.designsystem.LocalDeftColors
import com.kmab.deft.core.designsystem.LocalDeftNoticeState
import com.kmab.deft.core.designsystem.icons.DeftActionIcons
import com.kmab.deft.core.designsystem.rememberDestinationBack

fun NavGraphBuilder.organizationGraph(
    navController: NavController,
    onScanDetails: () -> Unit = {
    },
    hasScanDetails: Boolean = false,
) {
    val openSettings = { navController.navigate("settings") }
    val openBook: (Long) -> Unit = { navController.navigate("book/$it") }
    composable("categories") {
        OrganizationPage("categories", null, openSettings, onScanDetails.takeIf { hasScanDetails }) { state, model ->
            CategoriesScreen(state, model) { navController.navigate("category/$it") }
        }
    }
    composable("authors") {
        OrganizationPage("authors", null, openSettings, onScanDetails.takeIf { hasScanDetails }) { state, model ->
            AuthorsScreen(state, model) { navController.navigate("author/$it") }
        }
    }
    composable("category/{categoryId}", listOf(navArgument("categoryId") { type = NavType.LongType })) { entry ->
        val close = rememberDestinationBack(navController, entry)
        val id = entry.arguments?.getLong("categoryId")
        OrganizationPage("category", close, null) { state, model ->
            val category = state.snapshot?.categories?.firstOrNull { it.category.id == id }
            if (category != null) {
                CategoryDetail(category, requireNotNull(state.snapshot), model, close, openBook)
            } else {
                OrganizationUnavailable(state, "category")
            }
        }
    }
    composable("author/{authorId}", listOf(navArgument("authorId") { type = NavType.LongType })) { entry ->
        val close = rememberDestinationBack(navController, entry)
        val id = entry.arguments?.getLong("authorId")
        OrganizationPage("author", close, null) { state, model ->
            val authors = state.snapshot?.authors.orEmpty()
            val author = authors.firstOrNull { it.author.id == id }
            if (author != null) {
                AuthorDetail(author, authors, model, close, openBook)
            } else {
                OrganizationUnavailable(state, "author")
            }
        }
    }
}

@Composable
private fun OrganizationUnavailable(
    state: OrganizationUiState,
    noun: String,
) {
    val loading = state.snapshot == null && !state.loadFailed
    OrganizationEmpty(if (loading) "loading" else "This $noun is unavailable.", loading)
}

@Composable
private fun OrganizationPage(
    title: String,
    back: (() -> Unit)?,
    settings: (() -> Unit)?,
    scanDetails: (() -> Unit)? = null,
    content: @Composable (OrganizationUiState, OrganizationViewModel) -> Unit,
) {
    val model: OrganizationViewModel = hiltViewModel()
    val state by model.state.collectAsStateWithLifecycle()
    val error by model.error.collectAsStateWithLifecycle()
    val sheetActive by model.sheetActive.collectAsStateWithLifecycle()
    val notices = LocalDeftNoticeState.current
    LaunchedEffect(error, sheetActive) {
        if (!sheetActive) {
            error?.let {
                notices?.show(it, DeftNoticeKind.Error)
                model.error.value = null
            }
        }
    }
    val colors = LocalDeftColors.current
    val imageLoader = rememberOrganizationImageLoader()
    Scaffold(containerColor = colors.bg, contentColor = colors.text, topBar = {
        DeftTopBar(
            title,
            navigationIcon =
                if (back == null) {
                    null
                } else {
                    { IconButton(back) { Icon(DeftActionIcons.Back, "back") } }
                },
            actions = { if (settings != null) DeftScreenMenu(settings, scanDetails) },
        )
    }) { insets ->
        CompositionLocalProvider(LocalOrganizationImageLoader provides imageLoader) {
            Box(Modifier.fillMaxSize().padding(insets)) { content(state, model) }
        }
    }
}
