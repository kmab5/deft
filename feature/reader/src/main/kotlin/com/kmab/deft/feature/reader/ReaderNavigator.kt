@file:OptIn(org.readium.r2.shared.ExperimentalReadiumApi::class)

package com.kmab.deft.feature.reader

import android.animation.ValueAnimator
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.view.ActionMode
import android.view.Menu
import android.view.MenuItem
import android.view.MotionEvent
import android.view.View
import android.view.ViewConfiguration
import android.webkit.WebView
import android.widget.FrameLayout
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.lifecycleScope
import com.kmab.deft.core.model.PageAnimation
import com.kmab.deft.core.model.ReaderPreferences
import com.kmab.deft.core.model.ReaderTheme
import com.kmab.deft.format.readium.localPublicationUrl
import com.kmab.deft.format.readium.navigatorConfiguration
import com.kmab.deft.format.readium.toNavigatorPreferences
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import org.readium.r2.navigator.HyperlinkNavigator
import org.readium.r2.navigator.epub.EpubNavigatorFactory
import org.readium.r2.navigator.epub.EpubNavigatorFragment
import org.readium.r2.navigator.input.InputListener
import org.readium.r2.navigator.input.TapEvent
import org.readium.r2.shared.publication.Link
import org.readium.r2.shared.publication.Locator
import org.readium.r2.shared.util.AbsoluteUrl
import org.readium.r2.shared.util.Url
import org.readium.r2.shared.util.data.ReadError

internal class ReaderNavigator(
    private val activity: FragmentActivity,
    private val model: ReaderViewModel,
    private val session: ReaderSession,
    private val onImage: (Url) -> Unit = {},
    private val toggleChrome: () -> Unit,
) {
    private var fragment: EpubNavigatorFragment? = null
    private var continuous: ContinuousReaderView? = null
    private var container: FrameLayout? = null
    private var collection: Job? = null
    private var generation = 0L
    private var preferences = ReaderPreferences()
    private var theme: ReaderTheme? = null
    private var selecting = false
    val handlesSwipes: Boolean
        get() =
            !preferences.scroll && !selecting

    fun attach(
        container: FrameLayout,
        initialPreferences: ReaderPreferences,
        initialTheme: ReaderTheme,
    ) {
        if (fragment != null || continuous != null || activity.isFinishing) return
        this.container = container
        generation = model.interaction.attachNavigator()
        preferences = initialPreferences
        theme = initialTheme
        if (initialPreferences.scroll) {
            attachContinuous(container, initialPreferences, initialTheme)
            return
        }
        val configuration =
            navigatorConfiguration(session.fonts, initialTheme).apply {
                selectionActionModeCallback = selectionCallback()
            }
        val factory =
            EpubNavigatorFactory(session.opened.publication).createFragmentFactory(
                initialLocator = model.state.value.locator ?: session.initialLocator,
                initialPreferences = initialPreferences.toNavigatorPreferences(initialTheme),
                configuration = configuration,
                listener =
                    object : EpubNavigatorFragment.Listener {
                        override fun onExternalLinkActivated(url: AbsoluteUrl) {
                            model.notice("External links stay closed while reading offline.")
                        }

                        override fun shouldFollowInternalLink(
                            link: Link,
                            context: HyperlinkNavigator.LinkContext?,
                        ): Boolean {
                            model.interaction.beforeJump()
                            return true
                        }

                        override fun onResourceLoadFailed(
                            href: Url,
                            error: ReadError,
                        ) {
                            model.notice(
                                "A part of this book could not be displayed. Try the next chapter from Contents.",
                            )
                        }
                    },
                paginationListener =
                    object : EpubNavigatorFragment.PaginationListener {
                        override fun onPageLoaded() {
                            fragment?.let {
                                hardenWebViews(it.view)
                                model.interaction.rendered(generation, it.currentLocator.value)
                                applyLinkColor(activity, fragment, theme)
                            }
                        }
                    },
            )
        val navigator =
            factory.instantiate(
                activity.classLoader,
                EpubNavigatorFragment::class.java.name,
            ) as EpubNavigatorFragment
        fragment = navigator
        navigator.addInputListener(
            object : InputListener {
                override fun onTap(event: TapEvent): Boolean {
                    val width = navigator.view?.width ?: return false
                    activity.lifecycleScope.launch {
                        val image = navigator.imageAt(event, width, session.opened.publication)
                        if (image != null) {
                            onImage(image)
                        } else {
                            val edge = width * TAP_EDGE_FRACTION
                            if (!preferences.tapToTurn || event.point.x in edge..(width - edge)) {
                                toggleChrome()
                            } else {
                                turnPhysical(right = event.point.x > width / 2)
                            }
                        }
                    }
                    return true
                }
            },
        )
        activity.supportFragmentManager
            .beginTransaction()
            .replace(container.id, navigator, "deft-epub")
            .commitNow()
        collection =
            activity.lifecycleScope.launch {
                var previousResource: Url? = null
                navigator.currentLocator.collect {
                    model.interaction.location(generation, it)
                    if (previousResource != it.href) {
                        previousResource = it.href
                        applyLinkColor(activity, fragment, theme)
                    }
                }
            }
    }

    fun apply(
        newPreferences: ReaderPreferences,
        newTheme: ReaderTheme,
    ) {
        if (preferences == newPreferences && theme == newTheme) return
        if (preferences.scroll != newPreferences.scroll) {
            val host = container
            if (host != null) {
                detach()
                attach(host, newPreferences, newTheme)
                return
            }
        }
        preferences = newPreferences
        theme = newTheme
        model.interaction.preferencesChanged()
        continuous?.apply(newPreferences, newTheme)
        fragment?.submitPreferences(newPreferences.toNavigatorPreferences(newTheme))
        applyLinkColor(activity, fragment, theme)
    }

    fun turn(forward: Boolean) {
        continuous?.let {
            model.interaction.userInput()
            it.turn(forward)
            return
        }
        val navigator = fragment ?: return
        model.interaction.userInput()
        val animated = preferences.pageAnimation == PageAnimation.SLIDE && ValueAnimator.areAnimatorsEnabled()
        if (forward) navigator.goForward(animated) else navigator.goBackward(animated)
    }

    fun swipe(offset: Float) = turnPhysical(right = offset < 0)

    private fun turnPhysical(right: Boolean) {
        val rtl =
            fragment?.settings?.value?.readingProgression == org.readium.r2.navigator.preferences.ReadingProgression.RTL
        turn(forward = right != rtl)
    }

    fun go(link: Link) {
        continuous?.let {
            session.opened.publication.locatorFromLink(link)?.let { point ->
                model.interaction.beforeJump()
                it.go(point)
            } ?: model.notice("That chapter is unavailable in this book.")
            return
        }
        val navigator = fragment ?: return
        model.interaction.beforeJump()
        if (!navigator.go(
                link,
                animated =
                    preferences.pageAnimation == PageAnimation.SLIDE && ValueAnimator.areAnimatorsEnabled(),
            )
        ) {
            model.notice("That chapter is unavailable in this book.")
        }
    }

    fun go(
        locator: Locator,
        remember: Boolean = true,
    ) {
        continuous?.let {
            if (remember) model.interaction.beforeJump()
            it.go(locator)
            return
        }
        val navigator = fragment ?: return
        if (remember) model.interaction.beforeJump()
        if (!navigator.go(
                locator,
                animated =
                    preferences.pageAnimation == PageAnimation.SLIDE && ValueAnimator.areAnimatorsEnabled(),
            )
        ) {
            model.notice("That location is unavailable in this book.")
        }
    }

    fun detach() {
        collection?.cancel()
        collection = null
        model.interaction.detachNavigator(generation)
        continuous?.destroy()
        continuous = null
        fragment?.let {
            if (!activity.supportFragmentManager.isDestroyed) {
                activity.supportFragmentManager
                    .beginTransaction()
                    .remove(
                        it,
                    ).commitNowAllowingStateLoss()
            }
        }
        fragment = null
        container = null
    }

    private fun attachContinuous(
        host: FrameLayout,
        initialPreferences: ReaderPreferences,
        initialTheme: ReaderTheme,
    ) {
        val token = generation
        continuous =
            ContinuousReaderView(
                activity,
                session,
                initialPreferences,
                initialTheme,
                model.state.value.locator ?: session.initialLocator,
                callbacks =
                    ContinuousReaderCallbacks(
                        onLocation = { model.interaction.rendered(token, it) },
                        onTap = toggleChrome,
                        onImage = onImage,
                        onLink = { href ->
                            val resource = localPublicationUrl(href)
                            val link = resource?.let { session.opened.publication.linkWithHref(it) }
                            val point = link?.let { session.opened.publication.locatorFromLink(it) }
                            if (point == null) {
                                model.notice("External links stay closed while reading offline.")
                            } else {
                                val fragment = href.substringAfter('#', "")
                                go(
                                    point.copy(
                                        locations =
                                            point.locations.copy(
                                                fragments = listOfNotNull(fragment.takeIf(String::isNotBlank)),
                                            ),
                                    ),
                                )
                            }
                        },
                        onSelection = model.interaction::selection,
                        onError = {
                            model.notice(
                                "A chapter is unavailable. Continue scrolling or choose another from Contents.",
                            )
                        },
                    ),
            )
        host.addView(continuous?.view, FrameLayout.LayoutParams(-1, -1))
    }

    private fun selectionCallback() =
        object : ActionMode.Callback {
            override fun onCreateActionMode(
                mode: ActionMode,
                menu: Menu,
            ): Boolean {
                selecting = true
                menu
                    .add(
                        Menu.NONE,
                        android.R.id.copy,
                        Menu.NONE,
                        "Copy",
                    ).setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM)
                activity.lifecycleScope.launch {
                    fragment?.currentSelection()?.let {
                        model.interaction.selection(
                            it.locator,
                        )
                    }
                }
                return true
            }

            override fun onPrepareActionMode(
                mode: ActionMode,
                menu: Menu,
            ): Boolean = false

            override fun onActionItemClicked(
                mode: ActionMode,
                item: MenuItem,
            ): Boolean {
                if (item.itemId != android.R.id.copy) return false
                activity.lifecycleScope.launch {
                    fragment?.currentSelection()?.locator?.text?.highlight?.let { text ->
                        activity
                            .getSystemService(
                                ClipboardManager::class.java,
                            ).setPrimaryClip(ClipData.newPlainText("Book text", text))
                    }
                    mode.finish()
                }
                return true
            }

            override fun onDestroyActionMode(mode: ActionMode) {
                selecting = false
                fragment?.clearSelection()
            }
        }
}

private fun hardenWebViews(view: View?) {
    when (view) {
        is WebView -> {
            view.settings.blockNetworkLoads = true
            view.settings.allowFileAccess = false
            view.settings.allowContentAccess = false
            view.settings.setSupportZoom(false)
            view.settings.builtInZoomControls = false
            view.settings.displayZoomControls = false
        }
        is android.view.ViewGroup -> for (index in 0 until view.childCount) hardenWebViews(view.getChildAt(index))
    }
}

/** Observes gestures without stealing WebView selection, scrolling or accessibility actions. */
internal class ReaderContainer(
    context: Context,
    private val onReadingGesture: () -> Unit,
    private val handlesSwipes: () -> Boolean,
    private val onSwipe: (Float) -> Unit,
) : FrameLayout(context) {
    private var x = 0f
    private var y = 0f
    private val slop = ViewConfiguration.get(context).scaledTouchSlop
    private var intercepted = false
    private var multiplePointers = false
    private var horizontal = false

    override fun dispatchTouchEvent(event: MotionEvent): Boolean {
        if (event.actionMasked == MotionEvent.ACTION_DOWN) {
            x = event.x
            y = event.y
            intercepted = false
            multiplePointers = false
            horizontal = false
        }
        if (event.pointerCount > 1) multiplePointers = true
        if (event.actionMasked == MotionEvent.ACTION_MOVE) observeMove(event)
        if (intercepted) {
            if (event.actionMasked == MotionEvent.ACTION_UP) {
                val offset = event.x - x
                if (horizontal &&
                    kotlin.math.abs(offset) >= MINIMUM_SWIPE_DP * resources.displayMetrics.density
                ) {
                    onSwipe(offset)
                }
                intercepted = false
            } else if (event.actionMasked == MotionEvent.ACTION_CANCEL) {
                intercepted = false
            }
            return true
        }
        return super.dispatchTouchEvent(event)
    }

    private fun observeMove(event: MotionEvent) {
        val dx = kotlin.math.abs(event.x - x)
        val dy = kotlin.math.abs(event.y - y)
        if (dx > slop || dy > slop) onReadingGesture()
        val canIntercept = !intercepted && !multiplePointers && handlesSwipes()
        if (canIntercept && (dx > slop || dy > slop)) {
            // Readium treats slight horizontal drift during vertical drags as a page fling.
            // Classify once, cancel that native fling, and only turn for a horizontal gesture.
            horizontal = dx > dy * HORIZONTAL_GESTURE_RATIO
            val cancel = MotionEvent.obtain(event).apply { action = MotionEvent.ACTION_CANCEL }
            super.dispatchTouchEvent(cancel)
            cancel.recycle()
            intercepted = true
        }
    }
}

private const val TAP_EDGE_FRACTION = 0.25f
private const val HORIZONTAL_GESTURE_RATIO = 1.25f
private const val MINIMUM_SWIPE_DP = 32

private fun applyLinkColor(
    activity: FragmentActivity,
    fragment: EpubNavigatorFragment?,
    theme: ReaderTheme?,
) {
    val color = theme?.accent?.takeIf { Regex("#[0-9a-fA-F]{6}").matches(it) } ?: return
    val transition =
        if (ValueAnimator.areAnimatorsEnabled()) {
            "background-color 180ms linear,color 180ms linear"
        } else {
            "none"
        }
    activity.lifecycleScope.launch {
        fragment?.evaluateJavascript(
            "document.documentElement.style.transition='$transition';" +
                "document.documentElement.style.setProperty('--RS__linkColor','$color');" +
                "document.documentElement.style.setProperty('--RS__visitedColor','$color');",
        )
    }
}
