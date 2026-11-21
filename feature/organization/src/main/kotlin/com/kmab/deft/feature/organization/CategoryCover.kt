package com.kmab.deft.feature.organization

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.unit.dp
import coil3.ImageLoader
import coil3.compose.AsyncImage
import com.kmab.deft.core.database.model.BookListItem
import com.kmab.deft.core.designsystem.DeftShapes
import com.kmab.deft.core.designsystem.GeneratedCover
import com.kmab.deft.core.designsystem.LocalDeftColors
import com.kmab.deft.core.designsystem.icons.DeftIcons
import com.kmab.deft.core.designsystem.privateCoverFile
import com.kmab.deft.library.organization.CategorySummary

internal val LocalOrganizationImageLoader = staticCompositionLocalOf<ImageLoader?> { null }

@Composable
internal fun rememberOrganizationImageLoader(): ImageLoader {
    LocalOrganizationImageLoader.current?.let { return it }
    val context = LocalContext.current.applicationContext
    val loader = remember(context) { ImageLoader.Builder(context).diskCache(null).build() }
    DisposableEffect(loader) { onDispose { loader.shutdown() } }
    return loader
}

@Composable
internal fun CategoryCover(
    category: CategorySummary,
    modifier: Modifier,
) {
    val context = LocalContext.current.applicationContext
    val loader = rememberOrganizationImageLoader()
    val custom =
        remember(context, category.category.coverPath) {
            privateCoverFile(category.category.coverPath, context.filesDir)
        }
    val colors = LocalDeftColors.current
    val books = category.books.take(COLLAGE_LIMIT)
    Box(modifier.clip(DeftShapes.cover).background(colors.surface2), contentAlignment = Alignment.Center) {
        if (books.isEmpty()) {
            Icon(DeftIcons.Categories, null, tint = colors.muted)
        } else {
            val columns = if (books.size == 1) 1 else 2
            Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                books.chunked(columns).forEach { row ->
                    Row(Modifier.weight(1f), horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                        row.forEach { member ->
                            OrganizationBookCover(member.book.book, Modifier.weight(1f).fillMaxSize(), loader)
                        }
                        if (row.size < columns) Box(Modifier.weight(1f).fillMaxSize())
                    }
                }
            }
        }
        if (custom != null) LocalCoverImage(custom, loader)
    }
}

@Composable
internal fun OrganizationBookCover(
    book: BookListItem,
    modifier: Modifier,
    loader: ImageLoader = rememberOrganizationImageLoader(),
) {
    val context = LocalContext.current.applicationContext
    val cover = remember(book.coverPath, context) { privateCoverFile(book.coverPath, context.filesDir) }
    Box(modifier.clip(DeftShapes.cover).clearAndSetSemantics { }) {
        GeneratedCover(book.displayTitle(), book.displayAuthors())
        if (cover != null) LocalCoverImage(cover, loader)
    }
}

@Composable
private fun LocalCoverImage(
    cover: java.io.File,
    loader: ImageLoader,
) {
    AsyncImage(
        cover,
        contentDescription = null,
        imageLoader = loader,
        modifier = Modifier.fillMaxSize(),
        contentScale = ContentScale.Crop,
    )
}

private const val COLLAGE_LIMIT = 4
