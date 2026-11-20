@file:Suppress("INVISIBLE_MEMBER", "INVISIBLE_REFERENCE")

package com.kmab.deft.feature.reader

import android.graphics.Rect
import android.view.View
import android.view.ViewGroup
import org.readium.r2.navigator.R2WebView

/** Small adapter to the pinned Readium 3.2 pager; retain exact native targets after a slow drag. */
internal class ReaderNativePageDrag(
    private val view: R2WebView,
) {
    private val originalPage = view.mCurItem

    fun commit(
        offset: Float,
        crossChapter: (Float) -> Unit,
    ) {
        val target = originalPage + if (offset < 0f) 1 else -1
        if (target in 0 until view.numPages) {
            // ACTION_CANCEL starts an asynchronous snap back. Replace that destination directly:
            // relative JavaScript paging from a fractional position can skip a backward page.
            view.setCurrentItem(target, true)
        } else {
            view.setCurrentItem(originalPage, false)
            crossChapter(offset)
        }
    }
}

internal fun captureNativePageDrag(
    view: View,
    x: Float,
    y: Float,
): ReaderNativePageDrag? =
    when (view) {
        is R2WebView -> {
            val bounds = Rect()
            if (view.getGlobalVisibleRect(bounds) &&
                bounds.contains(x.toInt(), y.toInt())
            ) {
                ReaderNativePageDrag(view)
            } else {
                null
            }
        }
        is ViewGroup ->
            (view.childCount - 1 downTo 0).firstNotNullOfOrNull {
                captureNativePageDrag(
                    view.getChildAt(it),
                    x,
                    y,
                )
            }
        else -> null
    }
