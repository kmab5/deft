package com.kmab.deft.feature.reader

import com.kmab.deft.core.model.ReadingPosition
import com.kmab.deft.library.scanner.BookRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

/** Application-owned serial writes survive Activity/ViewModel destruction without reordering saves. */
@Singleton
internal class ReaderPositionWriter
    @Inject
    constructor(
        private val books: BookRepository,
    ) {
        private val pending = PendingPositions()
        private val wakeup = Channel<Unit>(Channel.CONFLATED)
        private val errors = MutableSharedFlow<String>(extraBufferCapacity = 1)
        val failures = errors.asSharedFlow()

        init {
            CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
                while (wakeup.receiveCatching().isSuccess) {
                    while (true) {
                        val (id, position) = pending.take() ?: break
                        try {
                            books.savePosition(id, position)
                        } catch (cancel: CancellationException) {
                            throw cancel
                        } catch (_: Exception) {
                            errors.emit("Your reading position could not be saved. Check available storage.")
                        }
                    }
                }
            }
        }

        fun save(
            bookId: Long,
            position: ReadingPosition,
        ) {
            pending.offer(bookId, position)
            wakeup.trySend(Unit)
        }
    }

internal class PendingPositions {
    private val positions = linkedMapOf<Long, ReadingPosition>()

    @Synchronized fun offer(
        bookId: Long,
        position: ReadingPosition,
    ) {
        positions[bookId] = position
    }

    @Synchronized fun take(): Pair<Long, ReadingPosition>? =
        positions.entries.firstOrNull()?.let { entry ->
            (entry.key to entry.value).also { positions.remove(entry.key) }
        }
}
