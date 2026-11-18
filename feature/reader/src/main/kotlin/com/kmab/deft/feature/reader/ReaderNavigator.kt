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
    private val toggleChrome: () -> Unit,
) {
    private var fragment: EpubNavigatorFragment? = null
    private var collection: Job? = null
    private var generation = 0L
    private var preferences = ReaderPreferences()
    private var theme: ReaderTheme? = null
    private var selecting = false
    val handlesSwipes: Boolean
        get() =
            !preferences.scroll && !selecting &&
                (preferences.pageAnimation == PageAnimation.NONE || !ValueAnimator.areAnimatorsEnabled())

    fun attach(
        container: FrameLayout,
        initialPreferences: ReaderPreferences,
        initialTheme: ReaderTheme,
    ) {
        if (fragment != null || activity.isFinishing) return
        generation = model.interaction.attachNavigator()
        preferences = initialPreferences
        theme = initialTheme
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
                                applyLinkColor()
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
                    val edge = width * TAP_EDGE_FRACTION
                    if (preferences.scroll || event.point.x in edge..(width - edge)) {
                        toggleChrome()
                    } else {
                        turnPhysical(right = event.point.x > width / 2)
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
                        applyLinkColor()
                    }
                }
            }
    }

    fun apply(
        newPreferences: ReaderPreferences,
        newTheme: ReaderTheme,
    ) {
        if (preferences == newPreferences && theme == newTheme) return
        preferences = newPreferences
        theme = newTheme
        model.interaction.preferencesChanged()
        fragment?.submitPreferences(newPreferences.toNavigatorPreferences(newTheme))
        applyLinkColor()
    }

    fun turn(forward: Boolean) {
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
    }

    private fun applyLinkColor() {
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

    override fun dispatchTouchEvent(event: MotionEvent): Boolean {
        if (event.actionMasked == MotionEvent.ACTION_DOWN) {
            x = event.x
            y = event.y
            intercepted = false
            multiplePointers = false
        }
        if (event.pointerCount > 1) multiplePointers = true
        if (event.actionMasked == MotionEvent.ACTION_MOVE) observeMove(event)
        if (intercepted) {
            if (event.actionMasked == MotionEvent.ACTION_UP) {
                val offset = event.x - x
                if (kotlin.math.abs(offset) >= MINIMUM_SWIPE_DP * resources.displayMetrics.density) onSwipe(offset)
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
        if (canIntercept && dx > slop && dx > dy * HORIZONTAL_GESTURE_RATIO) {
            // Readium hardcodes native fling animation. Only NONE/reduced motion takes this path.
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
