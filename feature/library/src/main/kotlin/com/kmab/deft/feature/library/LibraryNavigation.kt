package com.kmab.deft.feature.library

import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable

fun NavGraphBuilder.libraryGraph(navController: NavController) {
    composable("books") {
        val viewModel: BooksViewModel = hiltViewModel()
        val state by viewModel.uiState.collectAsStateWithLifecycle()
        BooksScreen(
            state,
            { navController.navigate("settings") },
            { navController.navigate("settings/library") },
            viewModel::rescan,
            viewModel::setGridView,
            viewModel::setSort,
            viewModel::setShowMissing,
            viewModel::setFormat,
        )
    }
}
