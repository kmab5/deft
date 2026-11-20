package com.kmab.deft.core.designsystem

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalAccessibilityManager
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.kmab.deft.core.designsystem.icons.DeftActionIcons
import kotlinx.coroutines.delay

private const val VISIBLE_NOTICES = 3
private const val NOTICE_TIMEOUT = 6_000L
private const val NOTICE_SLIDE_DP = 32f

enum class DeftNoticeKind { Info, Success, Error }

internal data class DeftNotice(
    val id: Long,
    val message: String,
    val kind: DeftNoticeKind,
    val actionLabel: String?,
    val onAction: (() -> Unit)?,
)

/** App-owned queue. Hidden notices get their full reading time when they become visible. */
@Stable
class DeftNoticeState {
    private var nextId = 0L
    private val queue = mutableStateListOf<DeftNotice>()
    internal val visible: List<DeftNotice> get() = queue.take(VISIBLE_NOTICES)

    fun show(
        message: String,
        kind: DeftNoticeKind = DeftNoticeKind.Info,
        actionLabel: String? = null,
        onAction: (() -> Unit)? = null,
    ): Long {
        // Errors with identical corrective text need only one place in the stack.
        if (onAction == null) {
            queue.firstOrNull { it.message == message && it.kind == kind && it.onAction == null }?.let { return it.id }
        }
        val notice = DeftNotice(++nextId, message, kind, actionLabel, onAction)
        queue += notice
        return notice.id
    }

    fun dismiss(id: Long) {
        queue.removeAll { it.id == id }
    }
}

val LocalDeftNoticeState = staticCompositionLocalOf<DeftNoticeState?> { null }

@Composable
fun rememberDeftNoticeState(): DeftNoticeState = remember { DeftNoticeState() }

@Composable
fun DeftNoticeHost(
    state: DeftNoticeState,
    modifier: Modifier = Modifier,
    paused: Boolean = false,
) {
    LazyColumn(
        modifier.fillMaxWidth().heightIn(max = 420.dp).padding(horizontal = 12.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        reverseLayout = true,
    ) {
        items(state.visible, key = { it.id }) { notice ->
            NoticeCard(
                notice,
                state,
                paused,
                Modifier.animateItem(
                    fadeInSpec = tween(DeftMotion.MEDIUM),
                    placementSpec = tween(DeftMotion.MEDIUM),
                    fadeOutSpec = tween(DeftMotion.SHORT),
                ),
            )
        }
    }
}

@Composable
private fun NoticeCard(
    notice: DeftNotice,
    state: DeftNoticeState,
    paused: Boolean,
    modifier: Modifier,
) {
    val colors = LocalDeftColors.current
    var entered by remember { mutableStateOf(false) }
    val slide by animateFloatAsState(
        if (entered) 0f else NOTICE_SLIDE_DP,
        tween(DeftMotion.MEDIUM),
        label = "notice entrance",
    )
    LaunchedEffect(Unit) { entered = true }
    var pressed by remember { mutableStateOf(false) }
    var focused by remember { mutableStateOf(false) }
    val timeout =
        LocalAccessibilityManager.current?.calculateRecommendedTimeoutMillis(
            NOTICE_TIMEOUT,
            containsIcons = true,
            containsText = true,
            containsControls = true,
        ) ?: NOTICE_TIMEOUT
    LaunchedEffect(notice.id, pressed, focused, paused, timeout) {
        if (!pressed && !focused && !paused) {
            delay(timeout)
            state.dismiss(notice.id)
        }
    }
    Surface(
        modifier
            .fillMaxWidth()
            .graphicsLayer { translationY = slide.dp.toPx() }
            .semantics { liveRegion = LiveRegionMode.Polite }
            .onFocusChanged { focused = it.hasFocus }
            .focusGroup()
            .pointerInput(notice.id) {
                awaitEachGesture {
                    awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial)
                    pressed = true
                    try {
                        waitForUpOrCancellation(pass = PointerEventPass.Initial)
                    } finally {
                        pressed = false
                    }
                }
            },
        color = colors.surface2,
        shape = DeftShapes.card,
        border = BorderStroke(1.dp, colors.line2),
        shadowElevation = 6.dp,
    ) {
        Column(Modifier.padding(start = 16.dp, top = 8.dp, bottom = 8.dp, end = 8.dp)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
                Icon(
                    when (notice.kind) {
                        DeftNoticeKind.Error -> DeftActionIcons.Warning
                        DeftNoticeKind.Success -> DeftActionIcons.Check
                        DeftNoticeKind.Info -> DeftActionIcons.Info
                    },
                    null,
                    Modifier.padding(top = 12.dp).size(20.dp),
                    tint =
                        when (notice.kind) {
                            DeftNoticeKind.Error -> colors.error
                            DeftNoticeKind.Success -> colors.finishedText
                            DeftNoticeKind.Info -> colors.accentText
                        },
                )
                Column(Modifier.weight(1f).padding(start = 12.dp, end = 8.dp, top = 10.dp)) {
                    Text(notice.message, style = DeftTypography.body, color = colors.text)
                    if (notice.actionLabel != null && notice.onAction != null) {
                        TextButton(
                            onClick = {
                                state.dismiss(notice.id)
                                notice.onAction.invoke()
                            },
                            modifier = Modifier.heightIn(min = 48.dp),
                            contentPadding = PaddingValues(vertical = 8.dp),
                            shape = DeftShapes.small,
                        ) {
                            Text(notice.actionLabel, style = DeftTypography.label, color = colors.accentText)
                        }
                    }
                }
                IconButton(onClick = { state.dismiss(notice.id) }, modifier = Modifier.size(48.dp)) {
                    Icon(DeftActionIcons.Close, "dismiss notice", tint = colors.muted)
                }
            }
        }
    }
}
