package com.moztro.app.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val MonochromeDarkScheme = darkColorScheme(
    primary = MonoWhite,
    onPrimary = MonoBlack,
    secondary = MonoAccentDim,
    onSecondary = MonoBlack,
    background = MonoDarkBg,
    onBackground = MonoTextPrimary,
    surface = MonoSurface,
    onSurface = MonoTextPrimary,
    surfaceVariant = MonoSurfaceHover,
    onSurfaceVariant = MonoTextSecondary,
    outline = MonoBorder
)

@Composable
fun MoztroTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = MonochromeDarkScheme,
        content = content
    )
}
