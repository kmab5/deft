package com.kmab.deft.feature.settings

import android.content.Context
import android.text.format.Formatter
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.kmab.deft.core.designsystem.DeftTypography
import com.kmab.deft.core.designsystem.LocalDeftColors
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

internal data class LibraryStorage(
    val database: Long,
    val covers: Long,
    val cache: Long,
) {
    companion object {
        fun read(context: Context): LibraryStorage =
            LibraryStorage(
                database = context.getDatabasePath("deft.db").parentFile?.let(::directoryBytes) ?: 0,
                covers = directoryBytes(File(context.filesDir, "covers")),
                cache = directoryBytes(context.cacheDir),
            )
    }
}

private fun directoryBytes(directory: File): Long =
    if (directory.exists()) directory.walkTopDown().filter { it.isFile }.sumOf { it.length() } else 0

@Composable
internal fun LibraryStorageSection(refreshKey: Any?) {
    val context = LocalContext.current.applicationContext
    val colors = LocalDeftColors.current
    val storage by produceState<LibraryStorage?>(null, refreshKey) {
        value = withContext(Dispatchers.IO) { LibraryStorage.read(context) }
    }
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("storage on this device", style = DeftTypography.title)
        storage?.let { sizes ->
            listOf("databases" to sizes.database, "cover thumbnails" to sizes.covers, "temporary cache" to sizes.cache)
                .forEach { (label, size) ->
                    Row(Modifier.fillMaxWidth()) {
                        Text(label, Modifier.weight(1f), style = DeftTypography.body, color = colors.muted)
                        Text(Formatter.formatShortFileSize(context, size), style = DeftTypography.label)
                    }
                }
        }
        Text(
            "Book files stay in your folders. Deft stores their details and small cover images here.",
            Modifier.padding(top = 4.dp),
            style = DeftTypography.body,
            color = colors.muted,
        )
    }
}
