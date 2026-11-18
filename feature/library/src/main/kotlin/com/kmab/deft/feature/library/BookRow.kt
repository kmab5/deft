package com.kmab.deft.feature.library

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.ImageLoader
import com.kmab.deft.core.database.model.BookListItem
import com.kmab.deft.core.designsystem.DeftTypography
import com.kmab.deft.core.designsystem.LocalDeftColors
import com.kmab.deft.core.model.BookDisplaySize

@Composable
fun BookRow(
    book: BookListItem,
    imageLoader: ImageLoader,
    modifier: Modifier = Modifier,
    size: BookDisplaySize = BookDisplaySize.MEDIUM,
) {
    val c = LocalDeftColors.current
    Row(
        modifier.fillMaxWidth().libraryBookSurface(12.dp).semantics(mergeDescendants = true) {
            stateDescription =
                if (book.lastReadAt == null) "not read" else "previously read"
        },
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        LibraryCover(
            book,
            imageLoader,
            Modifier.width(size.listCoverWidth()),
            showBadges = false,
            compactPlaceholder = true,
        )
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                book.displayTitle(),
                color = c.text,
                style = if (size == BookDisplaySize.LARGE) DeftTypography.title else DeftTypography.label,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                book.displayAuthors(),
                color = c.muted,
                style = DeftTypography.cardTitle,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                listOfNotNull(
                    book.format.displayName.lowercase(),
                    "missing".takeIf { book.missing },
                    "protected".takeIf { book.drmProtected },
                ).joinToString(" · "),
                color = c.muted,
                style = DeftTypography.cardTitle,
            )
        }
    }
}

internal fun BookDisplaySize.listCoverWidth() =
    when (this) {
        BookDisplaySize.SMALL -> 44.dp
        BookDisplaySize.MEDIUM -> 60.dp
        BookDisplaySize.LARGE -> 80.dp
    }
