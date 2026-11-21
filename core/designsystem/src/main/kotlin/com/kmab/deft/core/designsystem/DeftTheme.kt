package com.kmab.deft.core.designsystem

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

val LocalDeftColors = staticCompositionLocalOf { DeftColors.Ink }

@Composable
fun DeftTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val c = (if (darkTheme) DeftColors.Ink else DeftColors.Paper).animated()
    val scheme = if (darkTheme) darkColorScheme() else lightColorScheme()
    CompositionLocalProvider(LocalDeftColors provides c) {
        MaterialTheme(
            colorScheme =
                scheme.copy(
                    primary = c.accent,
                    onPrimary = c.onAccent,
                    background = c.bg,
                    onBackground = c.text,
                    surface = c.surface,
                    onSurface = c.text,
                    surfaceVariant = c.surface2,
                    onSurfaceVariant = c.muted,
                    outline = c.line2,
                    error = c.error,
                    surfaceTint = c.surface,
                ),
            typography = DeftTypography.material,
            shapes = DeftShapes.material,
            content = content,
        )
    }
}

@Composable
private fun DeftColors.animated(): DeftColors =
    copy(
        bg = paletteColor(bg),
        surface = paletteColor(surface),
        surface2 = paletteColor(surface2),
        line = paletteColor(line),
        line2 = paletteColor(line2),
        text = paletteColor(text),
        muted = paletteColor(muted),
        dim = paletteColor(dim),
        accent = paletteColor(accent),
        accentText = paletteColor(accentText),
        onAccent = paletteColor(onAccent),
        finished = paletteColor(finished),
        finishedText = paletteColor(finishedText),
        paused = paletteColor(paused),
        pausedText = paletteColor(pausedText),
        abandoned = paletteColor(abandoned),
        error = paletteColor(error),
        scrim = paletteColor(scrim),
    )

@Composable
private fun paletteColor(value: Color): Color =
    animateColorAsState(value, tween(DeftMotion.MEDIUM), label = "app palette").value
