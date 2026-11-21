package com.kmab.deft.feature.reader

import android.util.Log
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.launch
import org.readium.r2.shared.publication.Locator

/** The floating toolbar can start before its range exists and be recreated during a handle drag. */
internal class ReaderSelectionFeedback(
    private val scope: CoroutineScope,
    private val lifecycle: Lifecycle,
    private val readSelection: suspend () -> Locator?,
    private val onStarted: (Locator) -> Unit,
) : DefaultLifecycleObserver {
    private var toolbar: Any? = null
    private var observer: Job? = null
    private var active = false
    private var closed = false

    val hasToolbar: Boolean get() = toolbar != null

    init {
        lifecycle.addObserver(this)
    }

    fun toolbarStarted(mode: Any) {
        if (closed) return
        toolbar = mode
        if (lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)) {
            startObservation()
        } else {
            active = true
        }
    }

    fun toolbarStopped(mode: Any) {
        if (toolbar === mode) toolbar = null
    }

    fun close() {
        closed = true
        observer?.cancel()
        toolbar = null
        lifecycle.removeObserver(this)
    }

    override fun onResume(owner: LifecycleOwner) {
        if (active || hasToolbar) startObservation()
    }

    override fun onPause(owner: LifecycleOwner) {
        // A range that survives a pause belongs to the selection already in progress.
        active = active || hasToolbar || observer?.isActive == true
        observer?.cancel()
    }

    override fun onDestroy(owner: LifecycleOwner) = close()

    private fun startObservation() {
        if (observer?.isActive != true) observer = scope.launch { observe() }
    }

    private suspend fun observe() {
        var emptySamples = 0
        var readFailed = false
        do {
            val sample = readSample()
            currentCoroutineContext().ensureActive()
            val failure = sample.exceptionOrNull()
            if (failure != null) {
                if (!readFailed) Log.w(TAG, "The selection is temporarily unavailable.", failure)
                readFailed = true
                emptySamples = 0
                delay(if (hasToolbar) SAMPLE_INTERVAL_MS else NO_TOOLBAR_INTERVAL_MS)
                continue
            }
            readFailed = false
            emptySamples = applySample(sample.getOrThrow(), emptySamples)
            if (active || hasToolbar) {
                delay(if (hasToolbar || emptySamples > 0) SAMPLE_INTERVAL_MS else NO_TOOLBAR_INTERVAL_MS)
            }
        } while (active || hasToolbar)
    }

    @Suppress("TooGenericExceptionCaught")
    private suspend fun readSample(): Result<Locator?> =
        try {
            Result.success(readSelection()?.takeUnless { it.text.highlight.isNullOrBlank() })
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (error: Exception) {
            Result.failure(error)
        }

    private fun applySample(
        selection: Locator?,
        emptySamples: Int,
    ): Int {
        if (selection != null) {
            if (!active) onStarted(selection)
            active = true
            return 0
        }
        val nextEmptySamples = emptySamples + 1
        if (nextEmptySamples >= CLEAR_SAMPLES) active = false
        return nextEmptySamples
    }

    private companion object {
        const val TAG = "ReaderSelection"
        const val SAMPLE_INTERVAL_MS = 75L
        const val NO_TOOLBAR_INTERVAL_MS = 250L
        const val CLEAR_SAMPLES = 2
    }
}
