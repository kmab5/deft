package com.kmab.deft.feature.reader

import android.os.Bundle
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ReaderRestorationTest {
    @Test fun actualActivitySavedStateRetainsControlsButDoesNotRecreateBookDependentFragments() {
        val first = Robolectric.buildActivity(FragmentActivity::class.java).setup()
        first.get().savedStateRegistry.registerSavedStateProvider("reader-controls") {
            Bundle().apply { putString("sheet", "contents") }
        }
        first
            .get()
            .supportFragmentManager
            .beginTransaction()
            .add(LiveBookFragment("opened-publication"), "book")
            .commitNow()
        val saved = Bundle().apply { putString("reader-location", "chapter-two") }
        first
            .pause()
            .saveInstanceState(saved)
            .stop()
            .destroy()

        // Assert against a real AndroidX save operation, so a changed registry key breaks this test.
        val registryKey = "androidx.lifecycle.BundlableSavedStateRegistry.key"
        assertTrue(saved.getBundle(registryKey)!!.containsKey("android:support:fragments"))
        val restored = saved.withoutReaderFragmentState()
        assertTrue(saved.getBundle(registryKey)!!.containsKey("android:support:fragments"))
        assertFalse(restored.getBundle(registryKey)!!.containsKey("android:support:fragments"))
        assertEquals("chapter-two", restored.getString("reader-location"))

        val second =
            Robolectric
                .buildActivity(FragmentActivity::class.java)
                .create(restored)
                .start()
                .resume()
        try {
            assertTrue(
                second
                    .get()
                    .supportFragmentManager.fragments
                    .isEmpty(),
            )
            assertEquals(
                "contents",
                second
                    .get()
                    .savedStateRegistry
                    .consumeRestoredStateForKey("reader-controls")
                    ?.getString("sheet"),
            )
        } finally {
            second.pause().stop().destroy()
        }
    }

    // Like Readium's navigator, this cannot be reconstructed without its live publication.
    class LiveBookFragment(
        val publication: String,
    ) : Fragment()
}
