package com.kmab.deft.feature.reader

import android.content.Intent
import android.content.pm.ActivityInfo
import android.content.pm.ApplicationInfo
import android.content.pm.ResolveInfo
import android.graphics.Rect
import android.os.Looper
import android.view.ActionMode
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.widget.PopupMenu
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.lifecycleScope
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.readium.r2.shared.publication.Locator
import org.readium.r2.shared.util.Url
import org.readium.r2.shared.util.mediatype.MediaType
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class ReaderSelectionMenuTest {
    @Test fun continuousMenuKeepsTheWebViewSelectionRectangle() {
        val controller = Robolectric.buildActivity(FragmentActivity::class.java).setup()
        val activity = controller.get()
        val feedback = ReaderSelectionFeedback(activity.lifecycleScope, activity.lifecycle, { null }) {}
        val expected = Rect(110, 430, 190, 460)
        val delegate =
            object : ActionMode.Callback2() {
                override fun onCreateActionMode(
                    mode: ActionMode,
                    menu: Menu,
                ) = true

                override fun onPrepareActionMode(
                    mode: ActionMode,
                    menu: Menu,
                ) = false

                override fun onActionItemClicked(
                    mode: ActionMode,
                    item: MenuItem,
                ) = false

                override fun onDestroyActionMode(mode: ActionMode) = Unit

                override fun onGetContentRect(
                    mode: ActionMode,
                    view: View,
                    outRect: Rect,
                ) = outRect.set(expected)
            }
        try {
            val callback =
                readerSelectionCallback(activity, feedback, ReaderSelectionOwner({ null }, {}, { _, _ -> }), delegate)
            assertTrue("Preserve WebView's floating-menu geometry callback", callback is ActionMode.Callback2)
            val actual = Rect()
            (callback as ActionMode.Callback2).onGetContentRect(TestSelectionMode(activity), View(activity), actual)
            assertEquals(expected, actual)
            expected.offset(0, -260)
            callback.onGetContentRect(TestSelectionMode(activity), View(activity), actual)
            assertEquals("Geometry follows scrolling and handle movement", expected, actual)
        } finally {
            feedback.close()
            controller.pause().stop().destroy()
        }
    }

    @Test fun installedTextActionsUseTheirLabelsAndFinalReadOnlySelection() {
        val controller = Robolectric.buildActivity(FragmentActivity::class.java).setup()
        val activity = controller.get()
        val feedback = ReaderSelectionFeedback(activity.lifecycleScope, activity.lifecycle, { null }) {}
        val query = Intent(Intent.ACTION_PROCESS_TEXT).setType("text/plain")
        shadowOf(activity.packageManager).addResolveInfoForIntent(query, target("Translate", "translator", true))
        shadowOf(activity.packageManager).addResolveInfoForIntent(query, target("Private", "private", false))
        var text = "original word"
        var cleared = false
        val owner =
            ReaderSelectionOwner(
                {
                    Locator(
                        requireNotNull(Url("chapter.xhtml")),
                        MediaType.XHTML,
                        text = Locator.Text(highlight = text),
                    )
                },
                { cleared = true },
                { _, _ -> error("External target is not a built-in reader action") },
            )
        try {
            val mode = TestSelectionMode(activity)
            val callback = readerSelectionCallback(activity, feedback, owner)
            callback.onCreateActionMode(mode, mode.menu)
            val titles =
                (0 until mode.menu.size()).map {
                    mode.menu
                        .getItem(it)
                        .title
                        .toString()
                }
            assertTrue("Installed text-processing app is in the selection menu", "Translate" in titles)
            assertTrue("Private activities stay hidden", "Private" !in titles)
            text = "final adjusted passage"
            val item = (0 until mode.menu.size()).map(mode.menu::getItem).single { it.title == "Translate" }
            assertTrue(callback.onActionItemClicked(mode, item))
            shadowOf(Looper.getMainLooper()).idle()
            val launched = shadowOf(activity).nextStartedActivity
            assertEquals("translator", launched.component?.packageName)
            assertEquals(Intent.ACTION_PROCESS_TEXT, launched.action)
            assertEquals(text, launched.getStringExtra(Intent.EXTRA_PROCESS_TEXT))
            assertTrue(launched.getBooleanExtra(Intent.EXTRA_PROCESS_TEXT_READONLY, false))
            assertTrue(cleared)
            assertTrue(mode.finished)
        } finally {
            feedback.close()
            controller.pause().stop().destroy()
        }
    }

    private fun target(
        label: String,
        packageName: String,
        exported: Boolean,
    ) = ResolveInfo().apply {
        nonLocalizedLabel = label
        activityInfo =
            ActivityInfo().apply {
                this.packageName = packageName
                name = "$packageName.ProcessText"
                this.exported = exported
                enabled = true
                applicationInfo = ApplicationInfo().apply { this.packageName = packageName }
            }
    }
}

private class TestSelectionMode(
    private val activity: FragmentActivity,
) : ActionMode() {
    private val items = PopupMenu(activity, View(activity)).menu
    var finished = false

    override fun setTitle(title: CharSequence?) = Unit

    override fun setTitle(resId: Int) = Unit

    override fun setSubtitle(subtitle: CharSequence?) = Unit

    override fun setSubtitle(resId: Int) = Unit

    override fun setCustomView(view: View?) = Unit

    override fun invalidate() = Unit

    override fun finish() {
        finished = true
    }

    override fun getMenu(): Menu = items

    override fun getTitle(): CharSequence = ""

    override fun getSubtitle(): CharSequence = ""

    override fun getCustomView(): View? = null

    override fun getMenuInflater() = MenuInflater(activity)
}
