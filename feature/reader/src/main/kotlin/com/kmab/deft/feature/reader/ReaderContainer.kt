package com.kmab.deft.feature.reader

import android.content.Context
import android.view.MotionEvent
import android.view.ViewConfiguration
import android.widget.FrameLayout
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import kotlin.math.abs
import kotlin.math.max

/** Routes whole touch streams, leaving Slide paging and selection in the native WebView. */
internal class ReaderContainer(
    context: Context,
    private val onReadingGesture: () -> Unit,
    private val handlesSwipes: () -> Boolean,
    private val onSwipe: (Float) -> Unit,
    private val nativeSwipes: () -> Boolean = { true },
) : FrameLayout(context) {
    private enum class Route { PENDING, NATIVE_PAGE, IMMEDIATE_PAGE, BLOCKED, CONTENT }

    private val slop = ViewConfiguration.get(context).scaledTouchSlop
    private var route = Route.PENDING
    private var startX = 0f
    private var startY = 0f
    private var furthestOffset = 0f
    private var systemEdge = false

    override fun dispatchTouchEvent(event: MotionEvent): Boolean {
        if (event.actionMasked == MotionEvent.ACTION_DOWN) {
            startX = event.x
            startY = event.y
            furthestOffset = 0f
            systemEdge = startsAtSystemEdge(event)
            route = if (handlesSwipes()) Route.PENDING else Route.CONTENT
        }
        // A long press may start selection after DOWN. Once a handle owns a gesture,
        // toolbar recreation must not let paging steal the remainder of that stream.
        if (route == Route.PENDING && !handlesSwipes()) route = Route.CONTENT
        if (route == Route.CONTENT) return super.dispatchTouchEvent(event)

        if (event.pointerCount > 1 && route != Route.BLOCKED) {
            cancelContent(event)
            route = Route.BLOCKED
        }
        when (event.actionMasked) {
            MotionEvent.ACTION_MOVE -> observeMove(event)
            MotionEvent.ACTION_UP -> finishGesture(event)
        }
        return when (route) {
            Route.BLOCKED, Route.IMMEDIATE_PAGE -> true
            else -> super.dispatchTouchEvent(event)
        }
    }

    private fun observeMove(event: MotionEvent) {
        val offset = event.x - startX
        val dx = abs(offset)
        val dy = abs(event.y - startY)
        val moved = dx > slop || dy > slop
        if (!systemEdge && moved) onReadingGesture()
        if (route == Route.PENDING && moved) classifyMove(event, dx, dy)
        val paging = route == Route.NATIVE_PAGE || route == Route.IMMEDIATE_PAGE
        if (paging && isVertical(dx, dy)) {
            if (route == Route.NATIVE_PAGE) cancelContent(event)
            route = Route.BLOCKED
        }
        if (dx > abs(furthestOffset)) furthestOffset = offset
    }

    private fun classifyMove(
        event: MotionEvent,
        dx: Float,
        dy: Float,
    ) {
        route =
            when {
                systemEdge || dx <= dy * HORIZONTAL_GESTURE_RATIO -> Route.BLOCKED
                nativeSwipes() -> Route.NATIVE_PAGE
                else -> Route.IMMEDIATE_PAGE
            }
        if (route != Route.NATIVE_PAGE) cancelContent(event)
    }

    private fun finishGesture(event: MotionEvent) {
        val offset = event.x - startX
        val cancelled =
            isVertical(abs(offset), abs(event.y - startY)) ||
                offset * furthestOffset <= 0f || abs(offset) < abs(furthestOffset) / 2f
        when (route) {
            Route.NATIVE_PAGE ->
                if (cancelled) {
                    cancelContent(event)
                    route = Route.BLOCKED
                }
            Route.IMMEDIATE_PAGE ->
                if (!cancelled && abs(offset) >= MINIMUM_SWIPE_DP * resources.displayMetrics.density) {
                    onSwipe(offset)
                }
            else -> Unit
        }
    }

    private fun isVertical(
        dx: Float,
        dy: Float,
    ): Boolean = dy > slop && dy > dx * HORIZONTAL_GESTURE_RATIO

    private fun cancelContent(event: MotionEvent) {
        val cancel = MotionEvent.obtain(event).apply { action = MotionEvent.ACTION_CANCEL }
        super.dispatchTouchEvent(cancel)
        cancel.recycle()
    }

    private fun startsAtSystemEdge(event: MotionEvent): Boolean {
        val insets = ViewCompat.getRootWindowInsets(this)?.getInsets(WindowInsetsCompat.Type.systemGestures())
        // Immersive mode can report zero while the first edge swipe reveals system bars.
        // Reserve that same narrow back-gesture strip even while the bars are hidden.
        val fallback = SYSTEM_EDGE_DP * resources.displayMetrics.density
        val left = max(fallback, insets?.left?.toFloat() ?: 0f)
        val right = max(fallback, insets?.right?.toFloat() ?: 0f)
        return event.x <= left || event.x >= width - right ||
            (insets != null && (event.y < insets.top || event.y > height - insets.bottom))
    }
}

private const val HORIZONTAL_GESTURE_RATIO = 1.25f
private const val MINIMUM_SWIPE_DP = 32
private const val SYSTEM_EDGE_DP = 24
