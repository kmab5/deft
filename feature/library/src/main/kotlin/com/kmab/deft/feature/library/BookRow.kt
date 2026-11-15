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

@Composable
fun BookRow(book: BookListItem, imageLoader: ImageLoader, modifier: Modifier = Modifier) {
    val c = LocalDeftColors.current
    Row(modifier.fillMaxWidth().semantics(mergeDescendants = true) { stateDescription = "not read" },
        horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
        LibraryCover(book, imageLoader, Modifier.width(56.dp))
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(book.title, color = c.text, style = DeftTypography.title, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(book.authorsDisplay, color = c.muted, style = DeftTypography.mono, maxLines = 2, overflow = TextOverflow.Ellipsis)
            Text(book.format.displayName.lowercase(), color = c.muted, style = DeftTypography.monoSmall)
        }
    }
}
