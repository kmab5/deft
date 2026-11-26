@file:OptIn(org.readium.r2.shared.ExperimentalReadiumApi::class)

package com.kmab.deft.feature.reader

import android.graphics.Color
import com.kmab.deft.core.database.entity.HighlightEntity
import com.kmab.deft.core.model.HighlightColor
import com.kmab.deft.library.notes.HighlightDraft
import com.kmab.deft.library.notes.NotesRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject
import org.readium.r2.navigator.Decoration
import org.readium.r2.shared.publication.Locator

internal data class ReaderAnnotationState(
    val highlights: List<HighlightEntity> = emptyList(),
    val draft: HighlightDraft? = null,
    val busy: Boolean = false,
)

/** One explicit editing transaction; merely selecting or cancelling never persists anything. */
internal class ReaderAnnotations(
    private val scope: CoroutineScope,
    private val repository: NotesRepository,
    private val notice: (String) -> Unit,
) {
    private val mutableState = MutableStateFlow(ReaderAnnotationState())
    val state = mutableState.asStateFlow()
    private var observation: Job? = null

    fun observe(bookId: Long) {
        observation?.cancel()
        observation =
            scope.launch {
                try {
                    repository.observeHighlights(bookId).collect { values ->
                        mutableState.update { it.copy(highlights = values) }
                    }
                } catch (cancel: CancellationException) {
                    throw cancel
                } catch (_: Exception) {
                    notice("Saved notes could not be loaded. Reopen this book to try again.")
                }
            }
    }

    fun select(
        bookId: Long,
        locator: Locator,
        highlight: Boolean,
    ) {
        if (state.value.busy) return
        val text = locator.text.highlight?.takeIf(String::isNotBlank) ?: return
        val serialized = locator.toJSON().toString()
        val existing =
            state.value.highlights.firstOrNull { saved ->
                saved.locator == serialized || parseLocator(saved.locator)?.let { samePassage(it, locator) } == true
            }
        mutableState.update {
            it.copy(
                draft =
                    existing?.draft() ?: HighlightDraft(
                        bookId = bookId,
                        locator = serialized,
                        text = text,
                        color = if (highlight) HighlightColor.PURPLE else HighlightColor.NONE,
                        totalProgression = locator.locations.totalProgression ?: 0.0,
                    ),
            )
        }
    }

    fun edit(id: Long) {
        if (!state.value.busy) {
            mutableState.update { state ->
                state.copy(draft = state.highlights.firstOrNull { it.id == id }?.draft())
            }
        }
    }

    fun dismiss() {
        if (!state.value.busy) mutableState.update { it.copy(draft = null) }
    }

    fun save(draft: HighlightDraft) =
        mutate {
            repository.saveHighlight(draft)
            notice("Note saved.")
        }

    fun delete() {
        val id = state.value.draft?.id ?: return
        mutate {
            repository.deleteHighlight(id)
            notice("Note deleted.")
        }
    }

    private fun mutate(block: suspend () -> Unit) {
        if (state.value.busy) return
        mutableState.update { it.copy(busy = true) }
        scope.launch {
            try {
                block()
                mutableState.update { it.copy(draft = null) }
            } catch (cancel: CancellationException) {
                throw cancel
            } catch (_: Exception) {
                notice("That note could not be saved. Please try again.")
            } finally {
                mutableState.update { it.copy(busy = false) }
            }
        }
    }
}

private fun HighlightEntity.draft() =
    HighlightDraft(bookId, locator, text, color, note, totalProgression, id, underline)

internal fun highlightHex(color: HighlightColor): String? =
    when (color) {
        HighlightColor.PURPLE -> "#9867c5"
        HighlightColor.GREEN -> "#588767"
        HighlightColor.AMBER -> "#b88436"
        HighlightColor.GRAY -> "#969298"
        HighlightColor.NONE -> null
    }

internal fun readerDecorations(values: List<HighlightEntity>): List<Decoration> =
    values.mapNotNull { value ->
        val color = highlightHex(value.color) ?: return@mapNotNull null
        val locator = parseLocator(value.locator) ?: return@mapNotNull null
        val tint = Color.parseColor(color)
        val style = if (value.underline) Decoration.Style.Underline(tint) else Decoration.Style.Highlight(tint)
        Decoration(value.id.toString(), locator.forReadiumRange(), style)
    }

internal fun continuousAnnotations(values: List<HighlightEntity>): JSONArray =
    JSONArray().apply {
        values.forEach { value ->
            val color = highlightHex(value.color) ?: return@forEach
            val point = parseLocator(value.locator) ?: return@forEach
            put(
                JSONObject().apply {
                    put("id", value.id.toString())
                    put("href", point.href.toString().substringBefore('#'))
                    put("color", color)
                    put("underline", value.underline)
                    put("progression", point.locations.progression ?: 0.0)
                    put("text", value.text)
                    put("before", point.text.before.orEmpty())
                    put("after", point.text.after.orEmpty())
                    put("domRange", point.toJSON().optJSONObject("locations")?.optJSONObject("domRange"))
                },
            )
        }
    }

internal fun selectionLocator(
    base: Locator,
    capture: JSONObject,
): Locator {
    val json = base.toJSON()
    val locations = json.optJSONObject("locations") ?: JSONObject().also { json.put("locations", it) }
    capture.optJSONObject("domRange")?.let { locations.put("domRange", it) }
    capture.optString("cssSelector").takeIf(String::isNotBlank)?.let { locations.put("cssSelector", it) }
    json.put(
        "text",
        JSONObject().apply {
            put("highlight", capture.optString("text"))
            put("before", capture.optString("before"))
            put("after", capture.optString("after"))
        },
    )
    return Locator.fromJSON(json) ?: base
}

internal const val ANNOTATION_GROUP = "deft-highlights"

/** Readium matches raw DOM text; selection display text may contain added block-boundary newlines. */
internal fun Locator.forReadiumRange(): Locator {
    val raw = toJSON().optJSONObject("locations")?.optJSONObject("domRange")?.optString("text")
    return if (raw.isNullOrEmpty()) this else copy(text = text.copy(highlight = raw))
}

private fun samePassage(
    left: Locator,
    right: Locator,
): Boolean {
    val leftRange = left.toJSON().optJSONObject("locations")?.optJSONObject("domRange")
    val rightRange = right.toJSON().optJSONObject("locations")?.optJSONObject("domRange")
    return left.href == right.href && left.text.highlight == right.text.highlight &&
        leftRange != null && rightRange != null && leftRange.toString() == rightRange.toString()
}
