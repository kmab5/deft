package com.kmab.deft.feature.reader

import android.animation.ValueAnimator
import android.graphics.Color
import android.view.ActionMode
import android.view.Menu
import android.view.MenuItem
import android.webkit.JavascriptInterface
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.fragment.app.FragmentActivity
import com.kmab.deft.core.model.PageAnimation
import com.kmab.deft.core.model.ReaderPreferences
import com.kmab.deft.core.model.ReaderTheme
import com.kmab.deft.format.readium.ContinuousReaderResources
import com.kmab.deft.format.readium.continuousReaderStyles
import com.kmab.deft.format.readium.localPublicationUrl
import org.json.JSONObject
import org.json.JSONTokener
import org.readium.r2.shared.publication.Locator
import org.readium.r2.shared.util.Url

/** A vertical viewport over isolated, virtualized chapter documents with original publication locators. */
internal class ContinuousReaderView(
    private val activity: FragmentActivity,
    private val session: ReaderSession,
    preferences: ReaderPreferences,
    theme: ReaderTheme,
    initialLocator: Locator?,
    private val callbacks: ContinuousReaderCallbacks,
) {
    private var ready = false
    private var destroyed = false
    private var css = continuousReaderStyles(preferences, theme, session.fonts)
    private var target = initialLocator
    private var awaitingRestore = initialLocator
    private var animate = preferences.pageAnimation == PageAnimation.SLIDE
    private val publicationResources =
        ContinuousReaderResources(session.opened.publication, activity.assets) {
            activity.runOnUiThread { if (!destroyed) callbacks.onError() }
        }
    val view: WebView =
        object : WebView(activity) {
            override fun startActionMode(
                callback: ActionMode.Callback,
                type: Int,
            ): ActionMode? = super.startActionMode(selectionCallback(callback), type)
        }.apply {
            settings.javaScriptEnabled = true
            settings.blockNetworkLoads = true
            settings.allowFileAccess = false
            settings.allowContentAccess = false
            settings.setSupportZoom(false)
            settings.builtInZoomControls = false
            settings.displayZoomControls = false
            settings.useWideViewPort = true
            isHorizontalScrollBarEnabled = false
            setBackgroundColor(Color.parseColor(theme.background))
            addJavascriptInterface(Bridge(), "DeftContinuous")
            webViewClient =
                object : WebViewClient() {
                    override fun shouldInterceptRequest(
                        view: WebView,
                        request: WebResourceRequest,
                    ): WebResourceResponse = publicationResources.response(request.url.toString())

                    override fun shouldOverrideUrlLoading(
                        view: WebView,
                        request: WebResourceRequest,
                    ): Boolean = request.isForMainFrame || localPublicationUrl(request.url.toString()) == null

                    override fun onPageFinished(
                        view: WebView,
                        url: String,
                    ) {
                        if (url != ContinuousReaderResources.HOST_URL || destroyed) return
                        ready = true
                        script("deftContinuous.configure(${JSONObject.quote(css)});")
                        target?.let(::go)
                    }
                }
            loadUrl(ContinuousReaderResources.HOST_URL)
        }

    fun apply(
        preferences: ReaderPreferences,
        theme: ReaderTheme,
    ) {
        css = continuousReaderStyles(preferences, theme, session.fonts)
        animate = preferences.pageAnimation == PageAnimation.SLIDE
        view.setBackgroundColor(Color.parseColor(theme.background))
        if (ready) script("deftContinuous.configure(${JSONObject.quote(css)});")
    }

    fun go(locator: Locator) {
        target = locator
        awaitingRestore = locator
        if (!ready) return
        val href = JSONObject.quote(locator.href.toString().substringBefore('#'))
        val fragment = locator.locations.fragments.firstOrNull() ?: locator.href.toString().substringAfter('#', "")
        script(
            "deftContinuous.go($href,${locator.locations.progression ?: 0.0}," +
                "${JSONObject.quote(fragment)},${JSONObject.quote(locator.text.highlight.orEmpty())}," +
                "${JSONObject.quote(
                    locator.text.before.orEmpty(),
                )},${JSONObject.quote(locator.text.after.orEmpty())});",
        )
    }

    fun turn(forward: Boolean) {
        val behavior = if (animate && ValueAnimator.areAnimatorsEnabled()) "smooth" else "instant"
        script("window.scrollBy({top:innerHeight*${if (forward) "0.85" else "-0.85"},behavior:'$behavior'});")
    }

    fun destroy() {
        destroyed = true
        ready = false
        view.stopLoading()
        view.removeJavascriptInterface("DeftContinuous")
        (view.parent as? android.view.ViewGroup)?.removeView(view)
        view.destroy()
    }

    private fun script(value: String) {
        if (!destroyed) view.evaluateJavascript(value, null)
    }

    private fun selectionCallback(delegate: ActionMode.Callback) =
        object : ActionMode.Callback {
            override fun onCreateActionMode(
                mode: ActionMode,
                menu: Menu,
            ): Boolean {
                view.evaluateJavascript("JSON.stringify(deftContinuous.selection())") { value ->
                    val data = runCatching { JSONObject(JSONTokener(value).nextValue().toString()) }.getOrNull()
                    data?.let {
                        locator(it.optString("href"), it.optDouble("progression", 0.0))
                            ?.copy(text = Locator.Text(highlight = it.optString("text")))
                            ?.let(callbacks.onSelection)
                    }
                }
                return delegate.onCreateActionMode(mode, menu)
            }

            override fun onPrepareActionMode(
                mode: ActionMode,
                menu: Menu,
            ): Boolean = delegate.onPrepareActionMode(mode, menu)

            override fun onActionItemClicked(
                mode: ActionMode,
                item: MenuItem,
            ): Boolean = delegate.onActionItemClicked(mode, item)

            override fun onDestroyActionMode(mode: ActionMode) {
                // Android recreates the floating toolbar while a selection handle moves.
                // The native delegate owns dismissal; clearing ranges here cancels that selection.
                delegate.onDestroyActionMode(mode)
            }
        }

    // Reject malformed bridge input before reading publication metadata.
    @Suppress("ReturnCount")
    private fun locator(
        href: String,
        progression: Double,
    ): Locator? {
        if (!progression.isFinite()) return null
        val url = localPublicationUrl(href) ?: return null
        val publication = session.opened.publication
        val index = publication.readingOrder.indexOfFirst { it.href.toString().substringBefore('#') == url.toString() }
        if (index < 0) return null
        val positions = session.positions.filter { it.href == url }
        val progress = progression.coerceIn(0.0, 1.0)
        val position = positions.getOrNull((progress * positions.size).toInt().coerceAtMost(positions.lastIndex))
        val start =
            positions.firstOrNull()?.locations?.totalProgression ?: index.toDouble() / publication.readingOrder.size
        val next = session.positions.firstOrNull { (it.locations.totalProgression ?: 0.0) > start && it.href != url }
        val end = next?.locations?.totalProgression ?: 1.0
        return publication.locatorFromLink(publication.readingOrder[index])?.copy(
            locations =
                Locator.Locations(
                    progression = progress,
                    totalProgression = start + (end - start) * progress,
                    position = position?.locations?.position,
                ),
        )
    }

    private inner class Bridge {
        @JavascriptInterface fun location(
            href: String,
            progression: Double,
        ) {
            activity.runOnUiThread {
                if (!ready || destroyed) return@runOnUiThread
                val point = locator(href, progression) ?: return@runOnUiThread
                awaitingRestore?.let { expected ->
                    if (point.href.toString() != expected.href.toString().substringBefore('#')) return@runOnUiThread
                    awaitingRestore = null
                }
                callbacks.onLocation(point)
            }
        }

        @JavascriptInterface fun tap() = activity.runOnUiThread { if (!destroyed) callbacks.onTap() }

        @JavascriptInterface fun image(href: String) =
            activity.runOnUiThread {
                if (destroyed) return@runOnUiThread
                localPublicationUrl(
                    href,
                )?.takeIf { session.opened.publication.linkWithHref(it) != null }?.let(callbacks.onImage)
            }

        @JavascriptInterface fun link(href: String) = activity.runOnUiThread { if (!destroyed) callbacks.onLink(href) }

        @JavascriptInterface fun error(href: String) =
            activity.runOnUiThread {
                if (!destroyed && localPublicationUrl(href) != null) {
                    awaitingRestore = null
                    callbacks.onError()
                }
            }
    }
}

internal data class ContinuousReaderCallbacks(
    val onLocation: (Locator) -> Unit,
    val onTap: () -> Unit,
    val onImage: (Url) -> Unit,
    val onLink: (String) -> Unit,
    val onSelection: (Locator) -> Unit,
    val onError: () -> Unit,
)
