package com.kmab.deft.feature.reader

import android.content.ActivityNotFoundException
import android.content.ComponentName
import android.content.Intent
import android.content.pm.PackageManager
import android.view.Menu
import android.view.MenuItem
import androidx.fragment.app.FragmentActivity

/** Android's installed text actions, resolved locally and launched only after an explicit menu tap. */
internal class ReaderTextProcessors(
    private val activity: FragmentActivity,
    private val notice: (String) -> Unit,
) {
    private val targets = mutableMapOf<Int, Intent>()

    fun populate(menu: Menu) {
        targets.clear()
        val query = Intent(Intent.ACTION_PROCESS_TEXT).setType("text/plain")
        activity.packageManager
            .queryIntentActivities(query, 0)
            .filter { result ->
                val info = result.activityInfo
                info.exported && info.enabled &&
                    (
                        info.permission == null ||
                            activity.checkSelfPermission(info.permission) == PackageManager.PERMISSION_GRANTED
                    )
            }.distinctBy { ComponentName(it.activityInfo.packageName, it.activityInfo.name) }
            .forEachIndexed { index, result ->
                val id = PROCESS_ITEM_BASE + index
                targets[id] =
                    Intent(query).setComponent(ComponentName(result.activityInfo.packageName, result.activityInfo.name))
                menu
                    .add(Menu.NONE, id, PROCESS_ITEM_BASE + index, result.loadLabel(activity.packageManager))
                    .setShowAsAction(MenuItem.SHOW_AS_ACTION_NEVER)
            }
    }

    fun target(item: MenuItem): Intent? = targets[item.itemId]?.let(::Intent)

    fun launch(
        target: Intent,
        text: String,
    ): Boolean =
        try {
            activity.startActivity(
                Intent(target)
                    .putExtra(Intent.EXTRA_PROCESS_TEXT, text)
                    .putExtra(Intent.EXTRA_PROCESS_TEXT_READONLY, true),
            )
            true
        } catch (_: ActivityNotFoundException) {
            notice("No app is available for this action.")
            false
        } catch (_: SecurityException) {
            notice("This app could not open the selected text.")
            false
        }
}

private const val PROCESS_ITEM_BASE = 7300
