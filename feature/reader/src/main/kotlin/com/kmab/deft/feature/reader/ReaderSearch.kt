@file:OptIn(org.readium.r2.shared.ExperimentalReadiumApi::class)

package com.kmab.deft.feature.reader

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.readium.r2.shared.publication.Locator
import org.readium.r2.shared.publication.Publication
import org.readium.r2.shared.publication.services.search.SearchIterator
import org.readium.r2.shared.publication.services.search.search
import kotlin.coroutines.coroutineContext

internal data class ReaderSearchState(
    val query: String = "",
    val searching: Boolean = false,
    val results: List<Locator> = emptyList(),
    val error: String? = null,
)

/** A cancellable, publication-local search; each query owns and closes its iterator. */
internal class ReaderSearch(
    private val scope: CoroutineScope,
) {
    private val mutableState = MutableStateFlow(ReaderSearchState())
    val state = mutableState.asStateFlow()
    private var job: Job? = null

    fun query(
        text: String,
        publication: Publication?,
    ) {
        job?.cancel()
        mutableState.value = ReaderSearchState(query = text, searching = text.isNotBlank())
        if (text.isBlank() || publication == null) {
            mutableState.update { it.copy(searching = false) }
            return
        }
        job =
            scope.launch {
                delay(SEARCH_DELAY)
                try {
                    val iterator = publication.search(text.trim())
                    if (iterator == null) {
                        mutableState.update { it.copy(searching = false, error = "This book cannot be searched.") }
                        return@launch
                    }
                    try {
                        var more = true
                        while (more) {
                            more = readSearchPage(iterator)
                        }
                        mutableState.update { it.copy(searching = false) }
                    } finally {
                        iterator.close()
                    }
                } catch (cancel: CancellationException) {
                    throw cancel
                } catch (_: Exception) {
                    mutableState.update {
                        it.copy(
                            searching = false,
                            error = "Search could not read this book. Try again.",
                        )
                    }
                }
            }
    }

    private suspend fun readSearchPage(iterator: SearchIterator): Boolean {
        val page = iterator.next()
        coroutineContext.ensureActive()
        val results = page.getOrNull()?.locators
        return when {
            page.isFailure -> {
                mutableState.update { it.copy(error = "Some of this book could not be searched.") }
                false
            }
            results == null -> false
            else -> {
                mutableState.update { it.copy(results = (it.results + results).take(RESULT_LIMIT)) }
                val limitReached = mutableState.value.results.size >= RESULT_LIMIT
                if (limitReached) {
                    mutableState.update {
                        it.copy(
                            error =
                                "Showing the first $RESULT_LIMIT matches. " +
                                    "Refine your search for more precise results.",
                        )
                    }
                }
                !limitReached
            }
        }
    }

    fun cancel() {
        job?.cancel()
        mutableState.update { it.copy(searching = false) }
    }
}

@Composable
internal fun ReaderSearchPanel(
    state: ReaderSearchState,
    query: (String) -> Unit,
    select: (Locator) -> Unit,
) {
    Column(Modifier.fillMaxWidth().heightIn(max = 620.dp).padding(horizontal = 20.dp)) {
        Text("search this book", style = MaterialTheme.typography.titleLarge)
        OutlinedTextField(
            value = state.query,
            onValueChange = query,
            label = { Text("word or phrase") },
            modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
            singleLine = true,
        )
        if (state.searching) LinearProgressIndicator(Modifier.fillMaxWidth())
        Text(
            state.error ?: when {
                state.query.isBlank() -> "Search the text in this book."
                state.searching -> "Searching · ${state.results.size} results"
                state.results.isEmpty() -> "No matches. Try another word or phrase."
                else -> "${state.results.size} results"
            },
            Modifier.padding(vertical = 8.dp),
            style = MaterialTheme.typography.labelMedium,
        )
        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            itemsIndexed(state.results) { _, locator ->
                Surface(shape = MaterialTheme.shapes.medium, color = MaterialTheme.colorScheme.surfaceVariant) {
                    Column(Modifier.fillMaxWidth().clickable { select(locator) }.padding(14.dp)) {
                        locator.title?.takeIf(String::isNotBlank)?.let {
                            Text(
                                it,
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.primary,
                            )
                        }
                        Text(
                            buildAnnotatedString {
                                append(locator.text.before.orEmpty())
                                withStyle(SpanStyle(color = MaterialTheme.colorScheme.primary)) {
                                    append(locator.text.highlight.orEmpty())
                                }
                                append(locator.text.after.orEmpty())
                            },
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    }
                }
            }
        }
    }
}

private const val SEARCH_DELAY = 300L
private const val RESULT_LIMIT = 500
