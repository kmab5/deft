package com.kmab.deft.feature.reader

import android.content.Intent
import android.content.pm.ActivityInfo
import android.content.pm.ResolveInfo
import android.view.View
import android.widget.PopupMenu
import androidx.fragment.app.FragmentActivity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class ReaderTextProcessorsTest {
    @Test fun refreshWhileReadingSelectionCannotRetargetTheTappedAction() {
        val controller = Robolectric.buildActivity(FragmentActivity::class.java).setup()
        val activity = controller.get()
        val manager = shadowOf(activity.packageManager)
        val query = Intent(Intent.ACTION_PROCESS_TEXT).setType("text/plain")
        val processors = ReaderTextProcessors(activity) { error(it) }
        val menu = PopupMenu(activity, View(activity)).menu
        try {
            manager.addResolveInfoForIntent(query, target("Translate", "translator"))
            processors.populate(menu)
            val tapped = requireNotNull(processors.target(menu.getItem(0)))
            manager.removeResolveInfosForIntent(query, "translator")
            manager.addResolveInfoForIntent(query, target("Dictionary", "dictionary"))
            menu.clear()
            processors.populate(menu)
            assertEquals("Dictionary", menu.getItem(0).title)
            assertTrue(processors.launch(tapped, "adjusted passage"))
            val launched = shadowOf(activity).nextStartedActivity
            assertEquals("translator", launched.component?.packageName)
            assertEquals("adjusted passage", launched.getStringExtra(Intent.EXTRA_PROCESS_TEXT))
        } finally {
            controller.pause().stop().destroy()
        }
    }

    private fun target(
        label: String,
        packageName: String,
    ) = ResolveInfo().apply {
        nonLocalizedLabel = label
        activityInfo =
            ActivityInfo().apply {
                this.packageName = packageName
                name = "$packageName.ProcessText"
                exported = true
                enabled = true
            }
    }
}
