package com.kmab.deft.feature.reader

internal enum class ReadingMotion { PAGE, SCROLL }

/** Separates restored, preference-driven and explicit jump locations from reading gestures. */
internal class ReadingNavigation<T> {
    private var previous: T? = null
    private var pending: ReadingMotion? = null
    private var inputAt: Long = 0
    private val history = ArrayDeque<T>()
    val returnPoint: T? get() = history.lastOrNull()

    fun userInput(
        scroll: Boolean,
        atMillis: Long,
    ) {
        pending = if (scroll) ReadingMotion.SCROLL else ReadingMotion.PAGE
        inputAt = atMillis
    }

    fun cancelInput() {
        pending = null
    }

    fun jumpFrom(location: T?) {
        if (location != null && history.lastOrNull() != location) history.addLast(location)
        pending = null
    }

    fun takeReturnPoint(): T? =
        history.removeLastOrNull().also {
            pending = null
        }

    fun arrived(
        location: T,
        atMillis: Long,
    ): ReadingMotion? {
        val changed = previous != null && previous != location
        previous = location
        if (!changed) return null
        return pending.takeIf { atMillis - inputAt in 0..INPUT_LIFETIME }.also { pending = null }
    }

    private companion object {
        const val INPUT_LIFETIME = 2_000L
    }
}
