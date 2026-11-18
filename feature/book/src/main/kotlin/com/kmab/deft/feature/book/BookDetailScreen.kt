package com.kmab.deft.feature.book

import android.text.Html
import android.text.format.Formatter
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import coil3.ImageLoader
import coil3.compose.AsyncImage
import com.kmab.deft.core.database.model.BookDetails
import com.kmab.deft.core.designsystem.CoverImage
import com.kmab.deft.core.designsystem.DeftButton
import com.kmab.deft.core.designsystem.DeftButtonStyle
import com.kmab.deft.core.designsystem.DeftDialog
import com.kmab.deft.core.designsystem.DeftShapes
import com.kmab.deft.core.designsystem.DeftTopBar
import com.kmab.deft.core.designsystem.DeftTypography
import com.kmab.deft.core.designsystem.GeneratedCover
import com.kmab.deft.core.designsystem.LocalDeftColors
import com.kmab.deft.core.designsystem.LocalDeftNoticeState
import com.kmab.deft.core.designsystem.icons.DeftActionIcons
import com.kmab.deft.core.designsystem.icons.DeftIcons
import com.kmab.deft.core.model.BookFormat
import java.io.File
import kotlin.math.roundToInt

@Composable
fun BookDetailScreen(
    state: BookDetailState,
    onBack: () -> Unit,
    onRead: () -> Unit,
    onIgnore: () -> Unit,
    onForget: () -> Unit,
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
                BookDetailBody(state.details, Modifier.padding(padding), state.busy) {
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

@Composable
private fun BookDetailBody(
    details: BookDetails,
    modifier: Modifier,
    busy: Boolean,
    onRead: () -> Unit,
) {
    val colors = LocalDeftColors.current
    val book = details.book
    val title = book.titleOverride ?: book.title
    val context = LocalContext.current
    Column(
        modifier
            .fillMaxSize()
            .testTag("book_detail")
            .verticalScroll(rememberScrollState())
            .padding(20.dp)
            .animateContentSize(),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        DetailCover(details, Modifier.width(144.dp).align(Alignment.CenterHorizontally))
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(title, style = DeftTypography.headline)
            if (details.authorsDisplay.isNotBlank()) {
                Text(
                    details.authorsDisplay,
                    style = DeftTypography.body,
                    color = colors.muted,
                )
            }
        }
        val badges =
            listOfNotNull(
                "ignored".takeIf { book.ignored },
                "protected".takeIf { book.drmProtected },
                "file missing".takeIf { details.files.none { !it.missing } },
            )
        if (badges.isNotEmpty()) Text(badges.joinToString(" · "), style = DeftTypography.label, color = colors.muted)
        DeftButton(
            if (book.currentLocator == null) "read" else "continue",
            onRead,
            enabled = !busy,
            icon = DeftIcons.Books,
        )
        if (book.currentLocator != null) {
            val progress by animateFloatAsState(
                book.totalProgression.coerceIn(0.0, 1.0).toFloat(),
                label = "saved book progress",
            )
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier.fillMaxWidth(),
                color = colors.accent,
                trackColor = colors.surface2,
                gapSize = 0.dp,
                drawStopIndicator = {},
            )
            Text("${(progress * PERCENT_SCALE).roundToInt()}%", style = DeftTypography.mono, color = colors.muted)
        }
        book.description?.takeIf { it.isNotBlank() }?.let { description ->
            val plain =
                remember(description) { Html.fromHtml(description, Html.FROM_HTML_MODE_LEGACY).toString().trim() }
            Text(plain, style = DeftTypography.body)
        }
        listOfNotNull(book.publisher, book.publishedDate, book.language, book.isbn13 ?: book.isbn10)
            .filter { it.isNotBlank() }
            .forEach { Text(it, style = DeftTypography.body, color = colors.muted) }
        HorizontalDivider(color = colors.line)
        Text("files · ${book.format.displayName.lowercase()}", style = DeftTypography.title)
        details.files.forEach { file ->
            Column(
                Modifier.fillMaxWidth().background(colors.surface, DeftShapes.card).padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Text(file.displayPath, style = DeftTypography.body)
                Text(
                    listOfNotNull(
                        if (file.size >= 0) Formatter.formatShortFileSize(context, file.size) else null,
                        "missing".takeIf { file.missing },
                    ).joinToString(" · "),
                    style = DeftTypography.mono,
                    color = colors.muted,
                )
            }
        }
        Text(
            "Original files are read-only. Deft keeps your position and bookmarks separately.",
            style = DeftTypography.body,
            color = colors.muted,
        )
    }
}

@Composable
private fun DetailCover(
    details: BookDetails,
    modifier: Modifier,
) {
    val context = LocalContext.current.applicationContext
    val loader = remember(context) { ImageLoader.Builder(context).diskCache(null).build() }
    DisposableEffect(loader) { onDispose { loader.shutdown() } }
    val book = details.book
    val title = book.titleOverride ?: book.title
    val path = book.coverOverridePath ?: book.coverPath
    val cover =
        remember(path) {
            path
                ?.let { value -> runCatching { File(value).canonicalFile }.getOrNull() }
                ?.takeIf { it.isAbsolute && it.toPath().startsWith(context.filesDir.canonicalFile.toPath()) }
        }
    CoverImage(title, details.authorsDisplay, modifier) {
        GeneratedCover(title, details.authorsDisplay)
        if (cover != null) {
            AsyncImage(
                model = cover,
                contentDescription = null,
                imageLoader = loader,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
            )
        }
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

private const val PERCENT_SCALE = 100
