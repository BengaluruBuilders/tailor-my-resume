package com.tailormyresume.core.designsystem.theme

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ProvidableCompositionLocal
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

@Immutable
class TmrColors(
    val header: Color,
    val headerShape: Color,
    val onHeader: Color,
    val onHeaderVariant: Color,
    val onHeaderControl: Color,
    val headerControl: Color,
    val primary: Color,
    val onPrimary: Color,
    val primaryContainer: Color,
    val onPrimaryContainer: Color,
    val background: Color,
    val surface: Color,
    val sheet: Color,
    val card: Color,
    val ground: Color,
    val document: Color,
    val tool: Color,
    val onTool: Color,
    val onToolVariant: Color,
    val onToolSelected: Color,
    val outline: Color,
    val outlineVariant: Color,
    val outlineSoft: Color,
    val boundary: Color,
    val disabledContent: Color,
    val onSurface: Color,
    val onSurfaceVariant: Color,
    val body: Color,
    val inverseSurface: Color,
    val inverseOnSurface: Color,
    val inversePrimary: Color,
    val met: Color,
    val metContainer: Color,
    val onMetContainer: Color,
    val partial: Color,
    val partialContainer: Color,
    val onPartialContainer: Color,
    val gap: Color,
    val gapContainer: Color,
    val onGapContainer: Color,
    val evidence: Color,
    val evidenceLine: Color,
    val error: Color,
    val onError: Color,
    val errorContainer: Color,
    val onErrorContainer: Color,
    val brand: Color,
    val brandPressed: Color,
    val sheetItemBorder: Color,
    val onBrand: Color,
    val coral: Color,
    val onCoral: Color,
    val special: Color,
    val onSpecial: Color,
    val scrim: Color,
    val logoTiles: List<Color>,
    val onLogoTile: Color,
) {
    val neutralContainer: Color get() = gapContainer
    val onNeutralContainer: Color get() = onGapContainer
}

val LocalTmrColors: ProvidableCompositionLocal<TmrColors> =
    staticCompositionLocalOf { TmrDarkColors }
