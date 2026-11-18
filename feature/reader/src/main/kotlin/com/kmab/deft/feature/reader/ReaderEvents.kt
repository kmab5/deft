package com.kmab.deft.feature.reader

import com.kmab.deft.core.model.ReadingPosition
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import javax.inject.Inject
import javax.inject.Singleton

sealed interface ReaderEvent {
    val bookId: Long
    val position: ReadingPosition?

    data class PageTurn(
        override val bookId: Long,
        override val position: ReadingPosition,
    ) : ReaderEvent

    data class ScrollStep(
        override val bookId: Long,
        override val position: ReadingPosition,
    ) : ReaderEvent

    data class Selection(
        override val bookId: Long,
        override val position: ReadingPosition,
    ) : ReaderEvent

    data class Visibility(
        override val bookId: Long,
        override val position: ReadingPosition?,
        val visible: Boolean,
    ) : ReaderEvent
}

/** Activity signals only. M3 will own session policy and elapsed-time recording. */
@Singleton
class ReaderEvents
    @Inject
    constructor() {
        private val mutableEvents =
            MutableSharedFlow<ReaderEvent>(extraBufferCapacity = 64, onBufferOverflow = BufferOverflow.DROP_OLDEST)
        val events: SharedFlow<ReaderEvent> = mutableEvents.asSharedFlow()

        internal fun emit(event: ReaderEvent) {
            mutableEvents.tryEmit(event)
        }
    }
