package com.kmab.deft.feature.organization

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.kmab.deft.core.designsystem.DeftTypography
import com.kmab.deft.core.designsystem.LocalDeftColors
import com.kmab.deft.core.designsystem.RatingBar
import com.kmab.deft.core.designsystem.icons.DeftActionIcons
import com.kmab.deft.library.organization.CategorySummary

@Composable
internal fun CategoryRatingSummary(
    category: CategorySummary,
    open: () -> Unit,
    modifier: Modifier,
) {
    val colors = LocalDeftColors.current
    val source = if (category.category.ratingOverride == null) "average" else "your rating"
    Row(
        modifier
            .heightIn(
                min = 48.dp,
            ).clickable(role = Role.Button, onClickLabel = "edit category rating", onClick = open),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text("rating", style = DeftTypography.label)
            Text("$source · ${ratingLabel(category.rating)}", style = DeftTypography.mono, color = colors.accentText)
        }
        Icon(DeftActionIcons.ChevronRight, null, Modifier.size(20.dp), tint = colors.muted)
    }
}

@Composable
internal fun CategoryRatingSheet(
    category: CategorySummary,
    model: OrganizationViewModel,
    close: () -> Unit,
) {
    val override = category.category.ratingOverride
    val average = category.stats.averageRating
    var draft by rememberSaveable(category.category.id) { mutableFloatStateOf(override?.toFloat() ?: 0f) }
    val busy by model.busy.collectAsStateWithLifecycle()
    OrganizationSheet("category rating", close, model, footer = {
        Row(Modifier.fillMaxWidth().height(IntrinsicSize.Min), Arrangement.spacedBy(8.dp)) {
            OrganizationButton("cancel", close, Modifier.weight(1f).fillMaxHeight(), enabled = !busy)
            OrganizationButton(
                "save rating",
                { model.edit(close) { setRating(category.category.id, draft.toDouble()) } },
                Modifier.weight(1f).fillMaxHeight(),
                primary = true,
                enabled = !busy && draft > 0f,
            )
        }
    }) {
        Text(
            "book average · ${ratingLabel(average)}",
            style = DeftTypography.mono,
            color = LocalDeftColors.current.muted,
        )
        Column {
            Text("your rating", style = DeftTypography.label)
            RatingBar(draft, { draft = it }, modifier = Modifier.fillMaxWidth(), enabled = !busy)
        }
        if (override != null) {
            OrganizationButton(
                "use book average",
                { model.edit(close) { setRating(category.category.id, null) } },
                Modifier.fillMaxWidth(),
                icon = DeftActionIcons.Return,
                enabled = !busy,
            )
        }
    }
}
