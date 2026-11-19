package com.kmab.deft.feature.library

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectVerticalDragGestures
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
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
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
    var scrollbarVisible by remember { mutableStateOf(false) }
    LaunchedEffect(scroll.scrolling) {
        if (scroll.scrolling) {
            scrollbarVisible = true
        } else {
            delay(SCROLLBAR_TIMEOUT_MS)
            scrollbarVisible = false
        }
    }
    LibraryScrollbar(
        scroll,
        scrollbarVisible,
        Modifier.align(Alignment.CenterEnd).fillMaxHeight().padding(vertical = 8.dp),
    )
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
}

@Composable
private fun LibraryScrollbar(
    scroll: LibraryScrollState,
    visible: Boolean,
    modifier: Modifier,
) {
    val color = LocalDeftColors.current.muted
    val minimumThumb = with(LocalDensity.current) { 48.dp.toPx() }
    var dragging by remember { mutableStateOf(false) }
    var trackHeight by remember { mutableStateOf(1f) }
    var target by remember { mutableStateOf<Int?>(null) }
    var pointer by remember { mutableStateOf(0f) }
    val opacity by animateFloatAsState(if (visible || dragging) 1f else 0f, label = "scrollbar fade")
    LaunchedEffect(target) {
        target?.let { if (scroll.gridView) scroll.grid.scrollToItem(it) else scroll.list.scrollToItem(it) }
    }

    fun seek(y: Float) {
        val metrics = scroll.metrics()
        val total =
            if (scroll.gridView) scroll.grid.layoutInfo.totalItemsCount else scroll.list.layoutInfo.totalItemsCount
        val thumb =
            (
                trackHeight * metrics.visible /
                    metrics.total.coerceAtLeast(
                        1,
                    )
            ).coerceIn(minimumThumb.coerceAtMost(trackHeight), trackHeight)
        val fraction = ((y - thumb / 2f) / (trackHeight - thumb).coerceAtLeast(1f)).coerceIn(0f, 1f)
        val last = ((metrics.total - metrics.visible).coerceAtLeast(0f) * metrics.columns).toInt()
        target =
            if (fraction >=
                1f
            ) {
                (total - 1).coerceAtLeast(0)
            } else {
                (fraction * last).toInt().coerceIn(0, (total - 1).coerceAtLeast(0))
            }
    }
    Canvas(
        modifier
            .width(48.dp)
            .testTag("library_scrollbar")
            .semantics { contentDescription = "drag to scroll library" }
            .onSizeChanged { trackHeight = it.height.toFloat().coerceAtLeast(1f) }
            .pointerInput(scroll) {
                detectVerticalDragGestures(
                    onDragStart = {
                        dragging = true
                        pointer = it.y
                        seek(pointer)
                    },
                    onDragEnd = { dragging = false },
                    onDragCancel = { dragging = false },
                    onVerticalDrag = { change, amount ->
                        change.consume()
                        pointer += amount
                        seek(pointer)
                    },
                )
            },
    ) {
        val metrics = scroll.metrics()
        if (metrics.total > 0 && metrics.visible < metrics.total) {
            val thumb =
                (size.height * metrics.visible / metrics.total).coerceIn(
                    48.dp.toPx().coerceAtMost(size.height),
                    size.height,
                )
            val fraction = (metrics.first / (metrics.total - metrics.visible)).coerceIn(0f, 1f)
            val width = 8.dp.toPx()
            drawRoundRect(
                color.copy(alpha = opacity),
                Offset(
                    size.width - width - 4.dp.toPx(),
                    (size.height - thumb) * fraction,
                ),
                Size(width, thumb),
                CornerRadius(width),
            )
        }
    }
}

private fun LibraryScrollState.metrics(): ScrollbarMetrics =
    if (gridView) {
        val info = grid.layoutInfo
        val first = info.visibleItemsInfo.firstOrNull()
        val height = (first?.size?.height ?: 1).coerceAtLeast(1) + info.mainAxisItemSpacing
        val columns = info.visibleItemsInfo.count { it.row == first?.row }.coerceAtLeast(1)
        ScrollbarMetrics(
            (info.totalItemsCount + columns - 1) / columns,
            grid.firstVisibleItemIndex / columns + grid.firstVisibleItemScrollOffset.toFloat() / height,
            (info.viewportEndOffset - info.viewportStartOffset).toFloat() / height,
            columns,
        )
    } else {
        val info = list.layoutInfo
        val height = (info.visibleItemsInfo.firstOrNull()?.size ?: 1).coerceAtLeast(1) + info.mainAxisItemSpacing
        ScrollbarMetrics(
            info.totalItemsCount,
            list.firstVisibleItemIndex + list.firstVisibleItemScrollOffset.toFloat() / height,
            (info.viewportEndOffset - info.viewportStartOffset).toFloat() / height,
        )
    }

private data class ScrollbarMetrics(
    val total: Int,
    val first: Float,
    val visible: Float,
    val columns: Int = 1,
)

private const val SCROLLBAR_TIMEOUT_MS = 900L
