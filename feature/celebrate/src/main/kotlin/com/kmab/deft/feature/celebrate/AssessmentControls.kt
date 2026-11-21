package com.kmab.deft.feature.celebrate

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconToggleButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import com.kmab.deft.core.designsystem.DeftHaptic
import com.kmab.deft.core.designsystem.DeftMotion
import com.kmab.deft.core.designsystem.DeftTypography
import com.kmab.deft.core.designsystem.LocalDeftColors
import com.kmab.deft.core.designsystem.RatingBar
import com.kmab.deft.core.designsystem.icons.DeftActionIcons
import com.kmab.deft.core.designsystem.icons.DeftIcons
import com.kmab.deft.core.designsystem.performDeftHaptic
import java.util.Locale

@Composable
fun AssessmentControls(
    rating: Double?,
    favorite: Boolean,
    onRating: (Double?) -> Unit,
    onFavorite: (Boolean) -> Unit,
    enabled: Boolean = true,
) {
    val c = LocalDeftColors.current
    val view = LocalView.current
    val chooseRating: (Double?) -> Unit = { value ->
        if (value != rating) {
            view.performDeftHaptic(DeftHaptic.CHOICE)
            onRating(value)
        }
    }
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text("your rating", Modifier.weight(1f), style = DeftTypography.label, color = c.text)
            Text(
                rating?.let { String.format(Locale.ROOT, "%.1f / 5", it) } ?: "not rated",
                style = DeftTypography.mono,
                color = c.muted,
            )
        }
        Row(
            Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            RatingBar(
                (rating ?: 0.0).toFloat(),
                { chooseRating(it.toDouble().takeIf { value -> value > 0 }) },
                Modifier.weight(1f),
                enabled = enabled,
                showValue = false,
            )
            IconButton(onClick = { chooseRating(null) }, enabled = enabled && rating != null) {
                Icon(DeftActionIcons.Close, "clear rating")
            }
            FavoriteHeart(
                favorite,
                { value ->
                    if (value != favorite) {
                        view.performDeftHaptic(DeftHaptic.CHOICE)
                        onFavorite(value)
                    }
                },
                enabled,
            )
        }
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
