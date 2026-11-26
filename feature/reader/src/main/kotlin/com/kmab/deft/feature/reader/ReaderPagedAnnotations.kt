@file:OptIn(org.readium.r2.shared.ExperimentalReadiumApi::class)

package com.kmab.deft.feature.reader

import android.view.ActionMode
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.lifecycleScope
import com.kmab.deft.core.database.entity.HighlightEntity
import com.kmab.deft.core.designsystem.DeftHaptic
import com.kmab.deft.core.designsystem.performDeftHaptic
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject
import org.json.JSONTokener
import org.readium.r2.navigator.DecorableNavigator
import org.readium.r2.navigator.Decoration
import org.readium.r2.navigator.epub.EpubNavigatorFragment
import org.readium.r2.shared.publication.Locator

internal data class ReaderAnnotationActions(
    val selection: (ReaderSelectionAction, Locator) -> Unit = { _, _ -> },
    val edit: (Long) -> Unit = {},
)

/** Paged annotations keep verified ranges and selection lifetime beside their Readium adapter. */
internal class ReaderPagedAnnotations(
    private val activity: FragmentActivity,
    private val notice: (String) -> Unit = {},
    private val navigator: () -> EpubNavigatorFragment?,
) {
    private var annotationJob: Job? = null
    private var highlights: List<HighlightEntity> = emptyList()
    var pendingRange: Locator? = null
    var searchLocator: Locator? = null
    private var feedback: ReaderSelectionFeedback? = null
    val hasToolbar: Boolean get() = feedback?.hasToolbar == true
    private val anchorScript by lazy {
        activity.assets
            .open("deft-reader/annotations.js")
            .bufferedReader()
            .use { it.readText() }
    }

    fun close() {
        feedback?.close()
        feedback = null
        annotationJob?.cancel()
        annotationJob = null
        pendingRange = null
        searchLocator = null
    }

    fun apply(annotations: List<HighlightEntity>) {
        highlights = annotations
        val navigator = navigator() ?: return
        annotationJob?.cancel()
        annotationJob =
            activity.lifecycleScope.launch {
                navigator.applyDecorations(readerDecorations(annotations), ANNOTATION_GROUP)
                val search = searchLocator
                val searchDecorations =
                    search
                        ?.let {
                            listOf(Decoration("search", it.forReadiumRange(), Decoration.Style.Highlight(SEARCH_TINT)))
                        }.orEmpty()
                navigator.applyDecorations(searchDecorations, SEARCH_GROUP)
                // Readium's quote matcher does not consume DOM ranges. Correct its exposed ranges
                // before layout so identical and cross-node passages retain their exact anchors.
                navigator.evaluateJavascript(
                    anchorScript + ";" +
                        "deftAnnotations.correctReadium(readium.getDecorations('$ANNOTATION_GROUP')," +
                        "${continuousAnnotations(annotations)});",
                )
                if (search != null) {
                    val anchor =
                        JSONObject()
                            .put("id", "search")
                            .put("text", search.text.highlight)
                            .put("domRange", search.toJSON().optJSONObject("locations")?.optJSONObject("domRange"))
                    navigator.evaluateJavascript(
                        "deftAnnotations.correctReadium(readium.getDecorations('$SEARCH_GROUP')," +
                            "${JSONArray().put(anchor)});",
                    )
                }
                pendingRange?.let { point ->
                    val applied = navigator.evaluateJavascript("deftAnnotations.goReadium(${point.toJSON()});")
                    if (applied == "true" && pendingRange == point) pendingRange = null
                }
            }
    }

    fun clearSearch() {
        if (searchLocator != null) {
            searchLocator = null
            apply(highlights)
        }
    }

    fun listenForEdits(onEdit: (Long) -> Unit) {
        navigator()?.addDecorationListener(
            ANNOTATION_GROUP,
            object : DecorableNavigator.Listener {
                override fun onDecorationActivated(event: DecorableNavigator.OnActivatedEvent): Boolean {
                    event.decoration.id
                        .toLongOrNull()
                        ?.let(onEdit)
                    return true
                }
            },
        )
    }

    private suspend fun currentSelection(full: Boolean = true): Locator? {
        val navigator = navigator() ?: return null
        val selected = navigator.currentLocator.value
        val method = if (full) "selection" else "sample"
        val install = if (full) anchorScript + ";" else ""
        val raw = navigator.evaluateJavascript(install + "JSON.stringify(window.deftAnnotations?.$method(document))")
        val capture = runCatching { JSONObject(JSONTokener(raw).nextValue().toString()) }.getOrNull()
        return capture?.let { selectionLocator(selected, it) }
    }

    fun selectionTouch(down: Boolean) {
        feedback?.touchChanged(down)
    }

    fun selectionCallback(
        onSelectionStarted: (Locator) -> Unit,
        onSelectionAction: (ReaderSelectionAction, Locator) -> Unit,
    ): ActionMode.Callback {
        val feedback =
            ReaderSelectionFeedback(
                activity.lifecycleScope,
                activity.lifecycle,
                { currentSelection(full = false) },
                { activity.window.decorView.performDeftHaptic(DeftHaptic.CHOICE) },
                onSelectionStarted,
            )
        this.feedback = feedback
        return readerSelectionCallback(
            activity,
            feedback,
            ReaderSelectionOwner({ currentSelection() }, { navigator()?.clearSelection() }, onSelectionAction),
            notice = notice,
        )
    }
}

private const val SEARCH_GROUP = "deft-search"
private val SEARCH_TINT = android.graphics.Color.parseColor("#9867c5")
