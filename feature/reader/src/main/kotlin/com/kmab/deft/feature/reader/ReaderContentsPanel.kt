@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.kmab.deft.feature.reader

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.kmab.deft.core.database.entity.BookmarkEntity
import com.kmab.deft.core.designsystem.icons.DeftActionIcons
import org.readium.r2.shared.publication.Link
import org.readium.r2.shared.util.Url

@Composable
internal fun ReaderContentsPanel(
    contents: List<Link>,
    savedBookmarks: List<BookmarkEntity>,
    currentHref: Url?,
    onChapter: (Link) -> Unit,
    onBookmark: (BookmarkEntity) -> Unit,
    onRename: (Long, String) -> Unit,
    onRemove: (Long) -> Unit,
    state: ReaderContentsState = rememberReaderContentsState(),
) {
    val bookmarks = state.bookmarks
    var editing by remember { mutableStateOf<BookmarkEntity?>(null) }
    val tree = remember(contents) { contentsTree(contents) }
    val hasGroups = remember(tree) { tree.any { it.link.children.isNotEmpty() } }
    val active =
        remember(tree, currentHref) {
            tree.firstOrNull { it.link.url() == currentHref }
                ?: tree.firstOrNull { it.link.url().removeFragment() == currentHref?.removeFragment() }
        }
    val chapters = remember(tree, state.collapsed) { tree.filter { row -> row.parents.none { it in state.collapsed } } }
    LaunchedEffect(tree) {
        if (!state.initialized && tree.isNotEmpty()) {
            state.initialized = true
            state.collapsed =
                tree
                    .filter { it.link.children.isNotEmpty() && it.key !in active?.parents.orEmpty() }
                    .map { it.key }
                    .toSet()
            val visible = tree.filter { row -> row.parents.none { it in state.collapsed } }
            val index = visible.indexOfFirst { it.key == active?.key }.coerceAtLeast(0)
            state.chapterList.scrollToItem(index)
        }
    }
    Column(Modifier.fillMaxWidth().heightIn(max = 620.dp).padding(horizontal = 20.dp)) {
        Text("your place in the book", style = MaterialTheme.typography.titleLarge)
        SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth().padding(vertical = 16.dp)) {
            SegmentedButton(!bookmarks, {
                state.bookmarks = false
            }, SegmentedButtonDefaults.itemShape(0, 2), icon = {
                Icon(ReaderIcons.Contents, null, Modifier.size(18.dp))
            }) { Text("contents") }
            SegmentedButton(bookmarks, {
                state.bookmarks = true
            }, SegmentedButtonDefaults.itemShape(1, 2), icon = {
                Icon(ReaderIcons.Bookmark, null, Modifier.size(18.dp))
            }) { Text("bookmarks") }
        }
        LazyColumn(
            Modifier.fillMaxWidth().weight(1f, fill = false).testTag("reader_contents_list"),
            state = if (bookmarks) state.bookmarkList else state.chapterList,
        ) {
            if (bookmarks) {
                if (savedBookmarks.isEmpty()) {
                    item {
                        Text(
                            "No bookmarks yet. Use the bookmark button to save your place.",
                            Modifier.padding(vertical = 20.dp),
                        )
                    }
                }
                itemsIndexed(savedBookmarks, key = { _, item -> item.id }) { _, bookmark ->
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .clickable {
                                onBookmark(bookmark)
                            }.heightIn(min = 64.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(Modifier.weight(1f).padding(vertical = 12.dp)) {
                            Text(bookmark.label, style = MaterialTheme.typography.bodyLarge)
                            Text(
                                "${(bookmark.totalProgression * PERCENT_SCALE).toInt()}%",
                                style = MaterialTheme.typography.labelMedium,
                            )
                        }
                        IconButton(
                            onClick = { editing = bookmark },
                        ) { Icon(ReaderIcons.Rename, "Rename ${bookmark.label}") }
                        IconButton(
                            onClick = { onRemove(bookmark.id) },
                        ) { Icon(DeftActionIcons.Trash, "Remove ${bookmark.label}") }
                    }
                }
            } else {
                if (chapters.isEmpty()) {
                    item {
                        Text(
                            "This book has no table of contents. Use the progress slider to move through it.",
                            Modifier.padding(vertical = 20.dp),
                        )
                    }
                }
                itemsIndexed(chapters, key = { _, row -> row.key }) { _, chapter ->
                    ContentsRow(
                        chapter,
                        active?.key == chapter.key,
                        chapter.key !in state.collapsed,
                        { state.toggle(chapter.key) },
                        { onChapter(chapter.link) },
                        Modifier.animateItem(),
                        reserveDisclosure = hasGroups,
                    )
                }
            }
            item { Spacer(Modifier.height(24.dp)) }
        }
    }
    editing?.let { bookmark ->
        var label by remember(bookmark.id) { mutableStateOf(bookmark.label) }
        Dialog(
            onDismissRequest = { editing = null },
            properties = DialogProperties(usePlatformDefaultWidth = false),
        ) {
            Box(Modifier.fillMaxWidth().imePadding().padding(horizontal = 24.dp), contentAlignment = Alignment.Center) {
                Surface(Modifier.widthIn(max = 560.dp).fillMaxWidth(), shape = MaterialTheme.shapes.large) {
                    Column(
                        Modifier
                            .fillMaxWidth()
                            .heightIn(max = 620.dp)
                            .verticalScroll(rememberScrollState())
                            .padding(24.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                    ) {
                        Text("name this bookmark", style = MaterialTheme.typography.titleLarge)
                        OutlinedTextField(
                            label,
                            { label = it.take(BOOKMARK_LABEL_LIMIT) },
                            label = { Text("bookmark name") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                        )
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                            TextButton(onClick = { editing = null }) { Text("cancel") }
                            TextButton(onClick = {
                                onRename(bookmark.id, label)
                                editing = null
                            }, enabled = label.isNotBlank()) { Text("save") }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ContentsRow(
    row: ContentsEntry,
    active: Boolean,
    expanded: Boolean,
    toggle: () -> Unit,
    open: () -> Unit,
    modifier: Modifier = Modifier,
    reserveDisclosure: Boolean = true,
) {
    val title = row.link.title?.takeIf(String::isNotBlank) ?: "untitled section"
    val angle by animateFloatAsState(if (expanded) EXPANDED_ROTATION else 0f, label = "chapter expansion")
    Row(
        modifier
            .fillMaxWidth()
            .background(if (active) MaterialTheme.colorScheme.secondaryContainer else Color.Transparent)
            .padding(start = (row.depth.coerceAtMost(VISIBLE_TOC_DEPTH) * TOC_INDENT).dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (row.link.children.isNotEmpty()) {
            IconButton(onClick = toggle) {
                Icon(
                    DeftActionIcons.ChevronRight,
                    "${if (expanded) "collapse" else "expand"} $title",
                    Modifier.rotate(angle),
                )
            }
        } else if (reserveDisclosure) {
            // Reserve the disclosure column so a child label is genuinely indented
            // beyond its parent, rather than jumping left when it has no chevron.
            Spacer(Modifier.width(48.dp))
        }
        Text(
            title,
            Modifier
                .weight(1f)
                .heightIn(min = 48.dp)
                .clickable(onClick = open)
                .semantics { selected = active }
                .padding(horizontal = 12.dp, vertical = 14.dp),
            style = MaterialTheme.typography.bodyLarge,
            color = if (active) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
        )
    }
}

internal data class ContentsEntry(
    val key: String,
    val link: Link,
    val depth: Int,
    val parents: List<String>,
)

internal fun contentsTree(
    links: List<Link>,
    parents: List<String> = emptyList(),
): List<ContentsEntry> =
    links.flatMapIndexed { index, link ->
        val key = (parents.lastOrNull()?.plus("/") ?: "") + index
        listOf(ContentsEntry(key, link, parents.size, parents)) +
            if (parents.size < MAX_TOC_DEPTH) contentsTree(link.children, parents + key) else emptyList()
    }

@Stable
internal class ReaderContentsState(
    val chapterList: LazyListState = LazyListState(),
    val bookmarkList: LazyListState = LazyListState(),
) {
    var bookmarks by mutableStateOf(false)
    var collapsed by mutableStateOf(emptySet<String>())
    var initialized by mutableStateOf(false)

    fun toggle(key: String) {
        collapsed = if (key in collapsed) collapsed - key else collapsed + key
    }
}

@Composable
internal fun rememberReaderContentsState(): ReaderContentsState =
    rememberSaveable(saver = ContentsStateSaver) {
        ReaderContentsState()
    }

@Suppress("MagicNumber") // Fixed positions in the saved-state tuple.
private val ContentsStateSaver =
    listSaver<ReaderContentsState, Any>(
        save = {
            listOf(
                it.chapterList.firstVisibleItemIndex,
                it.chapterList.firstVisibleItemScrollOffset,
                it.bookmarkList.firstVisibleItemIndex,
                it.bookmarkList.firstVisibleItemScrollOffset,
                it.bookmarks,
                it.collapsed.joinToString(","),
                it.initialized,
            )
        },
        restore = {
            ReaderContentsState(
                LazyListState(it[0] as Int, it[1] as Int),
                LazyListState(it[2] as Int, it[3] as Int),
            ).apply {
                bookmarks = it[4] as Boolean
                collapsed = (it[5] as String).split(',').filter(String::isNotBlank).toSet()
                initialized = it[6] as Boolean
            }
        },
    )

private const val VISIBLE_TOC_DEPTH = 4
private const val TOC_INDENT = 24
private const val PERCENT_SCALE = 100
private const val BOOKMARK_LABEL_LIMIT = 240
private const val MAX_TOC_DEPTH = 12

private const val EXPANDED_ROTATION = 90f
