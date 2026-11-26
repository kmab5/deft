package com.kmab.deft.feature.notes

import android.content.Context
import android.net.Uri
import androidx.core.content.FileProvider
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kmab.deft.library.notes.HighlightDraft
import com.kmab.deft.library.notes.NoteEntry
import com.kmab.deft.library.notes.NoteFilter
import com.kmab.deft.library.notes.NoteType
import com.kmab.deft.library.notes.NotesRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.util.concurrent.TimeUnit
import javax.inject.Inject

internal sealed interface NotesList {
    data object Loading : NotesList

    data object Failure : NotesList

    data class Ready(
        val entries: List<NoteEntry>,
    ) : NotesList
}

@HiltViewModel
internal class NotesViewModel
    @Inject
    constructor(
        private val repository: NotesRepository,
        private val savedState: SavedStateHandle,
        @ApplicationContext private val context: Context,
    ) : ViewModel() {
        val editing = savedState.getStateFlow<String?>("editing_entry", null)
        val busy = MutableStateFlow(false)
        val error = MutableStateFlow<String?>(null)
        val share = MutableStateFlow<Uri?>(null)

        fun observe(filter: NoteFilter) =
            repository
                .observe(filter)
                .map<List<NoteEntry>, NotesList>(NotesList::Ready)
                .catch { emit(NotesList.Failure) }

        fun edit(entry: NoteEntry) {
            if (!busy.value) savedState["editing_entry"] = entry.key
        }

        fun closeEditor() {
            if (!busy.value) savedState["editing_entry"] = null
        }

        fun save(draft: HighlightDraft) =
            update("Couldn't save this annotation.") {
                repository.saveHighlight(draft)
                savedState["editing_entry"] = null
            }

        fun rename(
            entry: NoteEntry,
            label: String,
        ) = update("Couldn't rename this bookmark.") {
            repository.renameBookmark(entry.id, label)
            savedState["editing_entry"] = null
        }

        fun delete(entry: NoteEntry) =
            update("Couldn't delete this entry.") {
                if (entry.type == NoteType.BOOKMARK) {
                    repository.deleteBookmark(entry.id)
                } else {
                    repository.deleteHighlight(entry.id)
                }
                savedState["editing_entry"] = null
            }

        fun export(bookId: Long) =
            update("Couldn't export these notes.") {
                share.value =
                    withContext(Dispatchers.IO) {
                        createNotesShareFile(context, repository.exportMarkdown(bookId))
                    }
            }

        private fun update(
            message: String,
            action: suspend () -> Unit,
        ) {
            if (!busy.compareAndSet(expect = false, update = true)) return
            viewModelScope.launch {
                try {
                    action()
                } catch (cancelled: CancellationException) {
                    throw cancelled
                } catch (_: Exception) {
                    error.value = message
                } finally {
                    busy.value = false
                }
            }
        }
    }

internal val NoteEntry.key: String get() = "${type == NoteType.BOOKMARK}:$id"

private fun createNotesShareFile(
    context: Context,
    markdown: String,
): Uri {
    val exports = File(context.cacheDir, "notes-share")
    check(exports.isDirectory || exports.mkdirs())
    val expired = System.currentTimeMillis() - TimeUnit.DAYS.toMillis(1)
    exports.listFiles()?.filter { it.lastModified() < expired }?.forEach { folder ->
        File(folder, "deft-notes.md").delete()
        folder.delete()
    }
    val folder =
        kotlin.io.path
            .createTempDirectory(exports.toPath(), "export-")
            .toFile()
    val file = File(folder, "deft-notes.md")
    var published = false
    try {
        file.writeText(markdown)
        return FileProvider.getUriForFile(context, "${context.packageName}.notes-share", file).also {
            published = true
        }
    } finally {
        if (!published) {
            file.delete()
            folder.delete()
        }
    }
}
