package com.kmab.deft.feature.book

import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavType
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.kmab.deft.core.designsystem.DeftNoticeKind
import com.kmab.deft.core.designsystem.LocalDeftNoticeState

fun NavGraphBuilder.bookGraph(
    navController: NavController,
    onRead: (Long) -> Unit,
) {
    composable("book/{bookId}", arguments = listOf(navArgument("bookId") { type = NavType.LongType })) {
        val viewModel: BookDetailViewModel = hiltViewModel()
        val state by viewModel.state.collectAsStateWithLifecycle()
        val notices = LocalDeftNoticeState.current
        LaunchedEffect(state.error, state.forgotten) {
            state.error?.let { message ->
                if (notices != null) {
                    notices.show(message, DeftNoticeKind.Error)
                    viewModel.dismissError()
                }
            }
            if (state.forgotten) {
                notices?.show("Book forgotten. Its original file is unchanged.")
                navController.popBackStack()
            }
        }
        BookDetailScreen(state, { navController.popBackStack() }, {
            state.details
                ?.book
                ?.id
                ?.let(onRead)
        }, {
            state.details?.book?.let { viewModel.setIgnored(!it.ignored) }
        }, viewModel::forget)
    }
}
