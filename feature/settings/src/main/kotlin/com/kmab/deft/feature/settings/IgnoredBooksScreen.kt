package com.kmab.deft.feature.settings

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.kmab.deft.core.database.model.BookListItem
import com.kmab.deft.core.designsystem.DeftButton
import com.kmab.deft.core.designsystem.DeftButtonStyle
import com.kmab.deft.core.designsystem.DeftNoticeKind
import com.kmab.deft.core.designsystem.DeftTopBar
import com.kmab.deft.core.designsystem.DeftTypography
import com.kmab.deft.core.designsystem.LocalDeftColors
import com.kmab.deft.core.designsystem.LocalDeftNoticeState
import com.kmab.deft.core.designsystem.icons.DeftActionIcons

@Composable
fun IgnoredBooksScreen(
    onBack: () -> Unit,
    onOpenBook: (Long) -> Unit,
    viewModel: IgnoredBooksViewModel = hiltViewModel(),
) {
    val books by viewModel.books.collectAsStateWithLifecycle()
    val error by viewModel.error.collectAsStateWithLifecycle()
    val notices = LocalDeftNoticeState.current
    LaunchedEffect(error) {
        error?.let { message ->
            if (notices != null) {
                notices.show(message, DeftNoticeKind.Error)
                viewModel.error.value = null
            }
        }
    }
    IgnoredBooksContent(books, onBack, onOpenBook, viewModel::restore)
}

@Composable
internal fun IgnoredBooksContent(
    books: List<BookListItem>?,
    onBack: () -> Unit,
    onOpenBook: (Long) -> Unit,
    onRestore: (Long) -> Unit,
) {
    val colors = LocalDeftColors.current
    Scaffold(containerColor = colors.bg, contentColor = colors.text, topBar = {
        DeftTopBar("ignored books", navigationIcon = {
            IconButton(onBack) { Icon(DeftActionIcons.Back, "back", tint = colors.text) }
        })
    }) { padding ->
        LazyColumn(Modifier.padding(padding).padding(horizontal = 20.dp)) {
            item {
                Text(
                    "These books stay out of your library. Their positions and bookmarks are kept.",
                    Modifier.padding(vertical = 16.dp),
                    style = DeftTypography.body,
                    color = colors.muted,
                )
                if (books == null) CircularProgressIndicator(color = colors.accent)
                if (books?.isEmpty() == true) Text("no ignored books", style = DeftTypography.title)
            }
            items(books.orEmpty(), key = { it.id }) { book ->
                Column(Modifier.animateItem().animateContentSize()) {
                    Row(
                        Modifier.fillMaxWidth().padding(vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        Column(
                            Modifier
                                .weight(1f)
                                .clickable(role = Role.Button) { onOpenBook(book.id) }
                                .padding(vertical = 12.dp),
                        ) {
                            Text(book.title, style = DeftTypography.title)
                            Text(book.authorsDisplay, style = DeftTypography.body, color = colors.muted)
                            if (book.missing) Text("file missing", style = DeftTypography.mono, color = colors.muted)
                        }
                        DeftButton(
                            "un-ignore",
                            { onRestore(book.id) },
                            Modifier.semantics { contentDescription = "un-ignore ${book.title}" },
                            style = DeftButtonStyle.Text,
                            icon = DeftActionIcons.Check,
                        )
                    }
                    HorizontalDivider(color = colors.line)
                }
            }
        }
    }
}
