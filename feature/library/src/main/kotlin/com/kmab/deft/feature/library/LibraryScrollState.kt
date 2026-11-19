package com.kmab.deft.feature.library

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.kmab.deft.core.database.model.BookListItem
import com.kmab.deft.core.designsystem.DeftShapes
import com.kmab.deft.core.designsystem.DeftTypography
import com.kmab.deft.core.designsystem.LocalDeftColors
import com.kmab.deft.core.designsystem.icons.DeftIcons
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Stable
internal class LibraryScrollState(
    val list: LazyListState,
    val grid: LazyGridState,
    val gridView: Boolean,
) {
    private var upward by mutableStateOf(true)
    val canScrollBackward: Boolean get() = if (gridView) grid.canScrollBackward else list.canScrollBackward
    val scrolling: Boolean get() = if (gridView) grid.isScrollInProgress else list.isScrollInProgress
    val showHeader: Boolean get() = upward || !canScrollBackward
    val showTop: Boolean get() = upward && canScrollBackward
    val connection =
        object : NestedScrollConnection {
            override fun onPreScroll(
                available: Offset,
                source: NestedScrollSource,
            ): Offset {
                if (source == NestedScrollSource.UserInput && available.y != 0f) upward = available.y > 0f
                return Offset.Zero
            }
        }

    suspend fun scrollToTop() {
        upward = true
        if (gridView) grid.animateScrollToItem(0) else list.animateScrollToItem(0)
    }
}

@Composable
internal fun rememberLibraryScrollState(gridView: Boolean): LibraryScrollState {
    val list = rememberLazyListState()
    val grid = rememberLazyGridState()
    return remember(list, grid, gridView) { LibraryScrollState(list, grid, gridView) }
}

@Composable
internal fun BoxScope.LibraryScrollOverlay(
    scroll: LibraryScrollState,
    continueReading: BookListItem?,
    onContinue: (Long) -> Unit,
) {
    val colors = LocalDeftColors.current
    val scope = rememberCoroutineScope()
    Column(
        Modifier.align(Alignment.BottomEnd).padding(16.dp),
        horizontalAlignment = Alignment.End,
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        AnimatedVisibility(scroll.showTop, enter = fadeIn(), exit = fadeOut()) {
            FloatingActionButton(
                onClick = { scope.launch { scroll.scrollToTop() } },
                shape = DeftShapes.card,
                containerColor = colors.surface2,
                contentColor = colors.text,
            ) {
                Icon(LibraryIcons.ToTop, "scroll to top")
            }
        }
        if (continueReading != null) {
            ExtendedFloatingActionButton(
                onClick = { onContinue(continueReading.id) },
                icon = { Icon(DeftIcons.Books, null) },
                text = { Text("continue", style = DeftTypography.label) },
                shape = DeftShapes.card,
                containerColor = colors.accent,
                contentColor = colors.onAccent,
                modifier = Modifier.testTag("floating_continue"),
            )
        }
    }
    var scrollbarVisible by remember { mutableStateOf(false) }
    LaunchedEffect(scroll.scrolling) {
        if (scroll.scrolling) {
            scrollbarVisible = true
        } else {
            delay(SCROLLBAR_TIMEOUT_MS)
            scrollbarVisible = false
        }
    }
    AnimatedVisibility(
        scrollbarVisible,
        modifier = Modifier.align(Alignment.CenterEnd).fillMaxHeight().padding(vertical = 8.dp, horizontal = 3.dp),
        enter = fadeIn(),
        exit = fadeOut(),
    ) {
        LibraryScrollbar(scroll)
    }
}

@Composable
private fun LibraryScrollbar(scroll: LibraryScrollState) {
    val color = LocalDeftColors.current.muted
    Canvas(Modifier.fillMaxHeight().width(3.dp).testTag("library_scrollbar")) {
        val metrics =
            if (scroll.gridView) {
                val info = scroll.grid.layoutInfo
                val visible = info.visibleItemsInfo
                val first = visible.firstOrNull()
                val itemHeight = first?.size?.height ?: 1
                val columns = visible.count { it.row == first?.row }.coerceAtLeast(1)
                val offset = scroll.grid.firstVisibleItemScrollOffset.toFloat() / itemHeight.coerceAtLeast(1)
                ScrollbarMetrics(
                    (info.totalItemsCount + columns - 1) / columns,
                    scroll.grid.firstVisibleItemIndex / columns + offset,
                    (info.viewportEndOffset - info.viewportStartOffset).toFloat() / itemHeight.coerceAtLeast(1),
                )
            } else {
                val info = scroll.list.layoutInfo
                val height =
                    info.visibleItemsInfo
                        .firstOrNull()
                        ?.size
                        ?.coerceAtLeast(1) ?: 1
                ScrollbarMetrics(
                    info.totalItemsCount,
                    scroll.list.firstVisibleItemIndex + scroll.list.firstVisibleItemScrollOffset.toFloat() / height,
                    (info.viewportEndOffset - info.viewportStartOffset).toFloat() / height,
                )
            }
        if (metrics.total > 0 && metrics.visible < metrics.total) {
            val thumb =
                (size.height * metrics.visible / metrics.total)
                    .coerceAtLeast(
                        24.dp.toPx(),
                    ).coerceAtMost(size.height)
            val fraction = (metrics.first / (metrics.total - metrics.visible)).coerceIn(0f, 1f)
            drawRoundRect(
                color,
                Offset(0f, (size.height - thumb) * fraction),
                Size(size.width, thumb),
                CornerRadius(size.width),
            )
        }
    }
}

private data class ScrollbarMetrics(
    val total: Int,
    val first: Float,
    val visible: Float,
)

private const val SCROLLBAR_TIMEOUT_MS = 900L
