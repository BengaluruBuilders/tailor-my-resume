package com.tailormyresume.core.designsystem.theme

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ProvidableCompositionLocal
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

@Immutable
class TmrColors(
    val background: Color,
    val surface: Color,
    val surfaceRaised: Color,
    val surfaceHigh: Color,
    val sheet: Color,
    val sheetOption: Color,
    val uploadCard: Color,
    val fill: Color,
    val disabledFill: Color,
    val line: Color,
    val lineStrong: Color,
    val lineHigher: Color,
    val tabDivider: Color,
    val text: Color,
    val textSecondary: Color,
    val textMuted: Color,
    val textDisabled: Color,
    val ink: Color,
    val lime: Color,
    val limeSelected: Color,
    val limeSoft: Color,
    val amber: Color,
    val amberHighlight: Color,
    val blue: Color,
    val cheek: Color,
    val paper: Color,
    val paperFold: Color,
    val segOffer: Color,
    val segRejected: Color,
    val rejectedBorder: Color,
    val scrim: Color,
) {
    val header: Color get() = background
    val headerShape: Color get() = line
    val onHeader: Color get() = text
    val onHeaderVariant: Color get() = textMuted
    val onHeaderControl: Color get() = text
    val headerControl: Color get() = surfaceHigh
    val primary: Color get() = lime
    val onPrimary: Color get() = ink
    val primaryContainer: Color get() = surfaceHigh
    val onPrimaryContainer: Color get() = lime
    val card: Color get() = surface
    val ground: Color get() = background
    val document: Color get() = surfaceHigh
    val tool: Color get() = surface
    val onTool: Color get() = text
    val onToolVariant: Color get() = textSecondary
    val onToolSelected: Color get() = lime
    val outline: Color get() = textDisabled
    val outlineVariant: Color get() = line
    val outlineSoft: Color get() = line
    val boundary: Color get() = textDisabled
    val disabledContent: Color get() = textDisabled
    val onSurface: Color get() = text
    val onSurfaceVariant: Color get() = textMuted
    val body: Color get() = textSecondary
    val inverseSurface: Color get() = paper
    val inverseOnSurface: Color get() = ink
    val inversePrimary: Color get() = ink
    val met: Color get() = lime
    val metContainer: Color get() = limeSelected
    val onMetContainer: Color get() = lime
    val partial: Color get() = amber
    val partialContainer: Color get() = surface
    val onPartialContainer: Color get() = amber
    val gap: Color get() = textSecondary
    val gapContainer: Color get() = surfaceHigh
    val onGapContainer: Color get() = textSecondary
    val evidence: Color get() = surfaceHigh
    val evidenceLine: Color get() = lime
    val error: Color get() = cheek
    val onError: Color get() = ink
    val errorContainer: Color get() = surface
    val onErrorContainer: Color get() = textSecondary
    val brand: Color get() = lime
    val brandPressed: Color get() = lime
    val sheetItemBorder: Color get() = lineHigher
    val onBrand: Color get() = ink
    val coral: Color get() = cheek
    val onCoral: Color get() = ink
    val special: Color get() = lime
    val onSpecial: Color get() = ink
    val onLogoTile: Color get() = ink
    val logoTiles: List<Color> get() = listOf(blue, amber, lime, cheek)
    val neutralContainer: Color get() = gapContainer
    val onNeutralContainer: Color get() = onGapContainer
}

val LocalTmrColors: ProvidableCompositionLocal<TmrColors> =
    staticCompositionLocalOf { TmrDarkColors }
