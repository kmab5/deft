package com.kmab.deft.feature.celebrate

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.Icon
import androidx.compose.material3.IconToggleButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import com.kmab.deft.core.designsystem.DeftMotion
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
    Column {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text("your rating", Modifier.weight(1f), style = DeftTypography.label, color = c.text)
            if (rating != null) {
                TextButton(
                    onClick = { onRating(null) },
                    modifier = Modifier.semantics { contentDescription = "clear rating" },
                    enabled = enabled,
                ) { Text("clear", style = DeftTypography.label) }
            }
            FavoriteHeart(favorite, onFavorite, enabled)
        }
        RatingBar(
            (rating ?: 0.0).toFloat(),
            { onRating(it.toDouble().takeIf { value -> value > 0 }) },
            Modifier.horizontalScroll(rememberScrollState()),
            enabled = enabled,
        )
    }
}

private const val HEART_PEAK_SCALE = 1.18f

@Composable
private fun FavoriteHeart(
    favorite: Boolean,
    onFavorite: (Boolean) -> Unit,
    enabled: Boolean,
) {
    val c = LocalDeftColors.current
    val scale = remember { Animatable(1f) }
    var previous by remember { mutableStateOf(favorite) }
    LaunchedEffect(favorite) {
        if (previous != favorite) {
            previous = favorite
            scale.animateTo(HEART_PEAK_SCALE, tween(DeftMotion.SHORT / 2))
            scale.animateTo(1f, tween(DeftMotion.SHORT, easing = DeftMotion.standard))
        }
    }
    IconToggleButton(favorite, onFavorite, enabled = enabled) {
        Icon(
            if (favorite) DeftIcons.HeartFilled else DeftIcons.Heart,
            "favorite",
            Modifier.graphicsLayer {
                scaleX = scale.value
                scaleY = scale.value
            },
            tint = if (favorite) c.accentText else c.muted,
        )
    }
}
