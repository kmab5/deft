package com.kmab.deft.feature.notes

import android.content.ActivityNotFoundException
import android.content.ClipData
import android.content.Intent
import android.net.Uri
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.kmab.deft.core.designsystem.DeftNoticeKind
import com.kmab.deft.core.designsystem.LocalDeftNoticeState
import com.kmab.deft.core.model.BookFormat
import com.kmab.deft.core.model.HighlightColor
import com.kmab.deft.library.notes.HighlightDraft
import com.kmab.deft.library.notes.NoteEntry
import com.kmab.deft.library.notes.NoteFilter
import com.kmab.deft.library.notes.NoteType

/** Hosts the notes pane and exposes its unfiltered count to the book's tabs. */
@Composable
fun NotesPane(
    onOpen: (Long, String) -> Unit,
    bookId: Long? = null,
    content: @Composable (Int?, @Composable (Modifier) -> Unit) -> Unit = { _, pane -> pane(Modifier) },
) {
    val model: NotesViewModel = hiltViewModel()
    NotesScreen(model, onOpen, bookId, content)
}

@Composable
internal fun NotesScreen(
    model: NotesViewModel,
    onOpen: (Long, String) -> Unit,
    bookId: Long? = null,
    content: @Composable (Int?, @Composable (Modifier) -> Unit) -> Unit = { _, pane -> pane(Modifier) },
) {
    val notices = LocalDeftNoticeState.current
    val context = LocalContext.current
    var selectedBook by rememberSaveable(bookId) { mutableStateOf<Long?>(null) }
    var color by rememberSaveable(bookId) { mutableStateOf<String?>(null) }
    var type by rememberSaveable(bookId) { mutableStateOf<String?>(null) }
    var query by rememberSaveable(bookId) { mutableStateOf("") }
    var retry by remember { mutableIntStateOf(0) }
    val baseFilter = NoteFilter(bookId = bookId)
    val filter =
        NoteFilter(bookId ?: selectedBook, color?.let(HighlightColor::valueOf), type?.let(NoteType::valueOf), query)
    val unfiltered = observedNotes(model, baseFilter, retry)
    val filtered = if (filter == baseFilter) unfiltered else observedNotes(model, filter, retry)
    val entries = (unfiltered as? NotesList.Ready)?.entries.orEmpty()
    val editing by model.editing.collectAsStateWithLifecycle()
    val busy by model.busy.collectAsStateWithLifecycle()
    val error by model.error.collectAsStateWithLifecycle()
    val share by model.share.collectAsStateWithLifecycle()

    LaunchedEffect(error) {
        error?.let {
            notices?.show(it, DeftNoticeKind.Error)
            model.error.value = null
        }
    }
    LaunchedEffect(share) {
        val uri = share ?: return@LaunchedEffect
        model.share.value = null
        try {
            context.startActivity(markdownShareChooser(uri))
        } catch (_: ActivityNotFoundException) {
            notices?.show("No app can share these notes.", DeftNoticeKind.Error)
        } catch (_: SecurityException) {
            notices?.show("Couldn't share these notes.", DeftNoticeKind.Error)
        }
    }

    content((unfiltered as? NotesList.Ready)?.entries?.size) { modifier ->
        NotesContent(
            state = filtered,
            books = if (bookId == null) entries.distinctBy(NoteEntry::bookId) else emptyList(),
            filter = filter,
            onFilter = {
                selectedBook = it.bookId
                color = it.color?.name
                type = it.type?.name
                query = it.query
            },
            onRetry = { retry++ },
            onOpen = { entry ->
                val reason = entry.openBlockReason()
                if (reason == null) onOpen(entry.bookId, entry.locator) else notices?.show(reason)
            },
            onEdit = model::edit,
            onExport =
                bookId?.takeIf { entries.isNotEmpty() }?.let { id ->
                    {
                        if (markdownSendIntent().resolveActivity(context.packageManager) == null) {
                            notices?.show("No app can share these notes.", DeftNoticeKind.Error)
                        } else {
                            model.export(id)
                        }
                    }
                },
            busy = busy,
            showBookFilter = bookId == null,
            modifier = modifier,
        )
        entries.firstOrNull { it.key == editing }?.let { entry ->
            key(entry.key) { NotesEntryEditor(entry, model, busy) }
        }
    }
}

private fun markdownSendIntent(): Intent = Intent(Intent.ACTION_SEND).setType("text/markdown")

internal fun markdownShareChooser(uri: Uri): Intent =
    Intent.createChooser(
        markdownSendIntent().apply {
            putExtra(Intent.EXTRA_STREAM, uri)
            clipData = ClipData.newRawUri("Deft notes", uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        },
        "share notes",
    )

@Composable
private fun observedNotes(
    model: NotesViewModel,
    filter: NoteFilter,
    retry: Int,
): NotesList =
    key(filter, retry) {
        val flow = remember(model, filter, retry) { model.observe(filter) }
        val state by flow.collectAsStateWithLifecycle(NotesList.Loading)
        state
    }

internal fun NoteEntry.openBlockReason(): String? =
    when {
        drmProtected -> "This file is DRM-protected. You can still edit or share its notes."
        missing -> "Deft can't find this file. You can still edit or share its notes."
        bookFormat != BookFormat.EPUB -> "${bookFormat.displayName} reading comes in a later update."
        else -> null
    }

@Composable
private fun NotesEntryEditor(
    entry: NoteEntry,
    model: NotesViewModel,
    busy: Boolean,
) {
    val remove = { model.delete(entry) }
    if (entry.type == NoteType.BOOKMARK) {
        BookmarkEditor(entry.label.orEmpty(), { model.rename(entry, it) }, model::closeEditor, remove, busy)
    } else {
        HighlightEditor(
            HighlightDraft(
                bookId = entry.bookId,
                locator = entry.locator,
                text = entry.text,
                color = entry.color ?: HighlightColor.NONE,
                note = entry.note,
                totalProgression = entry.totalProgression,
                id = entry.id,
                underline = entry.underline,
            ),
            model::save,
            model::closeEditor,
            remove,
            busy,
        )
    }
}
