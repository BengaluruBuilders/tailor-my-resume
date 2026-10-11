package com.tailormyresume.core.designsystem.theme

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Immutable
data class TmrSpacing(
    val xxs: Dp,
    val xs: Dp,
    val sm: Dp,
    val md: Dp,
    val lg: Dp,
    val xl: Dp,
    val xxl: Dp,
    val xxxl: Dp,
    val d2: Dp,
    val d4: Dp,
    val d8: Dp,
    val d12: Dp,
    val d16: Dp,
    val d20: Dp,
    val d24: Dp,
    val d32: Dp,
    val d40: Dp,
    val d48: Dp,
    val d64: Dp,
    val gutter: Dp,
    val cardPadding: Dp,
    val sectionGap: Dp,
    val touch: Dp,
    val topBarButton: Dp,
    val primaryButtonHeight: Dp,
    val tabItem: Dp,
    val tabCentreDisc: Dp,
    val toastTop: Dp,
    val sheetPaddingTop: Dp,
    val sheetPaddingHorizontal: Dp,
    val sheetPaddingBottom: Dp,
    val sheetHandleWidth: Dp,
    val sheetHandleHeight: Dp,
)

val LocalTmrSpacing = staticCompositionLocalOf { TmrSpacingDefaults.Default }

internal object TmrSpacingDefaults {
    val Default = TmrSpacing(
        xxs = 2.dp,
        xs = 4.dp,
        sm = 8.dp,
        md = 12.dp,
        lg = 16.dp,
        xl = 20.dp,
        xxl = 24.dp,
        xxxl = 32.dp,
        d2 = 2.dp,
        d4 = 4.dp,
        d8 = 8.dp,
        d12 = 12.dp,
        d16 = 16.dp,
        d20 = 20.dp,
        d24 = 24.dp,
        d32 = 32.dp,
        d40 = 40.dp,
        d48 = 48.dp,
        d64 = 64.dp,
        gutter = 14.dp,
        cardPadding = 16.dp,
        sectionGap = 24.dp,
        touch = 48.dp,
        topBarButton = 44.dp,
        primaryButtonHeight = 56.dp,
        tabItem = 54.dp,
        tabCentreDisc = 56.dp,
        toastTop = 96.dp,
        sheetPaddingTop = 12.dp,
        sheetPaddingHorizontal = 18.dp,
        sheetPaddingBottom = 40.dp,
        sheetHandleWidth = 44.dp,
        sheetHandleHeight = 4.dp,
    )
}
