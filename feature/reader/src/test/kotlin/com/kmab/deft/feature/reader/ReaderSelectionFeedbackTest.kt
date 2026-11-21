package com.kmab.deft.feature.reader

import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.readium.r2.shared.publication.Locator
import org.readium.r2.shared.util.Url
import org.readium.r2.shared.util.mediatype.MediaType
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
@OptIn(ExperimentalCoroutinesApi::class)
class ReaderSelectionFeedbackTest {
    @Test fun waitsForRealSelectionAndDoesNotRepeatOnHandlesOrToolbarRecreation() =
        runTest {
            var current: Locator? = null
            val starts = mutableListOf<String?>()
            val feedback =
                ReaderSelectionFeedback(backgroundScope, ReaderLifecycle().lifecycle, { current }) {
                    starts += it.text.highlight
                }
            val firstToolbar = Any()
            feedback.toolbarStarted(firstToolbar)
            advanceTimeBy(130)
            assertEquals(emptyList<String>(), starts)

            current = point("word")
            advanceTimeBy(130)
            assertEquals(listOf("word"), starts)

            current = null
            val recreated = Any()
            feedback.toolbarStarted(recreated)
            feedback.toolbarStopped(firstToolbar)
            advanceTimeBy(55)
            current = point("word and")
            advanceTimeBy(130)
            assertEquals(listOf("word"), starts)

            feedback.toolbarStopped(recreated)
            current = null
            advanceTimeBy(300)
            current = point("second word")
            feedback.toolbarStarted(Any())
            advanceTimeBy(130)
            assertEquals(listOf("word", "second word"), starts)
        }

    @Test fun emptyRangeAndClosedNavigatorNeverPulse() =
        runTest {
            var current: Locator? = point(" ")
            var starts = 0
            val feedback =
                ReaderSelectionFeedback(backgroundScope, ReaderLifecycle().lifecycle, { current }) { starts++ }
            feedback.toolbarStarted(Any())
            advanceTimeBy(300)
            assertEquals(0, starts)
            feedback.close()
            current = point("stale selection")
            advanceTimeBy(300)
            assertEquals(0, starts)
        }

    @Test fun transientSelectionReadFailureRetriesWithoutSendingAnExtraPulse() =
        runTest {
            var reads = 0
            val starts = mutableListOf<String?>()
            val read = {
                reads++
                if (reads == 1 || reads == 3) error("WebView temporarily unavailable")
                point("word")
            }
            val feedback =
                ReaderSelectionFeedback(backgroundScope, ReaderLifecycle().lifecycle, { read() }) {
                    starts += it.text.highlight
                }
            feedback.toolbarStarted(Any())
            advanceTimeBy(400)
            assertEquals(listOf("word"), starts)
        }

    @Test fun pausedReaderDoesNotReadAndResumeDoesNotPulseAnExistingSelection() =
        runTest {
            val owner = ReaderLifecycle()
            var current: Locator? = null
            var reads = 0
            val starts = mutableListOf<String?>()
            val read = {
                reads++
                current
            }
            val feedback =
                ReaderSelectionFeedback(backgroundScope, owner.lifecycle, { read() }) {
                    starts += it.text.highlight
                }
            feedback.toolbarStarted(Any())
            advanceTimeBy(110)
            owner.lifecycle.handleLifecycleEvent(Lifecycle.Event.ON_PAUSE)
            val readsAtPause = reads
            current = point("old range")
            advanceTimeBy(300)
            assertEquals(readsAtPause, reads)
            assertEquals(emptyList<String>(), starts)

            owner.lifecycle.handleLifecycleEvent(Lifecycle.Event.ON_STOP)
            advanceTimeBy(300)
            assertEquals(readsAtPause, reads)
            owner.lifecycle.handleLifecycleEvent(Lifecycle.Event.ON_START)
            owner.lifecycle.handleLifecycleEvent(Lifecycle.Event.ON_RESUME)
            advanceTimeBy(150)
            assertEquals(emptyList<String>(), starts)
            current = null
            advanceTimeBy(200)
            current = point("new range")
            advanceTimeBy(150)
            assertEquals(listOf("new range"), starts)
            feedback.close()
        }

    private fun point(text: String) =
        Locator(
            href = requireNotNull(Url("chapter.xhtml")),
            mediaType = MediaType.XHTML,
            text = Locator.Text(highlight = text),
        )

    private class ReaderLifecycle : LifecycleOwner {
        override val lifecycle =
            LifecycleRegistry.createUnsafe(this).apply {
                currentState = Lifecycle.State.RESUMED
            }
    }
}
