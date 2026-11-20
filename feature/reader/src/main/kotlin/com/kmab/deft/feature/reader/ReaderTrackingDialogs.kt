package com.kmab.deft.feature.reader

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import com.kmab.deft.core.model.BookState
import com.kmab.deft.core.model.ReaderOpenChoice

@Composable
internal fun ReaderTrackingDialogs(
    opening: BookState?,
    finishPrompt: Boolean,
    busy: Boolean,
    choose: (ReaderOpenChoice) -> Unit,
    close: () -> Unit,
    finish: () -> Unit,
    decline: () -> Unit,
) {
    if (opening != null) {
        val finished = opening == BookState.FINISHED
        AlertDialog(
            onDismissRequest = { if (!busy) close() },
            title = { Text(if (finished) "You've finished this." else "Pick up where you left off?") },
            text = {
                Text(
                    if (finished) {
                        "Re-reading starts a new read-through. Browsing doesn't count toward stats."
                    } else {
                        "Resume this read-through, or start a new one from the beginning."
                    },
                )
            },
            confirmButton = {
                TextButton(
                    onClick = { choose(if (finished) ReaderOpenChoice.RE_READ else ReaderOpenChoice.RESUME) },
                    enabled = !busy,
                ) {
                    Text(if (finished) "Re-read" else "Resume")
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    choose(if (finished) ReaderOpenChoice.BROWSING else ReaderOpenChoice.START_OVER)
                }, enabled = !busy) {
                    Text(if (finished) "Just browsing" else "Start over")
                }
            },
        )
    } else if (finishPrompt) {
        AlertDialog(
            onDismissRequest = { if (!busy) decline() },
            title = { Text("Mark as finished?") },
            text = { Text("You've reached the end. You can rate this book after marking it finished.") },
            confirmButton = { TextButton(onClick = finish, enabled = !busy) { Text("Yes") } },
            dismissButton = { TextButton(onClick = decline, enabled = !busy) { Text("No") } },
        )
    }
}
