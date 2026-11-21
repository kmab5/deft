package com.kmab.deft.feature.reader

/** A reader is eligible only when all three independent lifecycle/rendering gates are open. */
internal class ReaderVisibility(
    private val changed: (Boolean) -> Unit,
) {
    private var resumed = false
    private var rendered = false
    private var obscured = false
    var visible = false
        private set

    fun resumed(value: Boolean) {
        resumed = value
        update()
    }

    fun rendered(value: Boolean) {
        rendered = value
        update()
    }

    fun obscured(value: Boolean) {
        obscured = value
        update()
    }

    private fun update() {
        val next = resumed && rendered && !obscured
        if (next == visible) return
        visible = next
        changed(next)
    }
}
