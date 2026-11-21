package com.kmab.deft.feature.library

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.kmab.deft.core.database.model.BookListItem
import com.kmab.deft.core.designsystem.DeftReadingState
import com.kmab.deft.core.designsystem.DeftTypography
import com.kmab.deft.core.designsystem.LocalDeftColors
import com.kmab.deft.core.designsystem.StateBar
import com.kmab.deft.core.designsystem.icons.DeftIcons
import com.kmab.deft.core.model.BookState
import com.kmab.deft.core.model.BookTrackingSummary

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun LibraryReadingStatus(
    book: BookListItem,
    tracking: BookTrackingSummary?,
) {
    val state = (tracking?.state ?: BookState.NOT_STARTED).appearance()
    val c = LocalDeftColors.current
    Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
        if (state != DeftReadingState.NotRead) {
            StateBar(
                state,
                if (state == DeftReadingState.Finished) 1f else book.totalProgression.toFloat(),
            )
        }
        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(
                state.label,
                style = DeftTypography.cardTitle,
                color =
                    when (state) {
                        DeftReadingState.Reading -> c.accentText
                        DeftReadingState.Finished -> c.finishedText
                        DeftReadingState.Paused -> c.pausedText
                        else -> c.muted
                    },
            )
            if (tracking?.assessment?.favorite ==
                true
            ) {
                Icon(DeftIcons.Heart, "favorite", Modifier.size(14.dp), tint = c.accent)
            }
            tracking?.assessment?.rating?.let { Text("$it ★", style = DeftTypography.mono, color = c.muted) }
        }
    }
}
