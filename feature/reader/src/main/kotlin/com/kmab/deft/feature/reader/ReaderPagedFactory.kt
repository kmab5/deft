@file:OptIn(org.readium.r2.shared.ExperimentalReadiumApi::class)

package com.kmab.deft.feature.reader

import androidx.fragment.app.FragmentActivity
import com.kmab.deft.core.model.ReaderPreferences
import com.kmab.deft.core.model.ReaderTheme
import com.kmab.deft.format.readium.navigatorConfiguration
import com.kmab.deft.format.readium.toNavigatorPreferences
import org.readium.r2.navigator.Decoration
import org.readium.r2.navigator.HyperlinkNavigator
import org.readium.r2.navigator.epub.EpubNavigatorFactory
import org.readium.r2.navigator.epub.EpubNavigatorFragment
import org.readium.r2.navigator.html.HtmlDecorationTemplate
import org.readium.r2.shared.publication.Link
import org.readium.r2.shared.publication.Locator
import org.readium.r2.shared.util.AbsoluteUrl
import org.readium.r2.shared.util.Url
import org.readium.r2.shared.util.data.ReadError

internal data class ReaderPagedCallbacks(
    val selected: (Locator) -> Unit,
    val action: (ReaderSelectionAction, Locator) -> Unit,
    val external: (String) -> Unit,
    val loaded: () -> Unit,
)

/** Readium configuration is rebuilt around each live publication and selection adapter. */
internal class ReaderPagedFactory(
    private val activity: FragmentActivity,
    private val model: ReaderViewModel,
    private val session: ReaderSession,
    private val annotations: ReaderPagedAnnotations,
    private val callbacks: ReaderPagedCallbacks,
) {
    fun create(
        preferences: ReaderPreferences,
        theme: ReaderTheme,
        initial: Locator?,
    ): EpubNavigatorFragment {
        val factory =
            EpubNavigatorFactory(session.opened.publication).createFragmentFactory(
                initialLocator = initial?.forReadiumRange(),
                initialPreferences = preferences.toNavigatorPreferences(theme),
                configuration = configuration(theme),
                listener =
                    object : EpubNavigatorFragment.Listener {
                        override fun onExternalLinkActivated(url: AbsoluteUrl) = callbacks.external(url.toString())

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
                        override fun onPageLoaded() = callbacks.loaded()
                    },
            )
        return factory.instantiate(
            activity.classLoader,
            EpubNavigatorFragment::class.java.name,
        ) as EpubNavigatorFragment
    }

    private fun configuration(theme: ReaderTheme) =
        navigatorConfiguration(session.fonts, theme).apply {
            selectionActionModeCallback = annotations.selectionCallback(callbacks.selected, callbacks.action)
            decorationTemplates[Decoration.Style.Highlight::class] =
                HtmlDecorationTemplate.highlight(
                    defaultTint = android.graphics.Color.GRAY,
                    lineWeight = 1,
                    cornerRadius = 2,
                    alpha = 0.24,
                )
        }
}
