package com.kmab.deft.feature.organization

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.dp
import com.kmab.deft.core.designsystem.DeftButton
import com.kmab.deft.core.designsystem.DeftTypography
import com.kmab.deft.core.designsystem.LocalDeftColors
import com.kmab.deft.core.designsystem.icons.DeftActionIcons
import com.kmab.deft.library.organization.CategorySummary
import kotlin.math.abs

@Composable
internal fun CategoryReorderSheet(
    category: CategorySummary,
    model: OrganizationViewModel,
    close: () -> Unit,
) {
    val books = category.books.associateBy { it.book.book.id }
    val initial = category.books.map { it.book.book.id }
    var order by rememberSaveable(category.category.id, initial) { mutableStateOf(ArrayList(initial)) }
    val list = rememberLazyListState()
    OrganizationSheet("reorder books", close, model, scrollable = false) {
        Text("Hold a handle and drag, or use the up and down buttons.", style = DeftTypography.body)
        DeftButton(
            "save order",
            { model.edit(close) { reorder(category.category.id, order) } },
            enabled = order != initial,
        )
        LazyColumn(Modifier.fillMaxWidth().weight(1f, fill = false), state = list) {
            itemsIndexed(order, key = { _, id -> id }) { index, id ->
                val book = books[id]?.book?.book ?: return@itemsIndexed
                ReorderRow(
                    book.displayTitle(),
                    index > 0,
                    index < order.lastIndex,
                    Modifier.animateItem(),
                ) { direction ->
                    val next = order.indexOf(id)
                    val target = (next + direction).coerceIn(0, order.lastIndex)
                    if (next != target) {
                        order = ArrayList(order).apply { add(target, removeAt(next)) }
                        if (list.layoutInfo.visibleItemsInfo.none { it.index == target }) {
                            list.requestScrollToItem(target)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ReorderRow(
    title: String,
    canMoveUp: Boolean,
    canMoveDown: Boolean,
    modifier: Modifier,
    move: (Int) -> Unit,
) {
    var dragging by remember { mutableStateOf(false) }
    var rowHeight by remember { mutableFloatStateOf(1f) }
    val currentMove by rememberUpdatedState(move)
    val colors = LocalDeftColors.current
    Row(
        modifier
            .heightIn(min = 48.dp)
            .onSizeChanged { rowHeight = it.height.coerceAtLeast(1).toFloat() }
            .background(if (dragging) colors.surface2 else colors.surface),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Icon(
            DeftActionIcons.List,
            "drag $title",
            Modifier.size(48.dp).pointerInput(Unit) {
                var offset = 0f
                detectDragGesturesAfterLongPress(
                    onDragStart = {
                        offset = 0f
                        dragging = true
                    },
                    onDragEnd = { dragging = false },
                    onDragCancel = { dragging = false },
                ) { change, drag ->
                    change.consume()
                    offset += drag.y
                    while (abs(offset) >= rowHeight) {
                        val direction = if (offset > 0) 1 else -1
                        currentMove(direction)
                        offset -= direction * rowHeight
                    }
                }
            },
        )
        Text(title, modifier = Modifier.weight(1f), style = DeftTypography.title)
        IconButton({ move(-1) }, enabled = canMoveUp) { Icon(DeftActionIcons.Ascending, "move $title up") }
        IconButton({ move(1) }, enabled = canMoveDown) { Icon(DeftActionIcons.Descending, "move $title down") }
    }
}
