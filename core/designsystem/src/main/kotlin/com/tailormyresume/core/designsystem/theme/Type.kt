package com.tailormyresume.core.designsystem.theme

import androidx.compose.material3.Typography
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ProvidableCompositionLocal
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp

object TmrFontFamilies {
    val sans = FontFamily.Default
    val display = FontFamily.Default
}

@Immutable
class TmrTypography(
    val displayL: TextStyle,
    val displayM: TextStyle,
    val headlineL: TextStyle,
    val headlineM: TextStyle,
    val titleL: TextStyle,
    val titleM: TextStyle,
    val titleS: TextStyle,
    val bodyL: TextStyle,
    val bodyM: TextStyle,
    val labelL: TextStyle,
    val labelM: TextStyle,
    val button: TextStyle,
    val bodyS: TextStyle,
    val numeralHero: TextStyle,
    val numeralM: TextStyle,
    val factId: TextStyle,
)

internal object TmrTypographyTokens {
    private val lineHeightStyle = LineHeightStyle(
        alignment = LineHeightStyle.Alignment.Center,
        trim = LineHeightStyle.Trim.None,
    )

    private fun sans(
        size: Int,
        line: Int,
        weight: FontWeight,
        tracking: TextUnit,
    ) = TextStyle(
        fontFamily = TmrFontFamilies.sans,
        fontWeight = weight,
        fontSize = size.sp,
        lineHeight = line.sp,
        letterSpacing = tracking,
        fontFeatureSettings = TABULAR_FIGURES,
        lineHeightStyle = lineHeightStyle,
    )

    private fun display(size: Int, line: Int) = TextStyle(
        fontFamily = TmrFontFamilies.display,
        fontWeight = FontWeight.Black,
        fontSize = size.sp,
        lineHeight = line.sp,
        letterSpacing = 0.em,
        lineHeightStyle = lineHeightStyle,
    )

    val Default = TmrTypography(
        displayL = display(34, 36),
        displayM = display(30, 32),
        headlineL = display(28, 30),
        headlineM = sans(22, 28, FontWeight.ExtraBold, (-0.01).em),
        titleL = sans(18, 24, FontWeight.ExtraBold, 0.em),
        titleM = sans(16, 22, FontWeight.ExtraBold, 0.em),
        titleS = sans(15, 21, FontWeight.Bold, 0.em),
        bodyL = sans(16, 23, FontWeight.SemiBold, 0.em),
        bodyM = sans(15, 22, FontWeight.SemiBold, 0.em),
        labelL = sans(14, 20, FontWeight.ExtraBold, 0.em),
        labelM = sans(13, 18, FontWeight.Bold, 0.em),
        button = sans(16, 22, FontWeight.ExtraBold, 0.em),
        bodyS = sans(13, 18, FontWeight.SemiBold, 0.em),
        numeralHero = display(52, 52),
        numeralM = sans(22, 28, FontWeight.ExtraBold, 0.em),
        factId = sans(12, 16, FontWeight.ExtraBold, 0.05.em),
    )
}

private const val TABULAR_FIGURES = "tnum"

val LocalTmrTypography: ProvidableCompositionLocal<TmrTypography> =
    staticCompositionLocalOf { TmrTypographyTokens.Default }

internal fun TmrTypography.toMaterial(): Typography = Typography(
    displayLarge = displayL,
    displayMedium = displayM,
    displaySmall = headlineL,
    headlineLarge = headlineL,
    headlineMedium = headlineM,
    headlineSmall = titleL,
    titleLarge = titleL,
    titleMedium = titleM,
    titleSmall = titleS,
    bodyLarge = bodyL,
    bodyMedium = bodyM,
    bodySmall = bodyS,
    labelLarge = labelL,
    labelMedium = labelM,
    labelSmall = labelM,
)
