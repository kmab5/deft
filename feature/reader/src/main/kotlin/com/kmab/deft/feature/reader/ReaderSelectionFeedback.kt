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
    private val onAdjusted: () -> Unit = {},
    private val onStarted: (Locator) -> Unit,
) : DefaultLifecycleObserver {
    private var toolbar: Any? = null
    private var observer: Job? = null
    private var active = false
    private var closed = false
    private var touching = false
    private var lastRange: String? = null

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

    fun touchChanged(down: Boolean) {
        if (closed) return
        touching = down
        if (down && lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)) startObservation()
    }

    fun close() {
        closed = true
        observer?.cancel()
        toolbar = null
        touching = false
        lifecycle.removeObserver(this)
    }

    override fun onResume(owner: LifecycleOwner) {
        if (active || hasToolbar) startObservation()
    }

    override fun onPause(owner: LifecycleOwner) {
        // A range that survives a pause belongs to the selection already in progress.
        active = active || hasToolbar || observer?.isActive == true
        touching = false
        lastRange = null
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
            val sample = readSelectionSample(readSelection)
            currentCoroutineContext().ensureActive()
            val failure = sample.exceptionOrNull()
            if (failure != null) {
                if (!readFailed) Log.w(TAG, "The selection is temporarily unavailable.", failure)
                readFailed = true
                emptySamples = 0
                delay(SAMPLE_INTERVAL_MS)
                continue
            }
            readFailed = false
            emptySamples = applySample(sample.getOrThrow(), emptySamples)
            if (active || hasToolbar || touching) {
                delay(SAMPLE_INTERVAL_MS)
            }
        } while (active || hasToolbar || touching)
    }

    private fun applySample(
        selection: Locator?,
        emptySamples: Int,
    ): Int {
        if (selection != null) {
            val range =
                selection.href.toString() + selection.text.highlight +
                    selection
                        .toJSON()
                        .optJSONObject("locations")
                        ?.opt("domRange")
                        ?.toString()
                        .orEmpty()
            if (!active) {
                onStarted(selection)
            } else if (lastRange != null && lastRange != range) {
                onAdjusted()
            }
            lastRange = range
            active = true
            return 0
        }
        val nextEmptySamples = emptySamples + 1
        if (nextEmptySamples >= CLEAR_SAMPLES) {
            active = false
            lastRange = null
        }
        return nextEmptySamples
    }

    private companion object {
        const val TAG = "ReaderSelection"
        const val SAMPLE_INTERVAL_MS = 50L
        const val CLEAR_SAMPLES = 2
    }
}

@Suppress("TooGenericExceptionCaught")
private suspend fun readSelectionSample(read: suspend () -> Locator?): Result<Locator?> =
    try {
        Result.success(read()?.takeUnless { it.text.highlight.isNullOrBlank() })
    } catch (cancelled: CancellationException) {
        throw cancelled
    } catch (error: Exception) {
        Result.failure(error)
    }
