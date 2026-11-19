@file:OptIn(org.readium.r2.shared.ExperimentalReadiumApi::class)

package com.kmab.deft.feature.reader

import com.kmab.deft.format.readium.localPublicationUrl
import org.json.JSONTokener
import org.readium.r2.navigator.epub.EpubNavigatorFragment
import org.readium.r2.navigator.input.TapEvent
import org.readium.r2.shared.publication.Publication
import org.readium.r2.shared.util.Url

/** Only manifest-backed publication images can open the native viewer. */
internal suspend fun EpubNavigatorFragment.imageAt(
    event: TapEvent,
    width: Int,
    publication: Publication,
): Url? {
    val height = view?.height?.takeIf { it > 0 }
    return if (height == null || width <= 0) {
        null
    } else {
        val x = event.point.x.toDouble() / width
        val y = event.point.y.toDouble() / height
        val result =
            evaluateJavascript(
                "(function(){var e=document.elementFromPoint(innerWidth*$x,innerHeight*$y);" +
                    "if(!e)return null;var i=e.closest('img,image');" +
                    "if(!i&&e.closest('svg'))i=e.closest('svg').querySelector('image');" +
                    "return i?(i.currentSrc||i.src||new URL(i.getAttribute('href')||" +
                    "i.getAttribute('xlink:href'),document.baseURI).href):null;})()",
            )
        val value = result?.let { runCatching { JSONTokener(it).nextValue() as? String }.getOrNull() }
        value?.let(::localPublicationUrl)?.takeIf { publication.linkWithHref(it) != null }
    }
}
