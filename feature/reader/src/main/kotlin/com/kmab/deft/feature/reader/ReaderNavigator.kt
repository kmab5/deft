@file:OptIn(org.readium.r2.shared.ExperimentalReadiumApi::class)

package com.kmab.deft.feature.reader

import android.animation.ValueAnimator
import android.view.View
import android.webkit.WebView
import android.widget.FrameLayout
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.lifecycleScope
import com.kmab.deft.core.database.entity.HighlightEntity
import com.kmab.deft.core.designsystem.DeftHaptic
import com.kmab.deft.core.designsystem.performDeftHaptic
import com.kmab.deft.core.model.PageAnimation
import com.kmab.deft.core.model.ReaderPreferences
import com.kmab.deft.core.model.ReaderTheme
import com.kmab.deft.format.readium.localPublicationUrl
import com.kmab.deft.format.readium.toNavigatorPreferences
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import org.readium.r2.navigator.epub.EpubNavigatorFragment
import org.readium.r2.navigator.input.InputListener
import org.readium.r2.navigator.input.TapEvent
import org.readium.r2.shared.publication.Link
import org.readium.r2.shared.publication.Locator
import org.readium.r2.shared.util.Url

internal data class ReaderMediaActions(
    val image: (Url) -> Unit = {},
    val external: (String) -> Unit = {},
)

internal class ReaderNavigator(
    private val activity: FragmentActivity,
    private val model: ReaderViewModel,
    private val session: ReaderSession,
    private val mediaActions: ReaderMediaActions = ReaderMediaActions(),
    private val annotationActions: ReaderAnnotationActions = ReaderAnnotationActions(),
    private val toggleChrome: () -> Unit,
) {
    private var fragment: EpubNavigatorFragment? = null
    private var continuous: ContinuousReaderView? = null
    private var container: FrameLayout? = null
    private var collection: Job? = null
    private var annotations: List<HighlightEntity> = emptyList()
    private val pagedAnnotations = ReaderPagedAnnotations(activity, model::notice) { fragment }
    private var generation = 0L
    private var preferences = ReaderPreferences()
    private var theme: ReaderTheme? = null
    private val onSelectionStarted: (Locator) -> Unit = { locator ->
        activity.window.decorView.performDeftHaptic(DeftHaptic.SELECTION)
        model.interaction.selection(locator)
    }
    val handlesSwipes: Boolean
        get() =
            !preferences.scroll && !pagedAnnotations.hasToolbar
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
        pagedAnnotations.pendingRange =
            (model.state.value.locator ?: session.initialLocator)?.takeIf {
                it.locations.otherLocations.containsKey("domRange")
            }
        val callbacks =
            ReaderPagedCallbacks(
                onSelectionStarted,
                annotationActions.selection,
                mediaActions.external,
            ) {
                fragment?.let {
                    hardenWebViews(it.view)
                    model.interaction.rendered(generation, it.currentLocator.value)
                    applyLinkColor(activity, fragment, theme)
                    pagedAnnotations.apply(annotations)
                }
            }
        val navigator =
            ReaderPagedFactory(activity, model, session, pagedAnnotations, callbacks).create(
                initialPreferences,
                initialTheme,
                model.state.value.locator ?: session.initialLocator,
            )
        fragment = navigator
        val inputActions =
            ReaderInputActions(mediaActions.image, { preferences.tapToTurn }) { action ->
                when (action) {
                    ReaderTapAction.CHROME -> toggleChrome()
                    ReaderTapAction.LEFT -> turnPhysical(right = false)
                    ReaderTapAction.RIGHT -> turnPhysical(right = true)
                }
            }
        installReaderInput(navigator, activity, session, inputActions)
        activity.supportFragmentManager
            .beginTransaction()
            .replace(container.id, navigator, "deft-epub")
            .commitNow()
        // Readium's decoration listener accesses the ViewModel, which requires an attached fragment.
        pagedAnnotations.listenForEdits(annotationActions.edit)
        pagedAnnotations.apply(annotations)
        collection =
            navigator.observeReadingLocation(
                activity,
                onLocation = { model.interaction.location(generation, it) },
                onResourceChanged = {
                    applyLinkColor(activity, fragment, theme)
                    pagedAnnotations.apply(annotations)
                },
            )
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
        pagedAnnotations.pendingRange = null
        pagedAnnotations.clearSearch()
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

    val onSelectionTouch: (Boolean) -> Unit = { down ->
        pagedAnnotations.selectionTouch(down)
        continuous?.selectionTouch(down)
    }

    private fun turnPhysical(right: Boolean) {
        val rtl =
            fragment?.settings?.value?.readingProgression == org.readium.r2.navigator.preferences.ReadingProgression.RTL
        turn(forward = right != rtl)
    }

    fun go(link: Link) {
        pagedAnnotations.pendingRange = null
        pagedAnnotations.clearSearch()
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
        search: Boolean = false,
    ) {
        continuous?.let {
            if (remember) model.interaction.beforeJump()
            it.go(locator, search)
            return
        }
        val navigator = fragment ?: return
        if (remember) model.interaction.beforeJump()
        pagedAnnotations.clearSearch()
        if (search) pagedAnnotations.searchLocator = locator
        pagedAnnotations.pendingRange = locator.takeIf { it.locations.otherLocations.containsKey("domRange") }
        if (!navigator.go(
                locator.forReadiumRange(),
                animated =
                    preferences.pageAnimation == PageAnimation.SLIDE && ValueAnimator.areAnimatorsEnabled(),
            )
        ) {
            pagedAnnotations.pendingRange = null
            pagedAnnotations.clearSearch()
            model.notice("That location is unavailable in this book.")
        } else {
            pagedAnnotations.apply(annotations)
        }
    }

    fun detach() {
        pagedAnnotations.close()
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
                        onImage = mediaActions.image,
                        onLink = { href ->
                            val resource = localPublicationUrl(href)
                            val link = resource?.let { session.opened.publication.linkWithHref(it) }
                            val point = link?.let { session.opened.publication.locatorFromLink(it) }
                            if (point == null) {
                                if (resource == null) {
                                    mediaActions.external(href)
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
                        onSelection = onSelectionStarted,
                        onSelectionAction = annotationActions.selection,
                        onAnnotation = annotationActions.edit,
                        onNotice = model::notice,
                        onError = {
                            model.interaction.renderingFailed()
                            model.notice(
                                "A chapter is unavailable. Continue scrolling or choose another from Contents.",
                            )
                        },
                    ),
            )
        host.addView(continuous?.view, FrameLayout.LayoutParams(-1, -1))
        continuous?.annotations(annotations)
    }

    fun annotations(values: List<HighlightEntity>) {
        if (annotations == values) return
        annotations = values
        continuous?.annotations(values)
        pagedAnnotations.apply(annotations)
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

private fun EpubNavigatorFragment.observeReadingLocation(
    activity: FragmentActivity,
    onLocation: (Locator) -> Unit,
    onResourceChanged: () -> Unit,
): Job =
    activity.lifecycleScope.launch {
        var previousResource: Url? = null
        currentLocator.collect {
            onLocation(it)
            if (previousResource != it.href) {
                previousResource = it.href
                onResourceChanged()
            }
        }
    }

private data class ReaderInputActions(
    val image: (Url) -> Unit,
    val tapToTurn: () -> Boolean,
    val tap: (ReaderTapAction) -> Unit,
)

private fun installReaderInput(
    navigator: EpubNavigatorFragment,
    activity: FragmentActivity,
    session: ReaderSession,
    actions: ReaderInputActions,
) {
    navigator.addInputListener(
        object : InputListener {
            override fun onTap(event: TapEvent): Boolean {
                val width = navigator.view?.width ?: return false
                activity.lifecycleScope.launch {
                    val image = navigator.imageAt(event, width, session.opened.publication)
                    if (image != null) {
                        actions.image(image)
                    } else {
                        actions.tap(readerTapAction(event.point.x, width, actions.tapToTurn()))
                    }
                }
                return true
            }
        },
    )
}
