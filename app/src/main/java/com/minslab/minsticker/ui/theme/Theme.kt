package com.minslab.minsticker.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

val MinstickerDarkColorScheme = darkColorScheme(
    primary = M3Primary,
    onPrimary = M3OnPrimary,
    primaryContainer = M3PrimaryContainer,
    onPrimaryContainer = M3OnPrimaryContainer,
    secondary = M3Secondary,
    onSecondary = M3OnSecondary,
    secondaryContainer = M3SecondaryContainer,
    onSecondaryContainer = M3OnSecondaryContainer,
    tertiary = M3Tertiary,
    onTertiary = M3OnTertiary,
    tertiaryContainer = M3TertiaryContainer,
    onTertiaryContainer = M3OnTertiaryContainer,
    background = M3Background,
    onBackground = M3OnBackground,
    surface = M3Surface,
    onSurface = M3OnSurface,
    surfaceVariant = M3SurfaceVariant,
    onSurfaceVariant = M3OnSurfaceVariant,
    surfaceContainerLowest = M3SurfaceContainerLowest,
    surfaceContainerLow = M3SurfaceContainerLow,
    surfaceContainer = M3SurfaceContainer,
    surfaceContainerHigh = M3SurfaceContainerHigh,
    surfaceContainerHighest = M3SurfaceContainerHighest,
    surfaceBright = M3SurfaceBright,
    surfaceDim = M3SurfaceDim,
    outline = M3Outline,
    outlineVariant = M3OutlineVariant,
    error = M3Error,
    onError = M3OnError,
    errorContainer = M3ErrorContainer,
    onErrorContainer = M3OnErrorContainer
)

@Composable
fun MinstickerTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = MinstickerDarkColorScheme,
        content = content
    )
}
