@file:OptIn(org.readium.r2.shared.ExperimentalReadiumApi::class)

package com.kmab.deft.feature.reader

import android.animation.ValueAnimator
import android.content.ClipData
import android.content.ClipboardManager
import android.view.ActionMode
import android.view.Menu
import android.view.MenuItem
import android.view.View
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
    private val onExternalLink: (String) -> Unit = {},
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
    val nativeSwipes: Boolean
        get() = preferences.pageAnimation == PageAnimation.SLIDE && ValueAnimator.areAnimatorsEnabled()

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
                            onExternalLink(url.toString())
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
                            model.interaction.renderingFailed()
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
                            when (readerTapAction(event.point.x, width, preferences.tapToTurn)) {
                                ReaderTapAction.CHROME -> toggleChrome()
                                ReaderTapAction.LEFT -> turnPhysical(right = false)
                                ReaderTapAction.RIGHT -> turnPhysical(right = true)
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
                                if (resource == null) {
                                    onExternalLink(href)
                                } else {
                                    model.notice("That location is unavailable in this book.")
                                }
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
                            model.interaction.renderingFailed()
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
                    fragment?.clearSelection()
                    mode.finish()
                }
                return true
            }

            override fun onDestroyActionMode(mode: ActionMode) {
                // Chromium can recreate the toolbar while a selection handle moves.
                // Its lifetime is not the lifetime of the selected DOM range.
                selecting = false
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
