package com.minslab.minsticker.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import com.minslab.minsticker.data.ThemeMode

val GoogleLightColorScheme = lightColorScheme(
    primary = GoogleLightPrimary,
    onPrimary = GoogleLightOnPrimary,
    primaryContainer = GoogleLightPrimaryContainer,
    onPrimaryContainer = GoogleLightOnPrimaryContainer,
    secondary = GoogleLightSecondary,
    onSecondary = GoogleLightOnSecondary,
    secondaryContainer = GoogleLightSecondaryContainer,
    onSecondaryContainer = GoogleLightOnSecondaryContainer,
    tertiary = GoogleLightTertiary,
    onTertiary = GoogleLightOnTertiary,
    tertiaryContainer = GoogleLightTertiaryContainer,
    onTertiaryContainer = GoogleLightOnTertiaryContainer,
    background = GoogleLightBackground,
    onBackground = GoogleLightOnBackground,
    surface = GoogleLightSurface,
    onSurface = GoogleLightOnSurface,
    surfaceVariant = GoogleLightSurfaceVariant,
    onSurfaceVariant = GoogleLightOnSurfaceVariant,
    surfaceContainerLowest = GoogleLightSurfaceContainerLowest,
    surfaceContainerLow = GoogleLightSurfaceContainerLow,
    surfaceContainer = GoogleLightSurfaceContainer,
    surfaceContainerHigh = GoogleLightSurfaceContainerHigh,
    surfaceContainerHighest = GoogleLightSurfaceContainerHighest,
    outline = GoogleLightOutline,
    outlineVariant = GoogleLightOutlineVariant,
    error = GoogleLightError,
    onError = GoogleLightOnError,
    errorContainer = GoogleLightErrorContainer,
    onErrorContainer = GoogleLightOnErrorContainer
)

val GoogleDarkColorScheme = darkColorScheme(
    primary = GoogleDarkPrimary,
    onPrimary = GoogleDarkOnPrimary,
    primaryContainer = GoogleDarkPrimaryContainer,
    onPrimaryContainer = GoogleDarkOnPrimaryContainer,
    secondary = GoogleDarkSecondary,
    onSecondary = GoogleDarkOnSecondary,
    secondaryContainer = GoogleDarkSecondaryContainer,
    onSecondaryContainer = GoogleDarkOnSecondaryContainer,
    tertiary = GoogleDarkTertiary,
    onTertiary = GoogleDarkOnTertiary,
    tertiaryContainer = GoogleDarkTertiaryContainer,
    onTertiaryContainer = GoogleDarkOnTertiaryContainer,
    background = GoogleDarkBackground,
    onBackground = GoogleDarkOnBackground,
    surface = GoogleDarkSurface,
    onSurface = GoogleDarkOnSurface,
    surfaceVariant = GoogleDarkSurfaceVariant,
    onSurfaceVariant = GoogleDarkOnSurfaceVariant,
    surfaceContainerLowest = GoogleDarkSurfaceContainerLowest,
    surfaceContainerLow = GoogleDarkSurfaceContainerLow,
    surfaceContainer = GoogleDarkSurfaceContainer,
    surfaceContainerHigh = GoogleDarkSurfaceContainerHigh,
    surfaceContainerHighest = GoogleDarkSurfaceContainerHighest,
    outline = GoogleDarkOutline,
    outlineVariant = GoogleDarkOutlineVariant,
    error = GoogleDarkError,
    onError = GoogleDarkOnError,
    errorContainer = GoogleDarkErrorContainer,
    onErrorContainer = GoogleDarkOnErrorContainer
)

// Backward compatibility aliases for static references
val M3Primary get() = GoogleDarkPrimary
val M3OnPrimary get() = GoogleDarkOnPrimary
val M3PrimaryContainer get() = GoogleDarkPrimaryContainer
val M3OnPrimaryContainer get() = GoogleDarkOnPrimaryContainer
val M3Secondary get() = GoogleDarkSecondary
val M3OnSecondary get() = GoogleDarkOnSecondary
val M3SecondaryContainer get() = GoogleDarkSecondaryContainer
val M3OnSecondaryContainer get() = GoogleDarkOnSecondaryContainer
val M3Tertiary get() = GoogleDarkTertiary
val M3Background get() = GoogleDarkBackground
val M3OnBackground get() = GoogleDarkOnBackground
val M3Surface get() = GoogleDarkSurface
val M3OnSurface get() = GoogleDarkOnSurface
val M3SurfaceVariant get() = GoogleDarkSurfaceVariant
val M3OnSurfaceVariant get() = GoogleDarkOnSurfaceVariant
val M3SurfaceContainerLowest get() = GoogleDarkSurfaceContainerLowest
val M3SurfaceContainerLow get() = GoogleDarkSurfaceContainerLow
val M3SurfaceContainer get() = GoogleDarkSurfaceContainer
val M3SurfaceContainerHigh get() = GoogleDarkSurfaceContainerHigh
val M3SurfaceContainerHighest get() = GoogleDarkSurfaceContainerHighest
val M3Outline get() = GoogleDarkOutline
val M3OutlineVariant get() = GoogleDarkOutlineVariant
val M3Error get() = GoogleDarkError
val M3OnError get() = GoogleDarkOnError
val M3ErrorContainer get() = GoogleDarkErrorContainer
val M3OnErrorContainer get() = GoogleDarkOnErrorContainer

@Composable
fun MinstickerTheme(
    themeMode: ThemeMode = ThemeMode.SYSTEM,
    darkTheme: Boolean = when (themeMode) {
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
    },
    content: @Composable () -> Unit
) {
    val colorScheme: ColorScheme = if (darkTheme) {
        GoogleDarkColorScheme
    } else {
        GoogleLightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        content = content
    )
}
