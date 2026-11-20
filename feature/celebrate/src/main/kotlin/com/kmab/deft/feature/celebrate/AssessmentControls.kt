package com.kmab.deft.feature.celebrate

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.Icon
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.kmab.deft.core.designsystem.DeftButton
import com.kmab.deft.core.designsystem.DeftButtonStyle
import com.kmab.deft.core.designsystem.DeftTypography
import com.kmab.deft.core.designsystem.LocalDeftColors
import com.kmab.deft.core.designsystem.RatingBar
import com.kmab.deft.core.designsystem.icons.DeftIcons

@Composable
fun AssessmentControls(
    rating: Double?,
    favorite: Boolean,
    onRating: (Double?) -> Unit,
    onFavorite: (Boolean) -> Unit,
    enabled: Boolean = true,
) {
    val c = LocalDeftColors.current
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        RatingBar(
            (rating ?: 0.0).toFloat(),
            { if (enabled) onRating(it.toDouble().takeIf { value -> value > 0 }) },
            Modifier.horizontalScroll(rememberScrollState()),
        )
        if (rating !=
            null
        ) {
            DeftButton("clear rating", { onRating(null) }, style = DeftButtonStyle.Text, enabled = enabled)
        }
        Row(
            Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Icon(DeftIcons.Heart, null, tint = if (favorite) c.accent else c.muted)
            Text("favorite", Modifier.weight(1f), style = DeftTypography.label)
            Switch(
                favorite,
                onFavorite,
                enabled = enabled,
                modifier =
                    Modifier.semantics {
                        contentDescription =
                            "favorite"
                    },
            )
        }
    }
}
