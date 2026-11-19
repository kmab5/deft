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
        val fixture = fixture()
        fixture.touch(MotionEvent.ACTION_DOWN, 300f, 400f)
        fixture.touch(MotionEvent.ACTION_MOVE, 270f, 180f)
        fixture.touch(MotionEvent.ACTION_UP, 230f, 40f)
        fixture.assertCancelled()
        assertTrue(fixture.swipes.isEmpty())
    }

    @Test fun slideDeliversMovesAndReleaseToNativePager() {
        val fixture = fixture()
        fixture.touch(MotionEvent.ACTION_DOWN, 400f, 200f)
        fixture.touch(MotionEvent.ACTION_MOVE, 300f, 200f)
        assertEquals(listOf(MotionEvent.ACTION_DOWN, MotionEvent.ACTION_MOVE), fixture.events)
        fixture.touch(MotionEvent.ACTION_MOVE, 180f, 200f)
        fixture.touch(MotionEvent.ACTION_UP, 100f, 200f)
        assertFalse(fixture.events.contains(MotionEvent.ACTION_CANCEL))
        assertEquals(MotionEvent.ACTION_UP, fixture.events.last())
        assertTrue(fixture.swipes.isEmpty())
    }

    @Test fun initialHorizontalJitterCannotTurnAVerticalDragIntoPaging() {
        val fixture = fixture()
        fixture.touch(MotionEvent.ACTION_DOWN, 400f, 200f)
        fixture.touch(MotionEvent.ACTION_MOVE, 380f, 200f)
        fixture.touch(MotionEvent.ACTION_MOVE, 370f, 400f)
        fixture.touch(MotionEvent.ACTION_UP, 330f, 450f)
        fixture.assertCancelled()
        assertTrue(fixture.swipes.isEmpty())
    }

    @Test fun returningMoreThanHalfwayCancelsNativeSlide() {
        val fixture = fixture()
        fixture.touch(MotionEvent.ACTION_DOWN, 400f, 200f)
        fixture.touch(MotionEvent.ACTION_MOVE, 100f, 200f)
        fixture.touch(MotionEvent.ACTION_MOVE, 280f, 200f)
        fixture.touch(MotionEvent.ACTION_UP, 280f, 200f)
        fixture.assertCancelled()
        assertTrue(fixture.swipes.isEmpty())
    }

    @Test fun returningLessThanHalfwayLeavesNativePagerInControl() {
        val fixture = fixture()
        fixture.touch(MotionEvent.ACTION_DOWN, 400f, 200f)
        fixture.touch(MotionEvent.ACTION_MOVE, 100f, 200f)
        fixture.touch(MotionEvent.ACTION_MOVE, 220f, 200f)
        fixture.touch(MotionEvent.ACTION_UP, 220f, 200f)
        assertFalse(fixture.events.contains(MotionEvent.ACTION_CANCEL))
        assertEquals(MotionEvent.ACTION_UP, fixture.events.last())
    }

    @Test fun noneOrDisabledSystemAnimationUsesImmediateTurn() {
        val fixture = fixture(native = false)
        fixture.touch(MotionEvent.ACTION_DOWN, 300f, 200f)
        fixture.touch(MotionEvent.ACTION_MOVE, 80f, 200f)
        fixture.touch(MotionEvent.ACTION_UP, 50f, 200f)
        fixture.assertCancelled()
        assertEquals(listOf(-250f), fixture.swipes)
    }

    @Test fun reversalAlsoCancelsImmediateTurn() {
        val fixture = fixture(native = false)
        fixture.touch(MotionEvent.ACTION_DOWN, 400f, 200f)
        fixture.touch(MotionEvent.ACTION_MOVE, 100f, 200f)
        fixture.touch(MotionEvent.ACTION_UP, 280f, 200f)
        assertTrue(fixture.swipes.isEmpty())
    }

    @Test fun eitherSystemEdgeCanNeverStartPageSwipe() {
        for ((start, end) in listOf(2f to 250f, 498f to 250f)) {
            val fixture = fixture()
            fixture.touch(MotionEvent.ACTION_DOWN, start, 200f)
            fixture.touch(MotionEvent.ACTION_MOVE, end, 200f)
            fixture.touch(MotionEvent.ACTION_UP, end, 200f)
            fixture.assertCancelled()
            assertFalse(fixture.events.contains(MotionEvent.ACTION_MOVE))
            assertTrue(fixture.swipes.isEmpty())
        }
    }

    @Test fun edgeTapStillReachesNativeTapHandler() {
        val fixture = fixture()
        fixture.touch(MotionEvent.ACTION_DOWN, 2f, 200f)
        fixture.touch(MotionEvent.ACTION_UP, 2f, 200f)
        assertEquals(listOf(MotionEvent.ACTION_DOWN, MotionEvent.ACTION_UP), fixture.events)
    }

    @Test fun systemCancellationNeverCommitsAnImmediateTurn() {
        val fixture = fixture(native = false)
        fixture.touch(MotionEvent.ACTION_DOWN, 300f, 200f)
        fixture.touch(MotionEvent.ACTION_MOVE, 80f, 200f)
        fixture.touch(MotionEvent.ACTION_CANCEL, 50f, 200f)
        assertTrue(fixture.swipes.isEmpty())
    }

    @Test fun scrollModeAndSelectionKeepTheirNativeGestures() {
        val fixture = fixture(handle = false)
        fixture.touch(MotionEvent.ACTION_DOWN, 300f, 200f)
        fixture.touch(MotionEvent.ACTION_MOVE, 80f, 200f)
        fixture.touch(MotionEvent.ACTION_UP, 50f, 200f)
        assertFalse(fixture.events.contains(MotionEvent.ACTION_CANCEL))
        assertTrue(fixture.events.contains(MotionEvent.ACTION_UP))
        assertTrue(fixture.swipes.isEmpty())
    }

    @Test fun handleDragRemainsNativeAfterSelectionToolbarTemporarilyEnds() {
        var handle = false
        val fixture = fixture(handles = { handle })
        fixture.touch(MotionEvent.ACTION_DOWN, 300f, 200f)
        handle = true
        fixture.touch(MotionEvent.ACTION_MOVE, 80f, 200f)
        fixture.touch(MotionEvent.ACTION_UP, 50f, 200f)
        assertFalse(fixture.events.contains(MotionEvent.ACTION_CANCEL))
        assertEquals(MotionEvent.ACTION_UP, fixture.events.last())
        assertTrue(fixture.swipes.isEmpty())
    }

    @Test fun selectionStartingDuringLongPressKeepsHandleMotionNative() {
        var handle = true
        val fixture = fixture(handles = { handle })
        fixture.touch(MotionEvent.ACTION_DOWN, 300f, 200f)
        handle = false
        fixture.touch(MotionEvent.ACTION_MOVE, 80f, 200f)
        handle = true
        fixture.touch(MotionEvent.ACTION_UP, 50f, 200f)
        assertFalse(fixture.events.contains(MotionEvent.ACTION_CANCEL))
        assertEquals(MotionEvent.ACTION_UP, fixture.events.last())
    }

    @Test fun secondPointerCancelsPagingAndCannotResumeOnPointerUp() {
        val fixture = fixture()
        fixture.touch(MotionEvent.ACTION_DOWN, 400f, 200f)
        fixture.touch(MotionEvent.ACTION_MOVE, 300f, 200f)
        val pointers = Array(2) { MotionEvent.PointerProperties().apply { id = it } }
        val coordinates =
            Array(2) {
                MotionEvent.PointerCoords().apply {
                    x = 300f + it * 60f
                    y = 200f
                }
            }
        val event =
            MotionEvent.obtain(
                0,
                30,
                MotionEvent.ACTION_POINTER_DOWN or (1 shl MotionEvent.ACTION_POINTER_INDEX_SHIFT),
                2,
                pointers,
                coordinates,
                0,
                0,
                1f,
                1f,
                0,
                0,
                0,
                0,
            )
        fixture.container.dispatchTouchEvent(event)
        event.recycle()
        fixture.touch(MotionEvent.ACTION_MOVE, 100f, 200f)
        fixture.touch(MotionEvent.ACTION_UP, 100f, 200f)
        fixture.assertCancelled()
        assertFalse(fixture.events.contains(MotionEvent.ACTION_POINTER_DOWN))
        assertTrue(fixture.swipes.isEmpty())
    }

    private fun fixture(
        handle: Boolean = true,
        native: Boolean = true,
        handles: () -> Boolean = { handle },
    ): Fixture {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val events = mutableListOf<Int>()
        val swipes = mutableListOf<Float>()
        val container =
            ReaderContainer(context, {}, handles, { swipes += it }, { native }).apply {
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
        return Fixture(container, events, swipes)
    }

    private class Fixture(
        val container: ReaderContainer,
        val events: MutableList<Int>,
        val swipes: MutableList<Float>,
    ) {
        private var time = 0L

        fun touch(
            action: Int,
            x: Float,
            y: Float,
        ) {
            time += 10
            val event = MotionEvent.obtain(0, time, action, x, y, 0)
            container.dispatchTouchEvent(event)
            event.recycle()
        }

        fun assertCancelled() {
            assertTrue(events.contains(MotionEvent.ACTION_CANCEL))
            assertFalse(events.contains(MotionEvent.ACTION_UP))
        }
    }
}
