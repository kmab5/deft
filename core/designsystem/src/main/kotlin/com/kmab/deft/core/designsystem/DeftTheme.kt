package com.kmab.deft.core.designsystem

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf

val LocalDeftColors = staticCompositionLocalOf { DeftColors.Ink }

@Composable
fun DeftTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val c = if (darkTheme) DeftColors.Ink else DeftColors.Paper
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
