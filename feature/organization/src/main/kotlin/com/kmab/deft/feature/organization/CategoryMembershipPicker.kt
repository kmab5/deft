package com.kmab.deft.feature.organization

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.kmab.deft.core.designsystem.DeftButton
import com.kmab.deft.core.designsystem.DeftNoticeHost
import com.kmab.deft.core.designsystem.DeftNoticeKind
import com.kmab.deft.core.designsystem.DeftSheet
import com.kmab.deft.core.designsystem.DeftTypography
import com.kmab.deft.core.designsystem.LocalDeftColors
import com.kmab.deft.core.designsystem.rememberDeftNoticeState
import com.kmab.deft.library.organization.CategorySummary

@Composable
fun CategoryMembershipPicker(
    bookIds: Set<Long>,
    onDismiss: () -> Unit,
    onAdded: () -> Unit,
) {
    val model: OrganizationViewModel = hiltViewModel()
    val state by model.state.collectAsStateWithLifecycle()
    val busy by model.busy.collectAsStateWithLifecycle()
    var create by rememberSaveable { mutableStateOf(false) }
    LaunchedEffect(Unit) { model.error.value = null }
    val snapshot = state.snapshot
    if (create && snapshot != null) {
        CategoryEditor(null, snapshot.types, model, { create = false }, onSaved = onAdded, initialBooks = bookIds)
    } else {
        OrganizationSheet("add to category", onDismiss, model) {
            if (snapshot == null) {
                OrganizationEmpty(if (state.loadFailed) "Couldn't load categories." else "loading", !state.loadFailed)
            } else {
                val existing =
                    remember(snapshot.categories, bookIds) {
                        snapshot.categories.filter { category ->
                            val confirmedIds =
                                category.books
                                    .asSequence()
                                    .filterNot { it.membership.suggested }
                                    .map { it.membership.bookId }
                                    .toHashSet()
                            !confirmedIds.containsAll(bookIds)
                        }
                    }
                CategoryPickerContent(
                    existing,
                    busy,
                    { id -> model.edit(onAdded) { addBooks(id, bookIds) } },
                    {
                        model.error.value = null
                        create = true
                    },
                )
            }
        }
    }
}

@Composable
internal fun CategoryPickerContent(
    categories: List<CategorySummary>,
    busy: Boolean,
    onPick: (Long) -> Unit,
    onCreate: () -> Unit,
) {
    var query by rememberSaveable { mutableStateOf("") }
    OutlinedTextField(
        query,
        { query = it },
        Modifier.fillMaxWidth(),
        label = { Text("find a category") },
        singleLine = true,
    )
    val matches = categories.filter { it.category.name.contains(query.trim(), ignoreCase = true) }
    if (matches.isEmpty()) Text("No matching categories.", style = DeftTypography.body)
    matches.forEach { category ->
        HorizontalDivider(color = LocalDeftColors.current.line)
        OrganizationRow(category.category.name, category.choiceDetails(), enabled = !busy) {
            onPick(category.category.id)
        }
    }
    DeftButton("create category", onCreate, enabled = !busy)
}

internal fun CategorySummary.choiceDetails(): String {
    val names = types.joinToString(", ") { it.name }.ifEmpty { "untyped" }
    return "$names · ${stats.total} books · #${category.id}"
}

@Composable
internal fun OrganizationSheet(
    title: String,
    onDismiss: () -> Unit,
    model: OrganizationViewModel,
    scrollable: Boolean = true,
    content: @Composable ColumnScope.() -> Unit,
) {
    val busy by model.busy.collectAsStateWithLifecycle()
    val error by model.error.collectAsStateWithLifecycle()
    val notices = rememberDeftNoticeState()
    DisposableEffect(model) {
        model.sheetActive.value = true
        onDispose {
            model.sheetActive.value = false
            model.error.value = null
        }
    }
    LaunchedEffect(error) {
        error?.let {
            notices.show(it, DeftNoticeKind.Error)
            model.error.value = null
        }
    }
    DeftSheet({
        if (!busy) {
            model.error.value = null
            onDismiss()
        }
    }) {
        Box(Modifier.fillMaxWidth()) {
            Column(
                Modifier.fillMaxWidth().then(
                    if (scrollable) Modifier.verticalScroll(rememberScrollState()) else Modifier,
                ),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Row(
                    Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text(title, Modifier.weight(1f), style = DeftTypography.headline)
                    if (busy) {
                        CircularProgressIndicator(Modifier.size(16.dp), strokeWidth = 2.dp)
                        Text("saving…", style = DeftTypography.mono)
                    }
                }
                content()
            }
            DeftNoticeHost(notices, Modifier.align(Alignment.BottomCenter))
        }
    }
}
