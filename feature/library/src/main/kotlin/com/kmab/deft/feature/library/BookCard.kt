package com.kmab.deft.feature.library

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.background
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.ImageLoader
import coil3.compose.AsyncImage
import com.kmab.deft.core.database.model.BookListItem
import com.kmab.deft.core.designsystem.CoverImage
import com.kmab.deft.core.designsystem.DeftTypography
import com.kmab.deft.core.designsystem.GeneratedCover
import com.kmab.deft.core.designsystem.LocalDeftColors
import java.io.File

@Composable
fun BookCard(book: BookListItem, imageLoader: ImageLoader, modifier: Modifier = Modifier) {
    val c = LocalDeftColors.current
    Column(modifier.semantics(mergeDescendants = true) { stateDescription = "not read" }, verticalArrangement = Arrangement.spacedBy(4.dp)) {
        LibraryCover(book, imageLoader)
        Box(Modifier.fillMaxWidth().height(1.dp).background(c.line))
        Text(book.title, color = c.text, style = DeftTypography.cardTitle, maxLines = 1, overflow = TextOverflow.Ellipsis)
        Text(book.firstAuthor(), color = c.muted, style = DeftTypography.monoSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

@Composable
internal fun LibraryCover(book: BookListItem, imageLoader: ImageLoader, modifier: Modifier = Modifier) {
    val root = LocalContext.current.filesDir
    val cover = remember(book.coverPath, root) { privateCover(book.coverPath, root) }
    CoverImage(book.title, book.firstAuthor(), modifier, missing = book.missing, isProtected = book.drmProtected) {
        Box(Modifier.fillMaxSize().clearAndSetSemantics { }) {
            GeneratedCover(book.title, book.firstAuthor())
            if (cover != null) AsyncImage(model = cover, imageLoader = imageLoader, contentDescription = null,
                modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
        }
    }
}

/** Only app-private files enter Coil; database strings cannot become URLs or SAF writes. */
internal fun privateCover(path: String?, root: File): File? {
    if (path == null) return null
    val candidate = File(path).toPath().normalize()
    return if (candidate.isAbsolute && candidate.startsWith(root.toPath().normalize())) candidate.toFile() else null
}
internal fun BookListItem.firstAuthor(): String = authorsDisplay.substringBefore(", ")
