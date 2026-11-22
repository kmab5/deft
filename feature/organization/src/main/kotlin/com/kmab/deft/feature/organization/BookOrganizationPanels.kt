package com.kmab.deft.feature.organization

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.kmab.deft.core.designsystem.DeftButton
import com.kmab.deft.core.designsystem.DeftButtonStyle
import com.kmab.deft.core.designsystem.DeftDialog
import com.kmab.deft.core.designsystem.DeftNoticeKind
import com.kmab.deft.core.designsystem.DeftTypography
import com.kmab.deft.core.designsystem.LocalDeftColors
import com.kmab.deft.core.designsystem.LocalDeftNoticeState
import com.kmab.deft.core.designsystem.icons.DeftActionIcons

@Composable
fun BookAuthorLinks(
    bookId: Long,
    fallback: String,
    openAuthor: (Long) -> Unit,
) {
    val model: OrganizationViewModel = hiltViewModel()
    val state by model.state.collectAsStateWithLifecycle()
    val snapshot = state.snapshot
    val authors =
        remember(snapshot, bookId) {
            val byId = snapshot?.authors.orEmpty().associateBy { it.author.id }
            snapshot
                ?.books
                ?.firstOrNull { it.book.id == bookId }
                ?.authorIds
                .orEmpty()
                .mapNotNull(byId::get)
        }
    if (authors.isEmpty()) {
        Text(fallback, style = DeftTypography.body, color = LocalDeftColors.current.muted)
    } else {
        FlowRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            authors.forEach { entry ->
                val author = entry.author
                androidx.compose.foundation.layout.Box(
                    Modifier
                        .heightIn(min = 48.dp)
                        .clickable(role = Role.Button) { openAuthor(author.id) }
                        .padding(vertical = 4.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(author.name, style = DeftTypography.body, color = LocalDeftColors.current.accentText)
                }
            }
        }
    }
}

@Composable
fun BookOrganizationPanels(
    bookId: Long,
    openCategory: (Long) -> Unit,
    openBook: (Long) -> Unit,
) {
    val model: OrganizationViewModel = hiltViewModel()
    val state by model.state.collectAsStateWithLifecycle()
    val snapshot = state.snapshot
    if (snapshot == null) {
        if (state.loadFailed) OrganizationPanel { Text("Couldn't load this book's categories.") }
        return
    }
    val error by model.error.collectAsStateWithLifecycle()
    val sheetActive by model.sheetActive.collectAsStateWithLifecycle()
    val notices = LocalDeftNoticeState.current
    LaunchedEffect(error, sheetActive) {
        if (!sheetActive) {
            error?.let {
                notices?.show(it, DeftNoticeKind.Error)
                model.error.value = null
            }
        }
    }
    val memberships =
        snapshot.categories.mapNotNull { category ->
            category.books.find { it.book.book.id == bookId }?.let { category to it }
        }
    val next = snapshot.nextInSeries(bookId)
    var picking by rememberSaveable { mutableStateOf(false) }
    var removingId by rememberSaveable { mutableStateOf<Long?>(null) }
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        OrganizationPanel {
            Text("categories", style = DeftTypography.title)
            if (memberships.isEmpty()) Text("No categories yet.", style = DeftTypography.body)
            memberships.forEach { (category, member) ->
                val name = category.category.name
                val position = if (category.isSeries) "#${positionLabel(member.membership.position)} · " else ""
                val types = category.types.joinToString(" · ") { it.name }.ifEmpty { "untyped" }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    androidx.compose.foundation.layout.Box(Modifier.weight(1f)) {
                        OrganizationRow(name, position + types) { openCategory(category.category.id) }
                    }
                    IconButton({ removingId = category.category.id }) {
                        Icon(DeftActionIcons.Close, "remove from $name")
                    }
                }
            }
            DeftButton("add to category", { picking = true }, style = DeftButtonStyle.Secondary)
        }
        if (next.isNotEmpty()) {
            OrganizationPanel {
                Text("next in series", style = DeftTypography.title)
                next.forEach { item ->
                    OrganizationRow(
                        item.book.book.displayTitle(),
                        "${item.categoryName} · #${positionLabel(item.position)}",
                    ) {
                        openBook(item.book.book.id)
                    }
                }
            }
        }
    }
    if (picking) CategoryMembershipPicker(setOf(bookId), { picking = false }, { picking = false })
    val removing = memberships.firstOrNull { it.first.category.id == removingId }?.first
    if (removing != null) {
        DeftDialog(
            "remove from category?",
            "Remove this book from ${removing.category.name}. " +
                "Its reading history will be kept.",
            "remove",
            {
                removingId = null
                model.edit { removeMember(removing.category.id, bookId) }
            },
            { removingId = null },
        )
    }
}
