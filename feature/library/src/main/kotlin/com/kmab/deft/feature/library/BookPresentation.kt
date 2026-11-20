package com.kmab.deft.feature.library

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.kmab.deft.core.database.model.BookListItem
import com.kmab.deft.core.designsystem.DeftShapes
import com.kmab.deft.core.designsystem.LocalDeftColors

@Composable
internal fun Modifier.libraryBookSurface(
    inset: Dp,
    onClick: (() -> Unit)? = null,
): Modifier {
    val colors = LocalDeftColors.current
    return shadow(1.dp, DeftShapes.card)
        .clip(DeftShapes.card)
        .background(colors.surface)
        .border(1.dp, colors.line, DeftShapes.card)
        .then(if (onClick == null) Modifier else Modifier.clickable(role = Role.Button, onClick = onClick))
        .padding(inset)
}

internal fun BookListItem.displayTitle(): String = title.trim().ifEmpty { "<no-title>" }

internal fun BookListItem.displayAuthors(): String = authorsDisplay.trim().ifEmpty { "<no-author>" }

internal fun BookListItem.firstAuthor(): String = authorsDisplay.substringBefore(", ").trim().ifEmpty { "<no-author>" }
