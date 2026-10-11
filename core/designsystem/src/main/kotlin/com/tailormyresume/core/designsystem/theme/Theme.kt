package com.tailormyresume.core.designsystem.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.unit.dp
import com.tailormyresume.core.designsystem.component.tmrReducedMotion

object TmrTheme {
    val colors: TmrColors
        @Composable
        @ReadOnlyComposable
        get() = LocalTmrColors.current

    val typography: TmrTypography
        @Composable
        @ReadOnlyComposable
        get() = LocalTmrTypography.current

    val spacing: TmrSpacing
        @Composable
        @ReadOnlyComposable
        get() = LocalTmrSpacing.current

    val shapes: TmrShapes
        @Composable
        @ReadOnlyComposable
        get() = LocalTmrShapes.current

    val elevation: TmrElevation
        @Composable
        @ReadOnlyComposable
        get() = LocalTmrElevation.current

    val motion: TmrMotion
        @Composable
        @ReadOnlyComposable
        get() = LocalTmrMotion.current
}

internal fun TmrColors.toDarkScheme(): ColorScheme = darkColorScheme(
    primary = lime,
    onPrimary = ink,
    primaryContainer = surfaceHigh,
    onPrimaryContainer = lime,
    secondary = lime,
    onSecondary = ink,
    secondaryContainer = surfaceHigh,
    onSecondaryContainer = lime,
    tertiary = amber,
    onTertiary = ink,
    error = cheek,
    onError = ink,
    errorContainer = surface,
    onErrorContainer = textSecondary,
    background = background,
    onBackground = text,
    surface = surface,
    onSurface = text,
    surfaceVariant = surface,
    onSurfaceVariant = textMuted,
    surfaceBright = surfaceHigh,
    surfaceContainerLowest = background,
    surfaceContainerLow = surface,
    surfaceContainer = surface,
    surfaceContainerHigh = surfaceRaised,
    surfaceContainerHighest = surfaceRaised,
    inverseSurface = paper,
    inverseOnSurface = ink,
    inversePrimary = ink,
    outline = textDisabled,
    outlineVariant = line,
    scrim = scrim,
)

val DarkColorScheme: ColorScheme = TmrDarkColors.toDarkScheme()

@Composable
fun TmrTheme(
    content: @Composable () -> Unit,
) {
    val motion = if (tmrReducedMotion()) TmrMotionDefaults.Reduced else TmrMotionDefaults.Default
    CompositionLocalProvider(
        LocalTmrColors provides TmrDarkColors,
        LocalTmrTypography provides TmrTypographyTokens.Default,
        LocalTmrSpacing provides TmrSpacingDefaults.Default,
        LocalTmrShapes provides TmrShapesDefaults.Default,
        LocalTmrElevation provides TmrElevationDefaults.Dark,
        LocalTmrMotion provides motion,
        LocalBackgroundTheme provides BackgroundTheme(color = TmrDarkColors.background, tonalElevation = 0.dp),
    ) {
        MaterialTheme(
            colorScheme = DarkColorScheme,
            typography = TmrTypographyTokens.Default.toMaterial(),
            shapes = TmrMaterialShapes,
            content = content,
        )
    }
}
