package com.kmab.deft.feature.book

import android.text.Html
import android.text.format.Formatter
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import coil3.ImageLoader
import coil3.compose.AsyncImage
import com.kmab.deft.core.database.entity.BookFileEntity
import com.kmab.deft.core.database.model.BookDetails
import com.kmab.deft.core.designsystem.CoverImage
import com.kmab.deft.core.designsystem.DeftButton
import com.kmab.deft.core.designsystem.DeftShapes
import com.kmab.deft.core.designsystem.DeftTypography
import com.kmab.deft.core.designsystem.GeneratedCover
import com.kmab.deft.core.designsystem.LocalDeftColors
import com.kmab.deft.core.designsystem.icons.DeftActionIcons
import com.kmab.deft.core.designsystem.icons.DeftIcons
import java.io.File
import kotlin.math.roundToInt

@Composable
internal fun BookDetailBody(
    details: BookDetails,
    modifier: Modifier,
    busy: Boolean,
    onRead: () -> Unit,
) {
    Box(modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
        Column(
            Modifier
                .widthIn(max = 720.dp)
                .fillMaxWidth()
                .testTag("book_detail")
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
                .animateContentSize(),
            verticalArrangement = Arrangement.spacedBy(24.dp),
        ) {
            BookIdentityPanel(details, busy, onRead)
            BookDescription(details.book.description)
            BookEdition(details)
            BookFiles(details.files)
        }
    }
}

@Composable
private fun BookIdentityPanel(
    details: BookDetails,
    busy: Boolean,
    onRead: () -> Unit,
) {
    val colors = LocalDeftColors.current
    val fontScale = LocalDensity.current.fontScale
    Surface(
        shape = DeftShapes.card,
        color = colors.surface,
        border = BorderStroke(1.dp, colors.line),
        shadowElevation = 1.dp,
        modifier = Modifier.fillMaxWidth(),
    ) {
        BoxWithConstraints(Modifier.padding(16.dp)) {
            val availableWidth = maxWidth
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                if (availableWidth < 280.dp || fontScale > STACKED_METADATA_FONT_SCALE) {
                    BookIdentity(details)
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        verticalAlignment = Alignment.Bottom,
                    ) {
                        DetailCover(details, Modifier.width(80.dp))
                        Box(Modifier.weight(1f), contentAlignment = Alignment.BottomEnd) {
                            BookReadAction(details, busy, onRead)
                        }
                    }
                } else {
                    Row(
                        Modifier.fillMaxWidth().height(IntrinsicSize.Min),
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        verticalAlignment = Alignment.Bottom,
                    ) {
                        DetailCover(details, Modifier.width(104.dp))
                        Column(Modifier.weight(1f).fillMaxHeight()) {
                            BookIdentity(details)
                            Spacer(Modifier.weight(1f).height(12.dp))
                            Box(Modifier.align(Alignment.End)) { BookReadAction(details, busy, onRead) }
                        }
                    }
                }
                SavedProgress(details)
            }
        }
    }
}

@Composable
@OptIn(ExperimentalLayoutApi::class)
private fun BookIdentity(
    details: BookDetails,
    modifier: Modifier = Modifier,
) {
    val colors = LocalDeftColors.current
    Column(modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(details.displayTitle(), style = DeftTypography.headline, color = colors.text)
        Text(details.displayAuthors(), style = DeftTypography.body, color = colors.muted)
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(
                details.book.format.displayName
                    .lowercase(),
                style = DeftTypography.mono,
                color = colors.muted,
            )
            if (details.book.ignored) BookStatus("ignored", DeftActionIcons.FolderMinus)
            if (details.book.drmProtected) BookStatus("protected", DeftIcons.Lock)
            if (details.files.none { !it.missing }) BookStatus("file missing", DeftActionIcons.Info)
        }
    }
}

@Composable
private fun BookStatus(
    label: String,
    icon: ImageVector,
) {
    val colors = LocalDeftColors.current
    Row(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, null, Modifier.size(16.dp), tint = colors.muted)
        Text(label, style = DeftTypography.label, color = colors.muted)
    }
}

@Composable
private fun BookReadAction(
    details: BookDetails,
    busy: Boolean,
    onRead: () -> Unit,
) {
    DeftButton(
        if (details.book.currentLocator == null) "read" else "continue",
        onRead,
        enabled = !busy,
        icon = DeftIcons.Books,
    )
}

@Composable
private fun SavedProgress(details: BookDetails) {
    if (details.book.currentLocator == null) return
    val colors = LocalDeftColors.current
    val saved =
        details.book.totalProgression
            .coerceIn(0.0, 1.0)
            .toFloat()
    val progress by animateFloatAsState(saved, label = "saved book progress")
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("saved position", style = DeftTypography.label, color = colors.muted)
            Text("${(saved * PERCENT_SCALE).roundToInt()}%", style = DeftTypography.mono, color = colors.text)
        }
        LinearProgressIndicator(
            progress = { progress },
            modifier = Modifier.fillMaxWidth(),
            color = colors.accent,
            trackColor = colors.surface2,
            gapSize = 0.dp,
            drawStopIndicator = {},
        )
    }
}

@Composable
private fun BookDescription(description: String?) {
    val plain =
        remember(description) {
            description?.let { Html.fromHtml(it, Html.FROM_HTML_MODE_LEGACY).toString().trim() }.orEmpty()
        }
    if (plain.isNotEmpty()) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            SectionTitle("about this book", DeftIcons.Books)
            val colors = LocalDeftColors.current
            Surface(
                modifier = Modifier.fillMaxWidth().testTag("book_description"),
                shape = DeftShapes.card,
                color = colors.surface,
                border = BorderStroke(1.dp, colors.line),
                shadowElevation = 2.dp,
            ) {
                Text(plain, Modifier.padding(16.dp), style = DeftTypography.body, color = colors.text)
            }
        }
    }
}

@Composable
private fun BookEdition(details: BookDetails) {
    val book = details.book
    val fields =
        listOf(
            "publisher" to book.publisher,
            "published" to book.publishedDate,
            "language" to book.language,
            "isbn" to (book.isbn13 ?: book.isbn10),
        ).filter { !it.second.isNullOrBlank() }
    if (fields.isEmpty()) return
    val colors = LocalDeftColors.current
    DetailSection("edition", DeftActionIcons.Info) {
        fields.forEachIndexed { index, (label, value) ->
            if (index > 0) HorizontalDivider(color = colors.line)
            BoxWithConstraints(Modifier.fillMaxWidth().padding(vertical = 12.dp)) {
                if (
                    maxWidth < 280.dp ||
                    LocalDensity.current.fontScale > STACKED_METADATA_FONT_SCALE
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(label, style = DeftTypography.label, color = colors.muted)
                        Text(value.orEmpty(), style = DeftTypography.body, color = colors.text)
                    }
                } else {
                    Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        Text(label, Modifier.width(88.dp), style = DeftTypography.body, color = colors.muted)
                        Text(value.orEmpty(), Modifier.weight(1f), style = DeftTypography.body, color = colors.text)
                    }
                }
            }
        }
    }
}

@Composable
private fun BookFiles(files: List<BookFileEntity>) {
    val colors = LocalDeftColors.current
    val context = LocalContext.current
    DetailSection(if (files.size == 1) "file" else "files", DeftActionIcons.Folder) {
        if (files.isEmpty()) {
            Text("No file is currently connected.", Modifier.padding(vertical = 12.dp), style = DeftTypography.body)
        }
        files.forEachIndexed { index, file ->
            if (index > 0) HorizontalDivider(color = colors.line)
            Column(Modifier.padding(vertical = 12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(file.displayPath, style = DeftTypography.body, color = colors.text)
                val summary =
                    listOfNotNull(
                        if (file.size >= 0) Formatter.formatShortFileSize(context, file.size) else null,
                        "missing".takeIf { file.missing },
                    ).joinToString(" · ")
                if (summary.isNotEmpty()) Text(summary, style = DeftTypography.mono, color = colors.muted)
            }
        }
    }
    Text("Your original files stay unchanged.", style = DeftTypography.body, color = colors.muted)
}

@Composable
private fun DetailSection(
    title: String,
    icon: ImageVector,
    content: @Composable ColumnScope.() -> Unit,
) {
    val colors = LocalDeftColors.current
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        SectionTitle(title, icon)
        Surface(
            shape = DeftShapes.card,
            color = colors.surface,
            border = BorderStroke(1.dp, colors.line),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Column(Modifier.padding(horizontal = 16.dp), content = content)
        }
    }
}

@Composable
private fun SectionTitle(
    title: String,
    icon: ImageVector,
) {
    val colors = LocalDeftColors.current
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, null, Modifier.size(20.dp), tint = colors.muted)
        Text(title, style = DeftTypography.title, color = colors.text)
    }
}

@Composable
private fun DetailCover(
    details: BookDetails,
    modifier: Modifier,
) {
    val context = LocalContext.current.applicationContext
    val loader = remember(context) { ImageLoader.Builder(context).diskCache(null).build() }
    DisposableEffect(loader) { onDispose { loader.shutdown() } }
    val book = details.book
    val path = book.coverOverridePath ?: book.coverPath
    val cover =
        remember(path) {
            path
                ?.let { value -> runCatching { File(value).canonicalFile }.getOrNull() }
                ?.takeIf { it.isAbsolute && it.toPath().startsWith(context.filesDir.canonicalFile.toPath()) }
        }
    val density = LocalDensity.current
    CoverImage(details.displayTitle(), details.displayAuthors(), modifier.clearAndSetSemantics { }) {
        // This is decorative cover artwork. Accessible title/author labels above honor font scaling.
        CompositionLocalProvider(LocalDensity provides Density(density.density, fontScale = 1f)) {
            GeneratedCover(details.displayTitle(), details.displayAuthors())
        }
        if (cover != null) {
            AsyncImage(
                model = cover,
                contentDescription = null,
                imageLoader = loader,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
            )
        }
    }
}

private fun BookDetails.displayTitle(): String = (book.titleOverride ?: book.title).trim().ifEmpty { "<no-title>" }

private fun BookDetails.displayAuthors(): String = authorsDisplay.trim().ifEmpty { "<no-author>" }

private const val PERCENT_SCALE = 100
private const val STACKED_METADATA_FONT_SCALE = 1.3f
