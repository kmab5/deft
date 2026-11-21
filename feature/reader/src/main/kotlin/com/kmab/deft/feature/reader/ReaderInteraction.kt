package com.kmab.deft.feature.reader

import android.os.SystemClock
import com.kmab.deft.core.model.ReaderPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import org.readium.r2.shared.publication.Locator

/**
 * Owns navigator generations and user-interaction classification, independently of opening books.
 * Navigator and lifecycle callbacks share one classification and visibility state.
 */
@Suppress("TooManyFunctions")
internal class ReaderInteraction(
    private val state: MutableStateFlow<ReaderState>,
    private val preferences: StateFlow<ReaderPreferences>,
    private val writer: ReaderPositionWriter,
    private val events: ReaderEvents,
) {
    private val navigation = ReadingNavigation<Locator>()
    private var generation = 0L
    private val visibility =
        ReaderVisibility { visible ->
            val current = state.value
            emitVisibility(current, events, visible)
            if (!visible) {
                navigation.cancelInput()
                val position = current.locator?.toPosition()
                val session = current.session
                if (position != null && session != null) writer.save(session.bookId, position)
            }
        }
    private val visible get() = visibility.visible

    fun attachNavigator(): Long {
        generation++
        renderingFailed()
        return generation
    }

    fun detachNavigator(token: Long) {
        if (token == generation) {
            generation++
            renderingFailed()
        }
    }

    fun rendered(
        token: Long,
        locator: Locator,
    ) {
        if (token != generation) return
        if (!state.value.rendered) {
            state.value.session?.let { writer.save(it.bookId, locator.toPosition()) }
            state.update { it.copy(rendered = true, locator = locator) }
            visibility.rendered(true)
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
                    null -> events.emit(ReaderEvent.Location(session.bookId, locator.toPosition()))
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
            state.update { it.copy(canReturn = navigation.returnPoint != null) }
        }

    fun selection(locator: Locator) {
        if (visible && state.value.rendered) {
            state.value.session?.let { events.emit(ReaderEvent.Selection(it.bookId, locator.toPosition())) }
        }
    }

    fun resumeReading() {
        val current = state.value
        val book = current.session?.bookId
        val position = current.locator?.toPosition()
        if (visible && current.rendered) {
            if (book != null && position != null) events.emit(ReaderEvent.Resume(book, position))
        }
    }

    fun renderingFailed() {
        visibility.rendered(false)
        state.update { it.copy(rendered = false) }
        navigation.cancelInput()
    }

    fun obscured(value: Boolean) = visibility.obscured(value)

    fun visibility(isVisible: Boolean) = visibility.resumed(isVisible)
}

private fun emitVisibility(
    state: ReaderState,
    events: ReaderEvents,
    visible: Boolean,
) {
    state.session?.let { events.emit(ReaderEvent.Visibility(it.bookId, state.locator?.toPosition(), visible)) }
}
