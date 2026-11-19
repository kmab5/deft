package com.kmab.deft.feature.reader

import android.content.Context
import android.view.MotionEvent
import android.view.View
import android.widget.FrameLayout
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class ReaderContainerTest {
    @Test fun verticalDragWithHorizontalDriftNeverTurnsAPage() {
        val childEvents = mutableListOf<Int>()
        val swipes = mutableListOf<Float>()
        val container = container(true, childEvents, swipes)
        touch(container, MotionEvent.ACTION_DOWN, 300f, 400f)
        touch(container, MotionEvent.ACTION_MOVE, 270f, 180f)
        touch(container, MotionEvent.ACTION_UP, 230f, 40f)
        assertTrue(childEvents.contains(MotionEvent.ACTION_CANCEL))
        assertFalse(childEvents.contains(MotionEvent.ACTION_UP))
        assertTrue(swipes.isEmpty())
    }

    @Test fun horizontalPageSwipeCancelsNativeFlingAndUsesPreferenceAwareTurn() {
        val childEvents = mutableListOf<Int>()
        val swipes = mutableListOf<Float>()
        val container = container(true, childEvents, swipes)
        touch(container, MotionEvent.ACTION_DOWN, 300f, 200f)
        touch(container, MotionEvent.ACTION_MOVE, 80f, 200f)
        touch(container, MotionEvent.ACTION_UP, 50f, 200f)
        assertTrue(childEvents.contains(MotionEvent.ACTION_CANCEL))
        assertFalse(childEvents.contains(MotionEvent.ACTION_UP))
        assertEquals(listOf(-250f), swipes)
    }

    @Test fun scrollModeAndSelectionKeepTheirNativeGestures() {
        val childEvents = mutableListOf<Int>()
        val swipes = mutableListOf<Float>()
        val container = container(false, childEvents, swipes)
        touch(container, MotionEvent.ACTION_DOWN, 300f, 200f)
        touch(container, MotionEvent.ACTION_MOVE, 80f, 200f)
        touch(container, MotionEvent.ACTION_UP, 50f, 200f)
        assertFalse(childEvents.contains(MotionEvent.ACTION_CANCEL))
        assertTrue(childEvents.contains(MotionEvent.ACTION_UP))
        assertTrue(swipes.isEmpty())
    }

    private fun container(
        handle: Boolean,
        events: MutableList<Int>,
        swipes: MutableList<Float>,
    ): ReaderContainer {
        val context = ApplicationProvider.getApplicationContext<Context>()
        return ReaderContainer(context, {}, { handle }, { swipes += it }).apply {
            addView(
                object : View(context) {
                    override fun onTouchEvent(event: MotionEvent): Boolean {
                        events += event.actionMasked
                        return true
                    }
                },
                FrameLayout.LayoutParams(500, 500),
            )
            measure(
                View.MeasureSpec.makeMeasureSpec(500, View.MeasureSpec.EXACTLY),
                View.MeasureSpec.makeMeasureSpec(500, View.MeasureSpec.EXACTLY),
            )
            layout(0, 0, 500, 500)
        }
    }

    private fun touch(
        container: ReaderContainer,
        action: Int,
        x: Float,
        y: Float,
    ) {
        val event = MotionEvent.obtain(0, 10, action, x, y, 0)
        container.dispatchTouchEvent(event)
        event.recycle()
    }
}
