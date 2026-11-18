package com.kmab.deft.core.designsystem

import android.view.WindowManager
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.selection.selectable
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.window.DialogWindowProvider
import com.kmab.deft.core.designsystem.icons.DeftIcons

@Composable
fun DeftTopBar(
    title: String,
    modifier: Modifier = Modifier,
    scrolled: Boolean = false,
    navigationIcon: (@Composable () -> Unit)? = null,
    actions: @Composable RowScope.() -> Unit = {},
) {
    val c = LocalDeftColors.current
    Column(modifier.background(c.bg)) {
        Row(
            Modifier.fillMaxWidth().heightIn(min = 56.dp).padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            navigationIcon?.invoke()
            Text(title, Modifier.weight(1f), color = c.text, style = DeftTypography.headline)
            actions()
        }
        if (scrolled) HorizontalDivider(color = c.line, thickness = 1.dp)
    }
}

@Composable
fun DeftBottomBar(
    selected: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val c = LocalDeftColors.current
    val labels = listOf("books", "categories", "authors", "notes", "stats")
    val icons = listOf(DeftIcons.Books, DeftIcons.Categories, DeftIcons.Authors, DeftIcons.Notes, DeftIcons.Stats)
    BoxWithConstraints(modifier.fillMaxWidth().background(c.surface)) {
        val itemWidth = maxWidth / labels.size
        val offset by animateDpAsState(
            itemWidth * selected.coerceIn(labels.indices),
            spring(dampingRatio = DeftMotion.DAMPING_RATIO, stiffness = DeftMotion.stiffness),
        )
        HorizontalDivider(color = c.line, thickness = 1.dp)
        Box(
            Modifier
                .offset(x = offset)
                .width(itemWidth)
                .height(2.dp)
                .background(c.accent),
        )
        Row(Modifier.fillMaxWidth().heightIn(min = 64.dp)) {
            labels.forEachIndexed { index, label ->
                val active = selected == index
                Column(
                    Modifier
                        .weight(1f)
                        .heightIn(min = 64.dp)
                        .selectable(active, role = Role.Tab, onClick = {
                            onSelect(index)
                        })
                        .padding(
                            top = 10.dp,
                            bottom = 8.dp,
                        ),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Icon(icons[index], null, Modifier.size(24.dp), tint = if (active) c.accentText else c.muted)
                    Text(label, style = DeftTypography.monoSmall, color = if (active) c.accentText else c.muted)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DeftSheet(
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    val c = LocalDeftColors.current
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        modifier = modifier,
        shape = DeftShapes.sheet,
        containerColor = c.surface,
        contentColor = c.text,
        scrimColor = c.scrim,
        tonalElevation =
            0
                .dp,
        dragHandle = {
            Box(Modifier.padding(top = 12.dp).size(32.dp, 4.dp).background(c.line2, DeftShapes.cover))
        },
    ) { Column(Modifier.fillMaxWidth().padding(24.dp), content = content) }
}

private const val DIALOG_START_SCALE = 0.96f

@Composable
fun DeftDialog(
    title: String,
    body: String,
    confirmLabel: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    dismissLabel: String = "cancel",
    modifier: Modifier = Modifier,
) {
    val c = LocalDeftColors.current
    val visibility = remember { MutableTransitionState(false).apply { targetState = true } }
    var afterExit by remember { mutableStateOf<(() -> Unit)?>(null) }
    val close: (() -> Unit) -> Unit = { action ->
        if (afterExit == null) {
            afterExit = action
            visibility.targetState = false
        }
    }
    LaunchedEffect(visibility.isIdle, visibility.currentState, afterExit) {
        if (visibility.isIdle && !visibility.currentState) afterExit?.invoke()
    }
    Dialog(
        onDismissRequest = { close(onDismiss) },
        properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false),
    ) {
        val window = (LocalView.current.parent as? DialogWindowProvider)?.window
        SideEffect { window?.clearFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND) }
        AnimatedVisibility(
            visibleState = visibility,
            enter =
                fadeIn(tween(DeftMotion.SHORT)) + scaleIn(tween(DeftMotion.MEDIUM), initialScale = DIALOG_START_SCALE),
            exit =
                fadeOut(tween(DeftMotion.SHORT)) + scaleOut(tween(DeftMotion.SHORT), targetScale = DIALOG_START_SCALE),
        ) {
            Box(
                Modifier
                    .fillMaxSize()
                    .background(c.scrim)
                    .pointerInput(onDismiss) {
                        detectTapGestures { close(onDismiss) }
                    }.padding(24.dp),
                contentAlignment = Alignment.Center,
            ) {
                Column(
                    modifier
                        .widthIn(max = 560.dp)
                        .fillMaxWidth()
                        .clip(DeftShapes.card)
                        .background(c.surface)
                        .pointerInput(Unit) {
                            detectTapGestures {}
                        }.padding(24.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    Text(title, style = DeftTypography.headline, color = c.text)
                    Text(body, style = DeftTypography.body, color = c.text)
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                        DeftButton(dismissLabel, { close(onDismiss) }, style = DeftButtonStyle.Text)
                        DeftButton(confirmLabel, { close(onConfirm) })
                    }
                }
            }
        }
    }
}

@Composable
fun DeftSnackbar(
    message: String,
    modifier: Modifier = Modifier,
    action: String? = null,
    onAction: () -> Unit = {},
) {
    val c = LocalDeftColors.current
    Row(
        modifier.fillMaxWidth().background(c.surface2, DeftShapes.small).padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(message, Modifier.weight(1f), color = c.text, style = DeftTypography.body)
        if (action != null) DeftButton(action, onAction, style = DeftButtonStyle.Text)
    }
}
