package com.kmab.deft.feature.reader

internal enum class ReadingMotion { PAGE, SCROLL }

/** Separates restored, preference-driven and explicit jump locations from reading gestures. */
internal class ReadingNavigation<T> {
    private var previous: T? = null
    private var pending: ReadingMotion? = null
    private var inputAt: Long = 0
    var returnPoint: T? = null
        private set

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
        returnPoint = location
        pending = null
    }

    fun takeReturnPoint(): T? =
        returnPoint.also {
            returnPoint = null
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
