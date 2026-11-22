package com.kmab.deft.feature.organization

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.kmab.deft.core.designsystem.DeftTypography
import com.kmab.deft.core.designsystem.LocalDeftColors
import com.kmab.deft.library.organization.OrganizedBook

@Composable
internal fun OrganizationBookInfo(
    entry: OrganizedBook,
    modifier: Modifier = Modifier,
    location: Boolean = false,
) {
    val book = entry.book
    Column(modifier, verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(book.displayTitle(), style = DeftTypography.title, maxLines = 2, overflow = TextOverflow.Ellipsis)
        Text(
            book.displayAuthors(),
            style = DeftTypography.mono,
            color = LocalDeftColors.current.muted,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
        Text(
            "${entry.state.readingLabel()} · ${book.format.name}" +
                if (book.missing) {
                    " · missing"
                } else if (book.drmProtected) {
                    " · protected"
                } else {
                    ""
                },
            style = DeftTypography.monoSmall,
            color = LocalDeftColors.current.muted,
        )
        if (location &&
            book.fileLocation.isNotBlank()
        ) {
            Text(
                book.fileLocation,
                style = DeftTypography.monoSmall,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                color = LocalDeftColors.current.muted,
            )
        }
    }
}

@Composable
internal fun OrganizationBookTile(
    entry: OrganizedBook,
    modifier: Modifier = Modifier,
    open: () -> Unit,
    extra: @Composable () -> Unit = {},
) {
    OrganizationPanel(modifier) {
        Column(Modifier.clickable(onClick = open), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            OrganizationBookCover(entry.book, Modifier.fillMaxWidth().aspectRatio(BOOK_COVER_RATIO))
            OrganizationBookInfo(entry)
        }
        extra()
    }
}

@Composable
internal fun OrganizationBookLine(
    entry: OrganizedBook,
    modifier: Modifier = Modifier,
    location: Boolean = false,
) {
    Row(
        modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        OrganizationBookCover(entry.book, Modifier.size(44.dp, 64.dp))
        OrganizationBookInfo(entry, Modifier.weight(1f), location)
    }
}

private const val BOOK_COVER_RATIO = 0.7f
