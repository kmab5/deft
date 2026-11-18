package com.kmab.deft.feature.library

import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.kmab.deft.core.designsystem.DeftNoticeKind
import com.kmab.deft.core.designsystem.LocalDeftNoticeState

fun NavGraphBuilder.libraryGraph(
    navController: NavController,
    onScanDetails: () -> Unit = {},
) {
    composable("books") {
        val viewModel: BooksViewModel = hiltViewModel()
        val state by viewModel.uiState.collectAsStateWithLifecycle()
        val notices = LocalDeftNoticeState.current
        LaunchedEffect(state.error, notices) {
            state.error?.let { message ->
                if (notices != null) {
                    notices.show(message, kind = DeftNoticeKind.Error)
                    viewModel.dismissError()
                }
            }
        }
        BooksScreen(
            state,
            { navController.navigate("settings") },
            { navController.navigate("settings/library") },
            viewModel::rescan,
            viewModel::setGridView,
            viewModel::setSort,
            viewModel::setShowMissing,
            viewModel::setFormat,
            viewModel::setReverseSort,
            viewModel::setGridSize,
            viewModel::setListSize,
            onScanDetails,
        )
    }
}
