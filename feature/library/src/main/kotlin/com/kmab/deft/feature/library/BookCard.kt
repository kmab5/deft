package com.kmab.deft.feature.library

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.ImageLoader
import coil3.compose.AsyncImage
import com.kmab.deft.core.database.model.BookListItem
import com.kmab.deft.core.designsystem.CoverImage
import com.kmab.deft.core.designsystem.DeftColors
import com.kmab.deft.core.designsystem.DeftTypography
import com.kmab.deft.core.designsystem.GeneratedCover
import com.kmab.deft.core.designsystem.LocalDeftColors
import com.kmab.deft.core.designsystem.Palette
import com.kmab.deft.core.designsystem.icons.DeftIcons
import com.kmab.deft.core.model.BookDisplaySize
import java.io.File
import java.nio.file.InvalidPathException

@Composable
fun BookCard(
    book: BookListItem,
    imageLoader: ImageLoader,
    modifier: Modifier = Modifier,
    size: BookDisplaySize = BookDisplaySize.MEDIUM,
    onClick: (() -> Unit)? = null,
) {
    val c = LocalDeftColors.current
    Column(
        modifier
            .testTag("book_${book.id}")
            .then(
                if (onClick ==
                    null
                ) {
                    Modifier
                } else {
                    Modifier.clickable(role = Role.Button, onClick = onClick)
                },
            ).semantics(mergeDescendants = true) {
                stateDescription = if (book.lastReadAt == null) "not read" else "previously read"
            },
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        LibraryCover(book, imageLoader, showBadges = size != BookDisplaySize.SMALL)
        if (size == BookDisplaySize.SMALL && (book.missing || book.drmProtected)) {
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                if (book.missing) Icon(LibraryIcons.Missing, "missing", Modifier.size(16.dp), tint = c.muted)
                if (book.drmProtected) Icon(DeftIcons.Lock, "protected", Modifier.size(16.dp), tint = c.muted)
            }
        }
        Text(
            book.title,
            color = c.text,
            style = if (size == BookDisplaySize.LARGE) DeftTypography.label else DeftTypography.cardTitle,
            minLines = 2,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
        Text(
            book.firstAuthor(),
            color = c.muted,
            style = DeftTypography.cardTitle,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
internal fun LibraryCover(
    book: BookListItem,
    imageLoader: ImageLoader,
    modifier: Modifier = Modifier,
    showBadges: Boolean = true,
    compactPlaceholder: Boolean = false,
) {
    val root = LocalContext.current.filesDir
    val cover = remember(book.coverPath, root) { privateCover(book.coverPath, root) }
    CoverImage(
        book.title,
        book.firstAuthor(),
        modifier,
        missing = showBadges && book.missing,
        isProtected =
            showBadges && book.drmProtected,
    ) {
        Box(Modifier.fillMaxSize().clearAndSetSemantics { }) {
            if (compactPlaceholder) {
                Box(
                    Modifier.fillMaxSize().background(
                        Palette.coverTints[Math.floorMod(book.title.hashCode(), Palette.coverTints.size)],
                    ),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        book.title
                            .trim()
                            .firstOrNull()
                            ?.uppercaseChar()
                            ?.toString() ?: "?",
                        style = DeftTypography.title,
                        color = DeftColors.Ink.text,
                    )
                }
            } else {
                GeneratedCover(book.title, book.firstAuthor())
            }
            if (cover != null) {
                AsyncImage(
                    model = cover,
                    imageLoader = imageLoader,
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop,
                )
            }
        }
    }
}

/** Only app-private files enter Coil; database strings cannot become URLs or SAF writes. */
internal fun privateCover(
    path: String?,
    root: File,
): File? {
    if (path == null) return null
    return try {
        val candidate = File(path).toPath().normalize()
        if (candidate.isAbsolute && candidate.startsWith(root.toPath().normalize())) candidate.toFile() else null
    } catch (_: InvalidPathException) {
        null
    }
}

internal fun BookListItem.firstAuthor(): String = authorsDisplay.substringBefore(", ")
