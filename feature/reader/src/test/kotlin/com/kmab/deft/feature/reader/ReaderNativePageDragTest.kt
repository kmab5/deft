@file:Suppress("INVISIBLE_MEMBER", "INVISIBLE_REFERENCE")

package com.kmab.deft.feature.reader

import android.content.Context
import android.os.SystemClock
import android.view.MotionEvent
import android.view.View
import android.webkit.WebView
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.readium.r2.navigator.R2WebView
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.Implementation
import org.robolectric.annotation.Implements
import org.robolectric.annotation.RealObject
import org.robolectric.shadow.api.Shadow
import org.robolectric.shadows.ShadowWebView
import org.robolectric.util.ReflectionHelpers.ClassParameter

/** Exercises the pinned Readium 3.2 native scroller, not a copy of its target-page calculation. */
@RunWith(RobolectricTestRunner::class)
@Config(
    sdk = [35],
    shadows = [ReaderNativePageDragTest.PagedWebViewShadow::class],
)
class ReaderNativePageDragTest {
    @Test fun backwardDragFromFiveThroughFourPointFourSettlesOnFourNotThree() =
        assertTarget(offset = 300f, fractionalScroll = 2200, target = 4)

    @Test fun forwardDragFromFiveThroughFivePointSixSettlesOnSixNotSeven() =
        assertTarget(offset = -300f, fractionalScroll = 2800, target = 6)

    @Test fun chapterBoundaryIsRestoredBeforeRelativeNavigationRuns() {
        val view: R2WebView = pageView(0)
        val drag = ReaderNativePageDrag(view)
        view.scrollTo(100, 0)
        val observed = mutableListOf<Int>()
        drag.commit(300f) { observed += view.scrollX }
        assertEquals(listOf(0), observed)
        view.destroy()
    }

    private fun assertTarget(
        offset: Float,
        fractionalScroll: Int,
        target: Int,
    ) {
        val view: R2WebView = pageView(5)
        val drag = ReaderNativePageDrag(view)
        touch(view, MotionEvent.ACTION_DOWN, 250f)
        touch(view, MotionEvent.ACTION_MOVE, 250f + offset / 2)
        view.scrollTo(fractionalScroll, 0)
        touch(view, MotionEvent.ACTION_CANCEL, 250f + offset / 2)
        val boundaryTurns = mutableListOf<Float>()
        drag.commit(offset) { boundaryTurns += it }
        assertEquals(target, view.mCurItem)
        assertTrue(boundaryTurns.isEmpty())
        SystemClock.sleep(1000)
        view.computeScroll()
        assertEquals(target * 500, view.scrollX)
        view.destroy()
    }

    private fun pageView(page: Int): R2WebView {
        val context = ApplicationProvider.getApplicationContext<Context>()
        return R2WebView(context, Robolectric.buildAttributeSet().build()).apply {
            measure(
                View.MeasureSpec.makeMeasureSpec(500, View.MeasureSpec.EXACTLY),
                View.MeasureSpec.makeMeasureSpec(500, View.MeasureSpec.EXACTLY),
            )
            layout(0, 0, 500, 500)
            assertEquals("Native viewport is laid out", 500, width)
            assertEquals("Readium receives ten native pages", 10, numPages)
            setCurrentItem(page, false)
            assertEquals("Native page setup is settled", page * 500, scrollX)
        }
    }

    private fun touch(
        view: R2WebView,
        action: Int,
        x: Float,
    ) {
        val time = SystemClock.uptimeMillis()
        val event = MotionEvent.obtain(time, time, action, x, 200f, 0)
        view.onTouchEvent(event)
        event.recycle()
    }

    @Implements(WebView::class)
    class PagedWebViewShadow : ShadowWebView() {
        @RealObject private lateinit var webView: WebView

        // WebView.setFrame delegates to Chromium. Robolectric installs a no-op deep proxy;
        // use View's actual frame implementation, preserving Readium layout and scroller code.
        @Implementation
        fun setFrame(
            left: Int,
            top: Int,
            right: Int,
            bottom: Int,
        ): Boolean =
            Shadow.directlyOn(
                webView,
                View::class.java,
                "setFrame",
                ClassParameter.from(Int::class.javaPrimitiveType, left),
                ClassParameter.from(Int::class.javaPrimitiveType, top),
                ClassParameter.from(Int::class.javaPrimitiveType, right),
                ClassParameter.from(Int::class.javaPrimitiveType, bottom),
            )

        @Suppress("FunctionOnlyReturningConstant")
        @Implementation
        fun computeHorizontalScrollRange(): Int = 5000
    }
}
