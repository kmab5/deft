package com.kmab.deft.feature.library

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.kmab.deft.core.database.model.BookListItem
import com.kmab.deft.core.designsystem.DeftShapes
import com.kmab.deft.core.designsystem.DeftTypography
import com.kmab.deft.core.designsystem.LocalDeftColors
import com.kmab.deft.core.designsystem.icons.DeftIcons

@Composable
internal fun LibrarySearch(
    query: String,
    onQueryChange: (String) -> Unit,
    onClose: () -> Unit,
) {
    val colors = LocalDeftColors.current
    val focus = remember { FocusRequester() }
    val keyboard = LocalSoftwareKeyboardController.current
    LaunchedEffect(Unit) { focus.requestFocus() }
    BackHandler(onBack = onClose)
    OutlinedTextField(
        value = query,
        onValueChange = onQueryChange,
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp)
                .focusRequester(focus)
                .testTag("library_search"),
        placeholder = { Text("title or author", style = DeftTypography.body) },
        leadingIcon = { Icon(DeftIcons.Search, null) },
        trailingIcon = {
            IconButton(onClick = { if (query.isEmpty()) onClose() else onQueryChange("") }) {
                Icon(LibraryIcons.Close, if (query.isEmpty()) "close search" else "clear search")
            }
        },
        textStyle = DeftTypography.body,
        singleLine = true,
        shape = DeftShapes.small,
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
        keyboardActions = KeyboardActions(onSearch = { keyboard?.hide() }),
        colors =
            OutlinedTextFieldDefaults.colors(
                focusedBorderColor = colors.accent,
                unfocusedBorderColor = colors.line2,
                focusedTextColor = colors.text,
                unfocusedTextColor = colors.text,
                cursorColor = colors.accentText,
                focusedContainerColor = colors.surface,
                unfocusedContainerColor = colors.surface,
            ),
    )
}

/** Search refines the active format/missing filters and preserves the selected ordering. */
internal fun List<BookListItem>.searchBooks(
    query: String,
    options: com.kmab.deft.core.model.SearchOptions =
        com.kmab.deft.core.model
            .SearchOptions(),
): List<BookListItem> {
    if (query.isBlank()) return this
    require(query.length <= com.kmab.deft.core.model.SearchPattern.MAX_PATTERN_LENGTH) {
        "Use a search pattern of at most 256 characters."
    }
    val queries = if (options.regex) listOf(query) else query.trim().split(Regex("\\s+"))
    val patterns =
        queries.map {
            com.kmab.deft.core.model.SearchPattern
                .compile(it, options)
        }
    return filter { book -> patterns.all { it.contains(book.title) || it.contains(book.authorsDisplay) } }
}

@Composable
internal fun LibrarySearchFilters(
    advanced: Boolean,
    options: com.kmab.deft.core.model.SearchOptions,
    onOptions: (com.kmab.deft.core.model.SearchOptions) -> Unit,
    result: Result<List<BookListItem>>,
) {
    if (advanced) {
        com.kmab.deft.core.designsystem
            .SearchOptionsControls(options, onOptions, Modifier.padding(horizontal = 16.dp, vertical = 4.dp))
    }
    result.exceptionOrNull()?.message?.let { Text(it, Modifier.padding(horizontal = 16.dp)) }
}
