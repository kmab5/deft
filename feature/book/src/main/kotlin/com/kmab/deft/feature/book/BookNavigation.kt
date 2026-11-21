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
import com.kmab.deft.core.designsystem.popDestination
import com.kmab.deft.core.designsystem.rememberDestinationBack

fun NavGraphBuilder.bookGraph(
    navController: NavController,
    onRead: (Long) -> Unit,
) {
    composable(
        "book/{bookId}?celebrate={celebrate}",
        arguments =
            listOf(
                navArgument("bookId") { type = NavType.LongType },
                navArgument("celebrate") {
                    type = NavType.BoolType
                    defaultValue = false
                },
            ),
    ) { entry ->
        val onBack = rememberDestinationBack(navController, entry)
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
                navController.popDestination(entry)
            }
        }
        BookDetailScreen(state, onBack, {
            state.details
                ?.book
                ?.id
                ?.let(onRead)
        }, {
            state.details?.book?.let { viewModel.setIgnored(!it.ignored) }
        }, viewModel::forget, viewModel.actions)
        BookCompletion(state, viewModel)
    }
}

@androidx.compose.runtime.Composable
private fun BookCompletion(
    state: BookDetailState,
    viewModel: BookDetailViewModel,
) {
    val details = state.details
    val snapshot = state.tracking
    if (state.celebrate && details != null && snapshot != null) {
        val attempt = snapshot.latest
        val start = attempt?.startedAt?.atZone(java.time.ZoneId.systemDefault())?.toLocalDate()
        val end = attempt?.endedAt?.atZone(java.time.ZoneId.systemDefault())?.toLocalDate()
        val days =
            if (start != null &&
                end != null
            ) {
                (
                    java.time.temporal.ChronoUnit.DAYS
                        .between(start, end) + 1
                ).coerceAtLeast(1).toInt()
            } else {
                1
            }
        com.kmab.deft.feature.celebrate.FinishCelebration(
            details.book.id,
            details.book.titleOverride ?: details.book.title,
            details.authorsDisplay,
            snapshot.activeSeconds,
            snapshot.sessionCount,
            days,
            state.celebrations,
            snapshot.assessment.rating,
            snapshot.assessment.favorite,
            viewModel.actions.rating,
            viewModel.actions.favorite,
            viewModel::dismissCelebration,
            coverPath = details.book.coverOverridePath ?: details.book.coverPath,
            busy = state.busy,
        )
    }
}
