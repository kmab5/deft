package com.kmab.deft.core.designsystem

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.Spring
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

object DeftShapes {
    val cover = RoundedCornerShape(2.dp)
    val small = RoundedCornerShape(4.dp)
    val card = RoundedCornerShape(8.dp)
    val sheet = RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp)
    val material = Shapes(extraSmall = small, small = small, medium = card, large = card, extraLarge = card)
}

object DeftSpace {
    val base = 4.dp
    val gutter = 16.dp
    val gridGap = 12.dp
    val sectionGap = 24.dp
    val touchTarget = 48.dp
}

// Bezier coordinates are the approved motion tokens.
@Suppress("MagicNumber")
object DeftMotion {
    const val SHORT = 150
    const val MEDIUM = 250
    const val LONG = 400
    val enter = CubicBezierEasing(0.05f, 0.7f, 0.1f, 1f)
    val exit = CubicBezierEasing(0.3f, 0f, 0.8f, 0.15f)
    val standard = CubicBezierEasing(0.2f, 0f, 0f, 1f)
    const val DAMPING_RATIO = 1f
    val stiffness = Spring.StiffnessMediumLow
}
