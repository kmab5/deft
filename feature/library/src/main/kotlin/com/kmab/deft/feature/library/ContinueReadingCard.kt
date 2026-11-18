package com.kmab.deft.feature.library

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.ImageLoader
import com.kmab.deft.core.database.model.BookListItem
import com.kmab.deft.core.designsystem.DeftShapes
import com.kmab.deft.core.designsystem.DeftTypography
import com.kmab.deft.core.designsystem.LocalDeftColors
import com.kmab.deft.core.designsystem.icons.DeftActionIcons
import kotlin.math.roundToInt

@Composable
internal fun ContinueReadingCard(
    book: BookListItem,
    loader: ImageLoader,
    onClick: () -> Unit,
) {
    val colors = LocalDeftColors.current
    val progress by animateFloatAsState(book.totalProgression.coerceIn(0.0, 1.0).toFloat(), label = "saved progress")
    Row(
        Modifier
            .fillMaxWidth()
            .testTag("continue_reading")
            .padding(horizontal = 20.dp, vertical = 8.dp)
            .clip(
                DeftShapes.card,
            ).background(colors.surface)
            .clickable(role = Role.Button, onClick = onClick)
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        LibraryCover(book, loader, Modifier.width(48.dp), showBadges = false)
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text("continue reading", style = DeftTypography.mono, color = colors.accentText)
            Text(
                book.title,
                style = DeftTypography.label,
                color = colors.text,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier.fillMaxWidth(),
                color = colors.accent,
                trackColor = colors.surface2,
                gapSize = 0.dp,
                drawStopIndicator = {},
            )
            Text(
                "${(book.totalProgression * PERCENT_SCALE).roundToInt()}%",
                style = DeftTypography.mono,
                color = colors.muted,
            )
        }
        Icon(DeftActionIcons.ChevronRight, null, tint = colors.muted)
    }
}

private const val PERCENT_SCALE = 100
