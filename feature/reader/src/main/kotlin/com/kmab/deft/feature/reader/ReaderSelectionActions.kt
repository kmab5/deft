package com.kmab.deft.feature.reader

import android.content.ActivityNotFoundException
import android.content.ClipData
import android.content.ClipboardManager
import android.content.ComponentName
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Rect
import android.view.ActionMode
import android.view.Menu
import android.view.MenuItem
import android.view.View
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import org.readium.r2.shared.publication.Locator

internal enum class ReaderSelectionAction(
    val title: String,
) {
    HIGHLIGHT("Highlight"),
    NOTE("Note"),
    COPY("Copy"),
    SHARE("Share"),
    DEFINE("Define"),
    SEARCH("Search in book"),
}

internal data class ReaderSelectionOwner(
    val selection: suspend () -> Locator?,
    val clear: () -> Unit,
    val action: (ReaderSelectionAction, Locator) -> Unit,
)

internal enum class ReaderSelectionResult { EDITOR, SEARCH, EXTERNAL, NONE }

internal fun handleReaderSelection(
    activity: FragmentActivity,
    model: ReaderViewModel,
    action: ReaderSelectionAction,
    locator: Locator,
): ReaderSelectionResult {
    val session = model.state.value.session ?: return ReaderSelectionResult.NONE
    return when (action) {
        ReaderSelectionAction.HIGHLIGHT, ReaderSelectionAction.NOTE -> {
            model.annotations.select(session.bookId, locator, action == ReaderSelectionAction.HIGHLIGHT)
            ReaderSelectionResult.EDITOR
        }
        ReaderSelectionAction.SEARCH -> {
            model.search.query(locator.text.highlight.orEmpty(), session.opened.publication)
            ReaderSelectionResult.SEARCH
        }
        else ->
            if (activity.externalSelectionAction(action, locator.text.highlight.orEmpty(), model::notice)) {
                ReaderSelectionResult.EXTERNAL
            } else {
                ReaderSelectionResult.NONE
            }
    }
}

internal fun readerSelectionCallback(
    activity: FragmentActivity,
    feedback: ReaderSelectionFeedback,
    owner: ReaderSelectionOwner,
    delegate: ActionMode.Callback? = null,
    notice: (String) -> Unit = {},
): ActionMode.Callback =
    object : ActionMode.Callback2() {
        private val processors = ReaderTextProcessors(activity, notice)

        override fun onGetContentRect(
            mode: ActionMode,
            view: View,
            outRect: Rect,
        ) {
            if (delegate is ActionMode.Callback2) {
                delegate.onGetContentRect(mode, view, outRect)
            } else {
                super.onGetContentRect(mode, view, outRect)
            }
        }

        private fun populate(menu: Menu) {
            menu.clear()
            ReaderSelectionAction.entries.forEach { value ->
                menu
                    .add(Menu.NONE, value.ordinal + SELECTION_ITEM_BASE, value.ordinal, value.title)
                    .setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM)
            }
            processors.populate(menu)
        }

        override fun onCreateActionMode(
            mode: ActionMode,
            menu: Menu,
        ): Boolean {
            if (delegate?.onCreateActionMode(mode, menu) == false) return false
            populate(menu)
            feedback.toolbarStarted(mode)
            return true
        }

        override fun onPrepareActionMode(
            mode: ActionMode,
            menu: Menu,
        ): Boolean {
            populate(menu)
            return true
        }

        override fun onActionItemClicked(
            mode: ActionMode,
            item: MenuItem,
        ): Boolean {
            val value = ReaderSelectionAction.entries.getOrNull(item.itemId - SELECTION_ITEM_BASE)
            val target = processors.target(item)
            if (value == null && target == null) return false
            activity.lifecycleScope.launch {
                val selected = owner.selection()?.takeIf { !it.text.highlight.isNullOrBlank() }
                if (selected != null) {
                    if (value != null) {
                        owner.action(value, selected)
                    } else if (target != null && !processors.launch(target, selected.text.highlight.orEmpty())) {
                        return@launch
                    }
                }
                owner.clear()
                mode.finish()
            }
            return true
        }

        override fun onDestroyActionMode(mode: ActionMode) {
            // Toolbar recreation during handle movement is not selection dismissal.
            feedback.toolbarStopped(mode)
            delegate?.onDestroyActionMode(mode)
        }
    }

internal fun selectionIntent(
    action: ReaderSelectionAction,
    text: String,
): Intent? =
    when (action) {
        ReaderSelectionAction.SHARE ->
            Intent(
                Intent.ACTION_SEND,
            ).setType("text/plain").putExtra(Intent.EXTRA_TEXT, text)
        ReaderSelectionAction.DEFINE ->
            Intent(Intent.ACTION_PROCESS_TEXT)
                .setType("text/plain")
                .putExtra(Intent.EXTRA_PROCESS_TEXT, text)
                .putExtra(Intent.EXTRA_PROCESS_TEXT_READONLY, true)
        else -> null
    }

internal fun FragmentActivity.externalSelectionAction(
    action: ReaderSelectionAction,
    text: String,
    notice: (String) -> Unit,
): Boolean =
    if (action == ReaderSelectionAction.COPY) {
        getSystemService(ClipboardManager::class.java).setPrimaryClip(ClipData.newPlainText("Book text", text))
        false
    } else {
        val chooser = selectionChooser(action, text)
        if (chooser == null) {
            notice("No dictionary app is installed. Install an app that supports Define / Process text.")
            false
        } else {
            try {
                startActivity(chooser)
                true
            } catch (_: ActivityNotFoundException) {
                notice("No app is available for this action.")
                false
            } catch (_: SecurityException) {
                notice("This app could not open the selected text.")
                false
            }
        }
    }

private fun FragmentActivity.selectionChooser(
    action: ReaderSelectionAction,
    text: String,
): Intent? {
    val intent = selectionIntent(action, text) ?: return null
    return if (action == ReaderSelectionAction.DEFINE) {
        val targets =
            packageManager
                .queryIntentActivities(intent, 0)
                .filter { activity ->
                    activity.activityInfo.exported && activity.activityInfo.enabled &&
                        (
                            activity.activityInfo.permission == null ||
                                checkSelfPermission(activity.activityInfo.permission) ==
                                PackageManager.PERMISSION_GRANTED
                        )
                }.map {
                    val component = ComponentName(it.activityInfo.packageName, it.activityInfo.name)
                    Intent(intent).setComponent(component)
                }
        targets.firstOrNull()?.let {
            Intent
                .createChooser(it, "Define")
                .putExtra(Intent.EXTRA_INITIAL_INTENTS, targets.drop(1).toTypedArray())
        }
    } else {
        Intent.createChooser(intent, "Share text")
    }
}

private const val SELECTION_ITEM_BASE = 7200
