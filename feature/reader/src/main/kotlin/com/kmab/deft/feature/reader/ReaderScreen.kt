@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.kmab.deft.feature.reader

import android.content.Intent
import android.content.IntentFilter
import android.graphics.Color.parseColor
import android.os.BatteryManager
import android.text.format.DateFormat
import android.view.View
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.displayCutoutPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.kmab.deft.core.database.entity.BookmarkEntity
import com.kmab.deft.core.designsystem.DeftColors
import com.kmab.deft.core.designsystem.DeftNoticeHost
import com.kmab.deft.core.designsystem.DeftShapes
import com.kmab.deft.core.designsystem.DeftTypography
import com.kmab.deft.core.designsystem.LocalDeftColors
import com.kmab.deft.core.designsystem.LocalDeftNoticeState
import com.kmab.deft.core.designsystem.icons.DeftActionIcons
import com.kmab.deft.core.designsystem.rememberDeftNoticeState
import com.kmab.deft.core.model.ReaderPreferences
import com.kmab.deft.core.model.ReaderTheme
import com.kmab.deft.feature.readersettings.ReaderPreferencesPanel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.readium.r2.shared.publication.Link
import org.readium.r2.shared.util.Url
import java.util.Date

private enum class ReaderSheet { CONTENTS, SETTINGS }

@Composable
internal fun ReaderScreen(
    model: ReaderViewModel,
    activity: ReaderActivity,
) {
    val state by model.state.collectAsStateWithLifecycle()
    val preferences by model.preferences.collectAsStateWithLifecycle()
    val themes by model.themes.collectAsStateWithLifecycle()
    val systemDark = isSystemInDarkTheme()
    val theme = preferences.activeTheme(systemDark, themes)
    var chrome by rememberSaveable { mutableStateOf(true) }
    var sheet by rememberSaveable { mutableStateOf<ReaderSheet?>(null) }
    val scope = rememberCoroutineScope()
    val notices = rememberDeftNoticeState()
    val session = state.session
    val navigator = remember(session) { session?.let { ReaderNavigator(activity, model, it) { chrome = !chrome } } }
    val dark = Color(parseColor(theme.background)).luminance() < DARK_LUMINANCE

    SideEffect {
        activity.navigator = navigator
        navigator?.apply(preferences, theme)
    }
    DisposableEffect(navigator) {
        onDispose {
            navigator?.detach()
            if (activity.navigator ===
                navigator
            ) {
                activity.navigator = null
            }
        }
    }
    ReaderWindowEffect(activity, preferences, chrome || sheet != null, dark)
    LaunchedEffect(model) { model.notices.collect { notices.show(it) } }
    BackHandler(sheet != null) { sheet = null }

    ReaderTheme(theme) {
        CompositionLocalProvider(LocalDeftNoticeState provides notices) {
            Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
                if (session != null && navigator != null) {
                    Column(Modifier.fillMaxSize().displayCutoutPadding()) {
                        AndroidView(
                            factory = { context ->
                                ReaderContainer(
                                    context,
                                    model.interaction::userInput,
                                    { navigator.handlesSwipes },
                                    navigator::swipe,
                                ).apply {
                                    id = R.id.reader_navigator
                                    addOnAttachStateChangeListener(
                                        object : View.OnAttachStateChangeListener {
                                            override fun onViewAttachedToWindow(view: View) {
                                                navigator.attach(this@apply, preferences, theme)
                                            }

                                            override fun onViewDetachedFromWindow(view: View) = Unit
                                        },
                                    )
                                }
                            },
                            modifier = Modifier.weight(1f).fillMaxWidth(),
                        )
                        if (preferences.showStatusLine) ReaderStatus(state, Modifier.fillMaxWidth())
                    }
                    ReaderChrome(
                        visible = chrome,
                        state = state.chromeState(),
                        onBack = activity::finish,
                        onContents = { sheet = ReaderSheet.CONTENTS },
                        onSettings = { sheet = ReaderSheet.SETTINGS },
                        onBookmark = model::toggleBookmark,
                        onProgress = { progress ->
                            scope.launch { model.progressionLocator(progress)?.let { navigator.go(it) } }
                        },
                        onReturn = { model.interaction.returnPoint()?.let { navigator.go(it, remember = false) } },
                    )
                }
                if (state.loading) {
                    Column(
                        Modifier.align(Alignment.Center),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                    ) {
                        CircularProgressIndicator(Modifier.size(32.dp))
                        Text("Opening your book…")
                    }
                }
                state.error?.let { error ->
                    Column(
                        Modifier.align(Alignment.Center).padding(28.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                    ) {
                        Icon(DeftActionIcons.Warning, null, Modifier.size(32.dp))
                        Text("unable to open this book", style = MaterialTheme.typography.titleLarge)
                        Text(error)
                        FilledTonalButton(onClick = activity::finish) { Text("back to library") }
                    }
                }
                DeftNoticeHost(notices, Modifier.align(Alignment.BottomCenter).navigationBarsPadding())
            }
            ReaderSheets(sheet, state, model, navigator) { sheet = null }
        }
    }
}

@Composable
internal fun ReaderTheme(
    theme: ReaderTheme,
    content: @Composable () -> Unit,
) {
    val background by animateColorAsState(Color(parseColor(theme.background)), label = "reader paper")
    val text by animateColorAsState(Color(parseColor(theme.text)), label = "reader ink")
    val accent by animateColorAsState(Color(parseColor(theme.accent)), label = "reader accent")
    val dark = background.luminance() < DARK_LUMINANCE
    val base = if (dark) DeftColors.Ink else DeftColors.Paper
    val colors =
        base.copy(
            bg = background,
            surface = background,
            surface2 = text.copy(alpha = SURFACE_TINT_ALPHA).compositeOver(background),
            text = text,
            muted = text.copy(alpha = 0.75f),
            accent = accent,
            accentText = accent,
            onAccent = background,
            line = text.copy(alpha = 0.15f),
            line2 = text.copy(alpha = DARK_LUMINANCE),
        )
    val scheme =
        (if (dark) darkColorScheme() else lightColorScheme()).copy(
            primary = accent,
            onPrimary = background,
            background = background,
            onBackground = text,
            surface = background,
            onSurface = text,
            surfaceVariant = colors.surface2,
            onSurfaceVariant = colors.muted,
            outline = colors.line2,
            surfaceTint = background,
        )
    CompositionLocalProvider(LocalDeftColors provides colors) {
        MaterialTheme(
            colorScheme = scheme,
            typography = DeftTypography.material,
            shapes = DeftShapes.material,
            content = content,
        )
    }
}

internal data class ReaderChromeState(
    val title: String,
    val progress: Float,
    val section: String,
    val ticks: List<Float>,
    val bookmarked: Boolean,
    val rendered: Boolean,
    val canReturn: Boolean,
)

private fun ReaderState.chromeState() =
    ReaderChromeState(
        title = locator?.title?.takeIf { it.isNotBlank() } ?: session?.title.orEmpty(),
        progress = (locator?.locations?.totalProgression ?: 0.0).toFloat().coerceIn(0f, 1f),
        section = sectionLabel(this),
        ticks = session?.let(::chapterTicks).orEmpty(),
        bookmarked = locator?.let { place -> bookmarks.any { bookmarkMatches(it, place) } } == true,
        rendered = rendered,
        canReturn = canReturn,
    )

@Composable
internal fun ReaderChrome(
    visible: Boolean,
    state: ReaderChromeState,
    onBack: () -> Unit,
    onContents: () -> Unit,
    onSettings: () -> Unit,
    onBookmark: () -> Unit,
    onProgress: (Float) -> Unit,
    onReturn: () -> Unit,
) {
    val progress = state.progress
    var slider by remember { mutableStateOf<Float?>(null) }
    val bookmarked = state.bookmarked
    Box(Modifier.fillMaxSize()) {
        AnimatedVisibility(
            visible,
            Modifier.align(Alignment.TopCenter),
            enter = fadeIn() + slideInVertically { -it },
            exit =
                fadeOut() + slideOutVertically { -it },
        ) {
            Surface(shadowElevation = 3.dp) {
                Row(
                    Modifier.fillMaxWidth().statusBarsPadding().padding(horizontal = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    IconButton(onClick = onBack) { Icon(DeftActionIcons.Back, "Close reader") }
                    Text(
                        state.title,
                        Modifier.weight(
                            1f,
                        ),
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        style = MaterialTheme.typography.titleSmall,
                    )
                    IconButton(onClick = onContents) { Icon(ReaderIcons.Contents, "Contents and bookmarks") }
                    IconButton(onClick = onBookmark, enabled = state.rendered) {
                        Icon(
                            if (bookmarked) DeftActionIcons.Check else ReaderIcons.Bookmark,
                            if (bookmarked) "Remove bookmark" else "Add bookmark",
                        )
                    }
                    IconButton(onClick = onSettings) { Icon(ReaderIcons.Settings, "Reading settings") }
                }
            }
        }
        AnimatedVisibility(
            visible,
            Modifier.align(Alignment.BottomCenter),
            enter = fadeIn() + slideInVertically { it },
            exit =
                fadeOut() + slideOutVertically { it },
        ) {
            Surface(shadowElevation = 3.dp) {
                Column(Modifier.fillMaxWidth().navigationBarsPadding().padding(horizontal = 20.dp, vertical = 12.dp)) {
                    if (state.canReturn) {
                        TextButton(onClick = onReturn) {
                            Icon(ReaderIcons.Return, null, Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("go back to where I was")
                        }
                    }
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(state.section, style = MaterialTheme.typography.labelMedium)
                        Text(
                            "${((slider ?: progress) * PERCENT_SCALE).toInt()}%",
                            style = MaterialTheme.typography.labelMedium,
                        )
                    }
                    Box {
                        Slider(
                            value = slider ?: progress,
                            onValueChange = { slider = it },
                            onValueChangeFinished = {
                                slider?.let(onProgress)
                                slider = null
                            },
                            enabled = state.rendered,
                            modifier =
                                Modifier.semantics {
                                    contentDescription =
                                        "Book progress"
                                },
                        )
                        val ticks = state.ticks
                        val tickColor = MaterialTheme.colorScheme.onSurfaceVariant
                        Canvas(
                            Modifier
                                .fillMaxWidth()
                                .height(
                                    4.dp,
                                ).align(Alignment.BottomCenter)
                                .padding(horizontal = 10.dp),
                        ) {
                            ticks.forEach {
                                drawLine(
                                    tickColor,
                                    Offset(size.width * it, 0f),
                                    Offset(size.width * it, size.height),
                                    strokeWidth = 1.dp.toPx(),
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun chapterTicks(session: ReaderSession): List<Float> =
    session.positions
        .distinctBy {
            it.href
        }.mapNotNull { it.locations.totalProgression?.toFloat() }

private fun sectionLabel(state: ReaderState): String {
    val links =
        state.session
            ?.opened
            ?.publication
            ?.readingOrder
            .orEmpty()
    val index = links.indexOfFirst { it.url().removeFragment() == state.locator?.href?.removeFragment() }
    return if (index >= 0) "section ${index + 1} of ${links.size}" else "reading progress"
}

@Composable
private fun ReaderStatus(
    state: ReaderState,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val status by produceState(initialValue = "") {
        while (true) {
            val battery = context.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
            val level = battery?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
            val scale = battery?.getIntExtra(BatteryManager.EXTRA_SCALE, -1) ?: -1
            val percentage = if (level >= 0 && scale > 0) "${level * PERCENT_SCALE / scale}%" else ""
            value = "${DateFormat.getTimeFormat(context).format(Date())}  ·  $percentage"
            delay(STATUS_REFRESH_MILLIS)
        }
    }
    Row(modifier.padding(horizontal = 18.dp, vertical = 4.dp), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(
            "${((state.locator?.locations?.progression ?: 0.0) * PERCENT_SCALE).toInt()}% of section",
            style = MaterialTheme.typography.labelSmall,
        )
        Text(status, style = MaterialTheme.typography.labelSmall)
    }
}

@Composable
internal fun ReaderContents(
    contents: List<Link>,
    savedBookmarks: List<BookmarkEntity>,
    currentHref: Url?,
    onChapter: (Link) -> Unit,
    onBookmark: (BookmarkEntity) -> Unit,
    onRename: (Long, String) -> Unit,
    onRemove: (Long) -> Unit,
) {
    var bookmarks by rememberSaveable { mutableStateOf(false) }
    var editing by remember { mutableStateOf<BookmarkEntity?>(null) }
    val chapters = remember(contents) { flattenContents(contents) }
    Column(Modifier.fillMaxWidth().heightIn(max = 620.dp).padding(horizontal = 20.dp)) {
        Text("your place in the book", style = MaterialTheme.typography.titleLarge)
        SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth().padding(vertical = 16.dp)) {
            SegmentedButton(!bookmarks, {
                bookmarks = false
            }, SegmentedButtonDefaults.itemShape(0, 2), icon = {
                Icon(ReaderIcons.Contents, null, Modifier.size(18.dp))
            }) { Text("contents") }
            SegmentedButton(bookmarks, {
                bookmarks = true
            }, SegmentedButtonDefaults.itemShape(1, 2), icon = {
                Icon(ReaderIcons.Bookmark, null, Modifier.size(18.dp))
            }) { Text("bookmarks") }
        }
        LazyColumn(Modifier.fillMaxWidth().weight(1f, fill = false)) {
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
                itemsIndexed(chapters) { _, chapter ->
                    Text(
                        chapter.first.title?.takeIf { it.isNotBlank() } ?: "untitled section",
                        Modifier.fillMaxWidth().clickable { onChapter(chapter.first) }.heightIn(min = 48.dp).padding(
                            start =
                                (
                                    chapter.second *
                                        16
                                ).dp,
                            top = 14.dp,
                            bottom = 14.dp,
                        ),
                        style = MaterialTheme.typography.bodyLarge,
                        color =
                            if (chapter.first.url().removeFragment() ==
                                currentHref?.removeFragment()
                            ) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.onSurface
                            },
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

private fun flattenContents(
    links: List<Link>,
    depth: Int = 0,
): List<Pair<Link, Int>> =
    links.flatMap {
        listOf(it to depth) +
            if (depth < MAX_TOC_DEPTH) flattenContents(it.children, depth + 1) else emptyList()
    }

@Composable
private fun ReaderWindowEffect(
    activity: ReaderActivity,
    preferences: ReaderPreferences,
    chromeVisible: Boolean,
    dark: Boolean,
) {
    DisposableEffect(preferences, chromeVisible, dark) {
        val observer =
            LifecycleEventObserver { _, event ->
                if (event == Lifecycle.Event.ON_RESUME) activity.applyWindow(chromeVisible, dark)
            }
        activity.lifecycle.addObserver(observer)
        if (activity.lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)) {
            activity.applyWindow(chromeVisible, dark)
        }
        onDispose { activity.lifecycle.removeObserver(observer) }
    }
}

@Composable
private fun ReaderSheets(
    sheet: ReaderSheet?,
    state: ReaderState,
    model: ReaderViewModel,
    navigator: ReaderNavigator?,
    dismiss: () -> Unit,
) {
    if (sheet == null) return
    ModalBottomSheet(
        onDismissRequest = dismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = MaterialTheme.colorScheme.surface,
    ) {
        when (sheet) {
            ReaderSheet.SETTINGS -> ReaderPreferencesPanel(Modifier.fillMaxWidth(), manageAssets = false)
            ReaderSheet.CONTENTS ->
                ReaderContents(
                    contents =
                        state.session
                            ?.opened
                            ?.publication
                            ?.tableOfContents
                            .orEmpty(),
                    savedBookmarks = state.bookmarks,
                    currentHref = state.locator?.href,
                    onChapter = {
                        navigator?.go(it)
                        dismiss()
                    },
                    onBookmark = { bookmark ->
                        parseLocator(bookmark.locator)?.let { navigator?.go(it) }
                        dismiss()
                    },
                    onRename = model::renameBookmark,
                    onRemove = model::removeBookmark,
                )
        }
    }
}

private const val DARK_LUMINANCE = 0.3f
private const val SURFACE_TINT_ALPHA = 0.08f
private const val PERCENT_SCALE = 100
private const val STATUS_REFRESH_MILLIS = 30_000L
private const val BOOKMARK_LABEL_LIMIT = 240
private const val MAX_TOC_DEPTH = 12
