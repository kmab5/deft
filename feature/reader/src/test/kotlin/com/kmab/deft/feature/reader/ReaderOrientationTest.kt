package com.kmab.deft.feature.reader

import android.app.Activity
import android.content.pm.ActivityInfo
import com.kmab.deft.core.model.ReaderOrientation
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class ReaderOrientationTest {
    @Test fun rotationChoiceAppliesAndCanReturnToSystemControl() {
        val controller = Robolectric.buildActivity(Activity::class.java).setup()
        try {
            val activity = controller.get()
            activity.applyReaderOrientation(ReaderOrientation.PORTRAIT)
            assertEquals(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT, activity.requestedOrientation)
            activity.applyReaderOrientation(ReaderOrientation.LANDSCAPE)
            assertEquals(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE, activity.requestedOrientation)
            activity.applyReaderOrientation(ReaderOrientation.SYSTEM)
            assertEquals(ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED, activity.requestedOrientation)
        } finally {
            controller.pause().stop().destroy()
        }
    }
}
