package com.kmab.deft.feature.reader

import android.os.SystemClock
import com.kmab.deft.core.model.ReaderPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import org.readium.r2.shared.publication.Locator

/** Owns navigator generations and user-interaction classification, independently of opening books. */
internal class ReaderInteraction(
    private val state: MutableStateFlow<ReaderState>,
    private val preferences: StateFlow<ReaderPreferences>,
    private val writer: ReaderPositionWriter,
    private val events: ReaderEvents,
) {
    private val navigation = ReadingNavigation<Locator>()
    private var generation = 0L
    private var visible = false

    fun attachNavigator(): Long {
        generation++
        navigation.cancelInput()
        return generation
    }

    fun detachNavigator(token: Long) {
        if (token == generation) generation++
    }

    fun rendered(
        token: Long,
        locator: Locator,
    ) {
        if (token != generation) return
        if (!state.value.rendered) {
            state.value.session?.let { writer.save(it.bookId, locator.toPosition()) }
            state.update { it.copy(rendered = true) }
            if (visible) emitVisibility(state.value, events, true)
        }
        location(token, locator)
    }

    fun location(
        token: Long,
        locator: Locator,
    ) {
        val current = state.value
        val session = current.session
        if (token != generation || session == null) return
        state.update { it.copy(locator = locator) }
        val motion = navigation.arrived(locator, SystemClock.elapsedRealtime())
        if (current.rendered) {
            if (current.locator != locator) writer.save(session.bookId, locator.toPosition())
            if (visible) {
                when (motion) {
                    ReadingMotion.PAGE -> events.emit(ReaderEvent.PageTurn(session.bookId, locator.toPosition()))
                    ReadingMotion.SCROLL -> events.emit(ReaderEvent.ScrollStep(session.bookId, locator.toPosition()))
                    null -> Unit
                }
            }
        }
    }

    fun userInput() = navigation.userInput(preferences.value.scroll, SystemClock.elapsedRealtime())

    fun preferencesChanged() = navigation.cancelInput()

    fun beforeJump() {
        navigation.jumpFrom(state.value.locator)
        state.update { it.copy(canReturn = navigation.returnPoint != null) }
    }

    fun returnPoint(): Locator? =
        navigation.takeReturnPoint().also {
            state.update {
                it.copy(
                    canReturn =
                        navigation.returnPoint != null,
                )
            }
        }

    fun selection(locator: Locator) {
        state.value.session?.let { events.emit(ReaderEvent.Selection(it.bookId, locator.toPosition())) }
    }

    fun visibility(isVisible: Boolean) {
        if (visible == isVisible) return
        visible = isVisible
        val current = state.value
        if (current.rendered) emitVisibility(current, events, isVisible)
        if (!isVisible) {
            navigation.cancelInput()
            val position = current.locator?.toPosition()
            val session = current.session
            if (current.rendered && position != null && session != null) writer.save(session.bookId, position)
        }
    }
}

private fun emitVisibility(
    state: ReaderState,
    events: ReaderEvents,
    visible: Boolean,
) {
    state.session?.let { events.emit(ReaderEvent.Visibility(it.bookId, state.locator?.toPosition(), visible)) }
}
