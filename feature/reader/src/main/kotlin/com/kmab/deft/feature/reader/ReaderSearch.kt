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
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import com.kmab.deft.core.designsystem.SearchOptionsControls
import com.kmab.deft.core.model.SearchOptions
import com.kmab.deft.core.model.SearchPattern
import com.kmab.deft.format.readium.searchText
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.readium.r2.shared.publication.Locator
import org.readium.r2.shared.publication.Publication
import kotlin.coroutines.coroutineContext

internal data class ReaderSearchState(
    val query: String = "",
    val searching: Boolean = false,
    val results: List<Locator> = emptyList(),
    val error: String? = null,
    val options: SearchOptions = SearchOptions(),
    val submitted: Boolean = false,
)

/** A cancellable, publication-local search; each submitted query closes its chapter resources. */
internal class ReaderSearch(
    private val scope: CoroutineScope,
) {
    private val mutableState = MutableStateFlow(ReaderSearchState())
    val state = mutableState.asStateFlow()
    private var job: Job? = null

    fun edit(text: String) {
        job?.cancel()
        mutableState.update {
            it.copy(
                query = text,
                searching = false,
                results = emptyList(),
                error = null,
                submitted = false,
            )
        }
    }

    fun options(value: SearchOptions) {
        job?.cancel()
        mutableState.update {
            it.copy(options = value, searching = false, results = emptyList(), error = null, submitted = false)
        }
    }

    fun submit(publication: Publication?) = query(state.value.query, publication)

    fun query(
        text: String,
        publication: Publication?,
    ) {
        job?.cancel()
        val options = state.value.options
        mutableState.value = ReaderSearchState(query = text, options = options, submitted = true)
        if (text.isBlank() || publication == null) return
        val pattern =
            try {
                SearchPattern.compile(if (options.regex) text else text.trim(), options)
            } catch (error: IllegalArgumentException) {
                mutableState.update { it.copy(error = error.message) }
                return
            }
        mutableState.update { it.copy(searching = true) }
        job =
            scope.launch {
                try {
                    val result = publication.searchText(pattern, RESULT_LIMIT)
                    coroutineContext.ensureActive()
                    mutableState.update {
                        it.copy(
                            searching = false,
                            results = result.locators,
                            error =
                                if (result.incomplete) {
                                    "Search is limited to 500 matches and bounded chapters. " +
                                        "Refine your search; some text may be omitted."
                                } else {
                                    null
                                },
                        )
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
    submit: () -> Unit = {},
    advanced: Boolean = false,
    options: (SearchOptions) -> Unit = {},
) {
    Column(Modifier.fillMaxWidth().heightIn(max = 620.dp).padding(horizontal = 20.dp)) {
        Text("search this book", style = MaterialTheme.typography.titleLarge)
        OutlinedTextField(
            value = state.query,
            onValueChange = query,
            label = { Text("word or phrase") },
            modifier =
                Modifier.fillMaxWidth().padding(vertical = 12.dp).onPreviewKeyEvent {
                    if (it.key == Key.Enter || it.key == Key.NumPadEnter) {
                        if (it.type == KeyEventType.KeyUp) submit()
                        true
                    } else {
                        false
                    }
                },
            singleLine = true,
            trailingIcon = { TextButton(onClick = submit) { Text("Search") } },
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            keyboardActions = KeyboardActions(onSearch = { submit() }),
        )
        if (advanced) SearchOptionsControls(state.options, options)
        if (state.searching) LinearProgressIndicator(Modifier.fillMaxWidth())
        Text(
            state.error ?: when {
                !state.submitted || state.query.isBlank() -> "Search the text in this book."
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

private const val RESULT_LIMIT = 500
