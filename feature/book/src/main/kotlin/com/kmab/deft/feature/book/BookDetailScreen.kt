package com.kmab.deft.feature.book

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.kmab.deft.core.database.model.BookDetails
import com.kmab.deft.core.designsystem.DeftButton
import com.kmab.deft.core.designsystem.DeftButtonStyle
import com.kmab.deft.core.designsystem.DeftDialog
import com.kmab.deft.core.designsystem.DeftShapes
import com.kmab.deft.core.designsystem.DeftTopBar
import com.kmab.deft.core.designsystem.DeftTypography
import com.kmab.deft.core.designsystem.LocalDeftColors
import com.kmab.deft.core.designsystem.LocalDeftNoticeState
import com.kmab.deft.core.designsystem.icons.DeftActionIcons
import com.kmab.deft.core.model.BookFormat

@Composable
fun BookDetailScreen(
    state: BookDetailState,
    onBack: () -> Unit,
    onRead: () -> Unit,
    onIgnore: () -> Unit,
    onForget: () -> Unit,
    actions: BookTrackingActions = BookTrackingActions(),
    organization: BookOrganizationLinks? = null,
) {
    val colors = LocalDeftColors.current
    val notices = LocalDeftNoticeState.current
    var confirmation by rememberSaveable { mutableStateOf<String?>(null) }
    var menu by remember { mutableStateOf(false) }
    Scaffold(containerColor = colors.bg, contentColor = colors.text, topBar = {
        DeftTopBar("book", navigationIcon = {
            IconButton(onBack) { Icon(DeftActionIcons.Back, "back", tint = colors.text) }
        }, actions = {
            if (state.details != null) {
                Box {
                    IconButton({ menu = true }, enabled = !state.busy) {
                        Icon(DeftActionIcons.More, "book actions", tint = colors.muted)
                    }
                    DropdownMenu(
                        menu,
                        { menu = false },
                        containerColor = colors.surface,
                        shape = DeftShapes.card,
                        shadowElevation = 0.dp,
                        tonalElevation = 0.dp,
                        border = BorderStroke(1.dp, colors.line),
                    ) {
                        DropdownMenuItem(
                            text = { Text(if (state.details.book.ignored) "un-ignore" else "ignore") },
                            leadingIcon = { Icon(DeftActionIcons.FolderMinus, null) },
                            onClick = {
                                menu = false
                                if (state.details.book.ignored) onIgnore() else confirmation = "ignore"
                            },
                        )
                        DropdownMenuItem(
                            text = { Text("forget this book") },
                            leadingIcon = { Icon(DeftActionIcons.Trash, null) },
                            onClick = {
                                menu = false
                                confirmation = "forget"
                            },
                        )
                    }
                }
            }
        })
    }) { padding ->
        when {
            state.loading ->
                Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = colors.accent)
                }
            state.details == null ->
                Column(Modifier.padding(padding).padding(20.dp)) {
                    Text("book unavailable", style = DeftTypography.headline)
                    DeftButton("back to books", onBack, style = DeftButtonStyle.Text)
                }
            else ->
                BookDetailBody(
                    state.details,
                    Modifier.padding(padding),
                    state.busy,
                    state.tracking ?: com.kmab.deft.core.model
                        .BookTrackingSnapshot(state.details.book.id),
                    actions,
                    organization,
                ) {
                    val reason = readBlockReason(state.details)
                    if (reason == null) onRead() else notices?.show(reason)
                }
        }
    }
    when (confirmation) {
        "ignore" ->
            DeftDialog(
                "ignore this?",
                "It disappears from your library and stats. " +
                    "Its history stays, and you can bring it back in settings.",
                "ignore",
                {
                    confirmation = null
                    onIgnore()
                },
                { confirmation = null },
            )
        "forget" ->
            DeftDialog(
                "forget this book?",
                "Its position, bookmarks and history will be deleted for good. " +
                    "The original file stays in your folder and may be found again by a later scan. " +
                    "This can't be undone.",
                "forget",
                {
                    confirmation = null
                    onForget()
                },
                { confirmation = null },
                dismissLabel = "keep",
            )
    }
}

internal fun readBlockReason(details: BookDetails): String? =
    when {
        details.book.ignored -> "Un-ignore this book to read it. Its saved position and bookmarks are kept."
        details.book.drmProtected -> "This file is DRM-protected. Deft can't open it."
        details.files.none { !it.missing } -> "Deft can't find this file anymore. Its history is safe."
        details.book.format != BookFormat.EPUB -> "${details.book.format.displayName} reading comes in a later update."
        else -> null
    }
