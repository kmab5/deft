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
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsDraggedAsState
import androidx.compose.foundation.interaction.collectIsPressedAsState
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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
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
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.collectAsStateWithLifecycle
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
import org.readium.r2.shared.util.Url
import java.util.Date

private enum class ReaderSheet { CONTENTS, SETTINGS, SEARCH }

@Composable
internal fun ReaderScreen(
    model: ReaderViewModel,
    activity: ReaderActivity,
) {
    val state by model.state.collectAsStateWithLifecycle()
    val tracking by model.trackingState.collectAsStateWithLifecycle()
    val trackingPreferences by model.trackingPreferences.collectAsStateWithLifecycle()
    val preferences by model.preferences.collectAsStateWithLifecycle()
    val themes by model.themes.collectAsStateWithLifecycle()
    val systemDark = isSystemInDarkTheme()
    val theme = preferences.activeTheme(systemDark, themes)
    var chrome by rememberSaveable { mutableStateOf(true) }
    var sheet by rememberSaveable { mutableStateOf<ReaderSheet?>(null) }
    val scope = rememberCoroutineScope()
    val notices = rememberDeftNoticeState()
    val session = state.session
    val contentsState = rememberReaderContentsState()
    val progressPositions = remember(session) { session?.progressPositions().orEmpty() }
    var imageHref by rememberSaveable { mutableStateOf<String?>(null) }
    var externalHref by rememberSaveable { mutableStateOf<String?>(null) }
    var chromeInteraction by remember { mutableStateOf(0) }
    var scrubbing by remember { mutableStateOf(false) }
    val navigator =
        remember(session) {
            session?.let {
                ReaderNavigator(
                    activity,
                    model,
                    it,
                    onImage = { url -> imageHref = url.toString() },
                    onExternalLink = { url -> externalHref = url },
                ) {
                    chrome = !chrome
                    chromeInteraction++
                }
            }
        }
    LaunchedEffect(model) { model.notices.collect { notices.show(it) } }
    ReaderTrackingEffects(model, activity, state, tracking, listOf(sheet, imageHref, externalHref).any { it != null })
    ReaderNavigationEffects(
        activity = activity,
        model = model,
        navigator = navigator,
        state = state,
        preferences = preferences,
        theme = theme,
        chrome = chrome,
        sheetOpen = sheet != null,
        imageOpen = imageHref != null || externalHref != null,
        scrubbing = scrubbing,
        interaction = chromeInteraction,
        hideChrome = { chrome = false },
        dismissSheet = {
            model.search.cancel()
            sheet = null
        },
    )

    ReaderTheme(theme) {
        CompositionLocalProvider(LocalDeftNoticeState provides notices) {
            Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
                if (session != null && navigator != null) {
                    Column(
                        Modifier
                            .fillMaxSize()
                            .displayCutoutPadding()
                            .then(
                                if (preferences.fullScreen) {
                                    Modifier.padding(top = 8.dp)
                                } else {
                                    Modifier.systemBarsPadding().padding(
                                        top = 12.dp,
                                    )
                                },
                            ),
                    ) {
                        AndroidView(
                            factory = { context ->
                                ReaderContainer(
                                    context,
                                    model.interaction::userInput,
                                    { navigator.handlesSwipes },
                                    navigator::swipe,
                                    { navigator.nativeSwipes },
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
                        ReaderTrackingIndicator(trackingPreferences.showIndicator, state.rendered, tracking.active)
                        if (preferences.showStatusLine) {
                            ReaderStatus(state, progressPositions, preferences.showPageNumber, Modifier.fillMaxWidth())
                        }
                    }
                    ReaderChrome(
                        visible = chrome,
                        state = state.chromeState(),
                        positions = progressPositions,
                        showLocations = preferences.showPageNumber,
                        onInteraction = { chromeInteraction++ },
                        onScrubbing = { scrubbing = it },
                        onSearch = { sheet = ReaderSheet.SEARCH },
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
            ReaderSheets(sheet, state, model, navigator, contentsState) {
                model.search.cancel()
                sheet = null
            }
            ReaderTrackingDialogs(
                opening = state.openingChoice,
                finishPrompt = tracking.finishPrompt,
                busy = state.choosing,
                choose = model::chooseReading,
                close = activity::finish,
                finish = model::finishReading,
                decline = model::declineFinish,
            )
            ReaderOverlayWindows(
                session,
                imageHref,
                externalHref,
                closeImage = { imageHref = null },
                closeExternal = { externalHref = null },
                unavailable = { notices.show("This link could not be opened. Check that a browser is installed.") },
            )
        }
    }
}

@Composable
private fun ReaderOverlayWindows(
    session: ReaderSession?,
    imageHref: String?,
    externalHref: String?,
    closeImage: () -> Unit,
    closeExternal: () -> Unit,
    unavailable: () -> Unit,
) {
    imageHref?.let { href ->
        session?.opened?.publication?.let { publication ->
            Url(href)?.let { ReaderImageViewer(publication, it, closeImage) }
        }
    }
    externalHref?.let { ReaderExternalLinkDialog(it, closeExternal, unavailable) }
}

@Composable
private fun ReaderNavigationEffects(
    activity: ReaderActivity,
    model: ReaderViewModel,
    navigator: ReaderNavigator?,
    state: ReaderState,
    preferences: ReaderPreferences,
    theme: ReaderTheme,
    chrome: Boolean,
    sheetOpen: Boolean,
    imageOpen: Boolean,
    scrubbing: Boolean,
    interaction: Int,
    hideChrome: () -> Unit,
    dismissSheet: () -> Unit,
) {
    SideEffect {
        activity.navigator = navigator
        navigator?.apply(preferences, theme)
    }
    DisposableEffect(navigator) {
        onDispose {
            navigator?.detach()
            if (activity.navigator === navigator) activity.navigator = null
        }
    }
    val dark = Color(parseColor(theme.background)).luminance() < DARK_LUMINANCE
    val tracking by model.trackingState.collectAsStateWithLifecycle()
    ReaderWindowEffect(
        activity,
        preferences,
        chrome || sheetOpen,
        dark,
        tracking.keepAwake && state.rendered && !tracking.finishPrompt,
    )
    val overlayOpen = sheetOpen || imageOpen
    val readingIdle = state.rendered && !scrubbing
    LaunchedEffect(chrome, overlayOpen, state.rendered, interaction, scrubbing) {
        if (chrome && !overlayOpen && readingIdle) {
            delay(CHROME_TIMEOUT_MILLIS)
            hideChrome()
        }
    }
    BackHandler(!overlayOpen && state.canReturn) {
        model.interaction.returnPoint()?.let { navigator?.go(it, remember = false) }
    }
    BackHandler(sheetOpen, onBack = dismissSheet)
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
    val surface = text.copy(alpha = SURFACE_TINT_ALPHA).compositeOver(background)
    val surface2 = text.copy(alpha = SURFACE_TINT_ALPHA).compositeOver(surface)
    val controlText = readableControlColor(text, surface2)
    val controlAccent = readableControlColor(accent, surface2)
    val colors =
        base.copy(
            bg = background,
            surface = surface,
            surface2 = surface2,
            text = controlText,
            muted = controlText.copy(alpha = 0.85f),
            accent = controlAccent,
            accentText = controlAccent,
            onAccent = background,
            line = text.copy(alpha = 0.15f),
            line2 = text.copy(alpha = DARK_LUMINANCE),
        )
    val scheme =
        (if (dark) darkColorScheme() else lightColorScheme()).copy(
            primary = controlAccent,
            onPrimary = background,
            background = background,
            onBackground = text,
            surface = surface,
            onSurface = controlText,
            surfaceVariant = colors.surface2,
            onSurfaceVariant = colors.muted,
            secondaryContainer = colors.surface2,
            onSecondaryContainer = controlText,
            outline = colors.line2,
            surfaceTint = surface,
        )
    CompositionLocalProvider(LocalDeftColors provides colors, LocalContentColor provides text) {
        MaterialTheme(
            colorScheme = scheme,
            typography = DeftTypography.material,
            shapes = DeftShapes.material,
            content = content,
        )
    }
}

/** Keep custom reader hues while ensuring labels still contrast against raised controls. */
private fun readableControlColor(
    color: Color,
    background: Color,
): Color {
    fun contrast(candidate: Color): Float =
        (maxOf(candidate.luminance(), background.luminance()) + WCAG_LUMINANCE_OFFSET) /
            (minOf(candidate.luminance(), background.luminance()) + WCAG_LUMINANCE_OFFSET)
    if (contrast(color) >= MIN_CONTROL_CONTRAST) return color
    val target = if (contrast(Color.White) > contrast(Color.Black)) Color.White else Color.Black
    return (1..CONTROL_COLOR_STEPS)
        .map { lerp(color, target, it.toFloat() / CONTROL_COLOR_STEPS) }
        .firstOrNull { contrast(it) >= MIN_CONTROL_CONTRAST } ?: target
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
        title = session?.title.orEmpty(),
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
    onSearch: () -> Unit = {},
    positions: List<org.readium.r2.shared.publication.Locator> = emptyList(),
    showLocations: Boolean = false,
    onInteraction: () -> Unit = {},
    onScrubbing: (Boolean) -> Unit = {},
) {
    val sliderInteraction = remember { MutableInteractionSource() }
    val dragging by sliderInteraction.collectIsDraggedAsState()
    val pressing by sliderInteraction.collectIsPressedAsState()
    LaunchedEffect(dragging, pressing) { onScrubbing(dragging || pressing) }
    DisposableEffect(Unit) { onDispose { onScrubbing(false) } }
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
                    Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .heightIn(min = 72.dp)
                        .padding(horizontal = 4.dp, vertical = 8.dp),
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
                        style = MaterialTheme.typography.headlineSmall,
                    )
                    IconButton(onClick = onSearch) { Icon(ReaderIcons.Search, "Search this book") }
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
                Column(
                    Modifier.fillMaxWidth().navigationBarsPadding().padding(
                        start = 18.dp,
                        end = 18.dp,
                        top = 16.dp,
                        bottom = 4.dp,
                    ),
                ) {
                    if (state.canReturn) {
                        TextButton(onClick = {
                            onInteraction()
                            onReturn()
                        }) {
                            Icon(ReaderIcons.Return, null, Modifier.size(16.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("go back to where I was")
                        }
                    }
                    val target = progressTarget(positions, slider ?: progress)
                    Row(
                        Modifier.fillMaxWidth().padding(horizontal = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            if (slider !=
                                null
                            ) {
                                target?.title?.takeIf(String::isNotBlank) ?: state.section
                            } else {
                                state.section
                            },
                            Modifier.weight(1f).padding(end = 12.dp),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            style = MaterialTheme.typography.labelMedium,
                        )
                        Text(
                            progressLabel(slider ?: progress, positions, showLocations),
                            style = MaterialTheme.typography.labelMedium,
                        )
                    }
                    Slider(
                        value = slider ?: progress,
                        onValueChange = {
                            slider = it
                            onInteraction()
                        },
                        onValueChangeFinished = {
                            slider?.let(onProgress)
                            slider = null
                        },
                        enabled = state.rendered,
                        interactionSource = sliderInteraction,
                        thumb = {
                            // Center the dot inside the full-height, top-aligned slider thumb wrapper.
                            Box(Modifier.size(width = 12.dp, height = 32.dp), contentAlignment = Alignment.Center) {
                                Surface(
                                    Modifier.size(12.dp).testTag("reader-progress-thumb"),
                                    shape = androidx.compose.foundation.shape.CircleShape,
                                    color = MaterialTheme.colorScheme.primary,
                                ) {}
                            }
                        },
                        track = { sliderState ->
                            val active = MaterialTheme.colorScheme.primary
                            val inactive = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.18f)
                            Canvas(Modifier.fillMaxWidth().height(32.dp).testTag("reader-progress-track")) {
                                drawLine(inactive, Offset(0f, center.y), Offset(size.width, center.y), 3.dp.toPx())
                                drawLine(
                                    active,
                                    Offset(0f, center.y),
                                    Offset(size.width * sliderState.value, center.y),
                                    3.dp.toPx(),
                                )
                            }
                        },
                        modifier =
                            Modifier.fillMaxWidth().height(32.dp).semantics {
                                contentDescription =
                                    "Book progress"
                            },
                    )
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

private fun sectionLabel(state: ReaderState): String =
    chapterLabel(
        state.locator,
        state.session
            ?.opened
            ?.publication
            ?.tableOfContents
            .orEmpty(),
        state.session
            ?.opened
            ?.publication
            ?.readingOrder
            .orEmpty(),
    )

@Composable
private fun ReaderStatus(
    state: ReaderState,
    positions: List<org.readium.r2.shared.publication.Locator>,
    showLocations: Boolean,
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
    Row(modifier.padding(horizontal = 24.dp, vertical = 6.dp), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(
            progressLabel((state.locator?.locations?.totalProgression ?: 0.0).toFloat(), positions, showLocations),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onBackground,
        )
        Text(status, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onBackground)
    }
}

@Composable
private fun ReaderWindowEffect(
    activity: ReaderActivity,
    preferences: ReaderPreferences,
    chromeVisible: Boolean,
    dark: Boolean,
    keepAwake: Boolean,
) {
    DisposableEffect(preferences, chromeVisible, dark, keepAwake) {
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
    contentsState: ReaderContentsState,
    dismiss: () -> Unit,
) {
    if (sheet == null) return
    ModalBottomSheet(
        onDismissRequest = dismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = MaterialTheme.colorScheme.surface,
    ) {
        when (sheet) {
            ReaderSheet.SEARCH -> {
                val search by model.search.state.collectAsStateWithLifecycle()
                val advanced by model.advancedSearch.collectAsStateWithLifecycle()
                ReaderSearchPanel(
                    search,
                    model.search::edit,
                    {
                        navigator?.go(it)
                        dismiss()
                    },
                    submit = { model.search.submit(state.session?.opened?.publication) },
                    advanced = advanced,
                    options = model.search::options,
                )
            }
            ReaderSheet.SETTINGS -> ReaderPreferencesPanel(Modifier.fillMaxWidth(), manageAssets = false)
            ReaderSheet.CONTENTS ->
                ReaderContentsPanel(
                    state = contentsState,
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
private const val SURFACE_TINT_ALPHA = 0.12f
private const val MIN_CONTROL_CONTRAST = 4.5f
private const val CONTROL_COLOR_STEPS = 20
private const val WCAG_LUMINANCE_OFFSET = 0.05f
private const val PERCENT_SCALE = 100
private const val STATUS_REFRESH_MILLIS = 30_000L

private const val CHROME_TIMEOUT_MILLIS = 4_000L
