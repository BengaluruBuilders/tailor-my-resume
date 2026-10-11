package com.tailormyresume.core.designsystem.theme

import androidx.compose.material3.Typography
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ProvidableCompositionLocal
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp
import com.tailormyresume.core.designsystem.R

@OptIn(ExperimentalTextApi::class)
object TmrFontFamilies {
    val mono: FontFamily = FontFamily(
        Font(
            resId = R.font.space_mono_regular,
            weight = FontWeight.Normal,
        ),
        Font(
            resId = R.font.space_mono_bold,
            weight = FontWeight.Bold,
        ),
    )

    val grotesk: FontFamily = FontFamily(
        Font(
            resId = R.font.space_grotesk,
            weight = FontWeight.Normal,
            style = FontStyle.Normal,
            variationSettings = FontVariation.Settings(FontVariation.weight(400)),
        ),
        Font(
            resId = R.font.space_grotesk,
            weight = FontWeight.Medium,
            style = FontStyle.Normal,
            variationSettings = FontVariation.Settings(FontVariation.weight(500)),
        ),
        Font(
            resId = R.font.space_grotesk,
            weight = FontWeight.SemiBold,
            style = FontStyle.Normal,
            variationSettings = FontVariation.Settings(FontVariation.weight(600)),
        ),
        Font(
            resId = R.font.space_grotesk,
            weight = FontWeight.Bold,
            style = FontStyle.Normal,
            variationSettings = FontVariation.Settings(FontVariation.weight(700)),
        ),
    )

    val sans: FontFamily = grotesk

    val display: FontFamily = mono
}

@Immutable
class TmrTypography(
    val headline: TextStyle,
    val headlineSmall: TextStyle,
    val title: TextStyle,
    val display: TextStyle,
    val displayLarge: TextStyle,
    val label: TextStyle,
    val labelWide: TextStyle,
    val button: TextStyle,
    val mono15: TextStyle,
    val mono14: TextStyle,
    val body: TextStyle,
    val bodyLarge: TextStyle,
    val bodySmall: TextStyle,
    val caption: TextStyle,
    val strongLarge: TextStyle,
    val strongSmall: TextStyle,
) {
    val displayL: TextStyle get() = display

    val displayM: TextStyle get() = headline

    val headlineL: TextStyle get() = headline

    val headlineM: TextStyle get() = title

    val titleL: TextStyle get() = title

    val titleM: TextStyle get() = strongLarge

    val titleS: TextStyle get() = strongSmall

    val bodyL: TextStyle get() = bodyLarge

    val bodyM: TextStyle get() = body

    val labelL: TextStyle get() = button

    val labelM: TextStyle get() = label

    val bodyS: TextStyle get() = bodySmall

    val numeralHero: TextStyle get() = displayLarge

    val numeralM: TextStyle get() = display

    val factId: TextStyle get() = label
}

internal object TmrTypographyTokens {
    private val lineHeightStyle = LineHeightStyle(
        alignment = LineHeightStyle.Alignment.Center,
        trim = LineHeightStyle.Trim.None,
    )

    private fun mono(
        weight: FontWeight,
        size: Float,
        lineHeight: Float?,
        tracking: Float,
    ): TextStyle = TextStyle(
        fontFamily = TmrFontFamilies.mono,
        fontWeight = weight,
        fontSize = size.sp,
        lineHeight = lineHeight?.sp ?: TextUnit.Unspecified,
        letterSpacing = tracking.sp,
        lineHeightStyle = lineHeightStyle,
    )

    private fun grotesk(
        weight: FontWeight,
        size: Float,
        lineHeight: Float?,
        tracking: Float,
    ): TextStyle = TextStyle(
        fontFamily = TmrFontFamilies.grotesk,
        fontWeight = weight,
        fontSize = size.sp,
        lineHeight = lineHeight?.sp ?: TextUnit.Unspecified,
        letterSpacing = tracking.sp,
        lineHeightStyle = lineHeightStyle,
    )

    val Default = TmrTypography(
        headline = mono(FontWeight.Normal, 28f, 30.8f, -1.0f),
        headlineSmall = mono(FontWeight.Normal, 26f, 28.6f, -1.0f),
        title = mono(FontWeight.Normal, 22f, 25.3f, -0.6f),
        display = mono(FontWeight.Bold, 34f, 34f, -1.5f),
        displayLarge = mono(FontWeight.Bold, 60f, 60f, -3.0f),
        label = mono(FontWeight.Normal, 12f, null, 0.6f),
        labelWide = mono(FontWeight.Normal, 12f, null, 0.8f),
        button = mono(FontWeight.Normal, 13f, null, 0f),
        mono15 = mono(FontWeight.Normal, 15f, null, 0f),
        mono14 = mono(FontWeight.Normal, 14f, null, 0f),
        body = grotesk(FontWeight.Normal, 15f, 21.75f, 0f),
        bodyLarge = grotesk(FontWeight.Normal, 16f, null, 0f),
        bodySmall = grotesk(FontWeight.Normal, 14f, 21f, 0f),
        caption = grotesk(FontWeight.Normal, 13f, null, 0f),
        strongLarge = grotesk(FontWeight.SemiBold, 15f, null, 0f),
        strongSmall = grotesk(FontWeight.Bold, 14f, null, 0f),
    )
}

val LocalTmrTypography: ProvidableCompositionLocal<TmrTypography> =
    staticCompositionLocalOf { TmrTypographyTokens.Default }

internal fun TmrTypography.toMaterial(): Typography = Typography(
    displayLarge = displayLarge,
    displayMedium = display,
    displaySmall = headline,
    headlineLarge = headline,
    headlineMedium = headlineSmall,
    headlineSmall = title,
    titleLarge = title,
    titleMedium = strongLarge,
    titleSmall = strongSmall,
    bodyLarge = bodyLarge,
    bodyMedium = body,
    bodySmall = bodySmall,
    labelLarge = button,
    labelMedium = label,
    labelSmall = label,
)
