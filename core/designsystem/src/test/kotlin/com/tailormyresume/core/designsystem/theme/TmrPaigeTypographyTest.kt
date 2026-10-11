package com.tailormyresume.core.designsystem.theme

import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontListFontFamily
import androidx.compose.ui.unit.isSpecified
import androidx.compose.ui.unit.isUnspecified
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class TmrPaigeTypographyTest {
    private val typography = TmrTypographyTokens.Default

    private fun check(
        style: TextStyle,
        family: FontFamily,
        weightValue: Int,
        sizeSp: Float,
        lineHeightSp: Float?,
        trackingSp: Float,
    ) {
        assertEquals(family, style.fontFamily)
        assertEquals(weightValue, style.fontWeight?.weight)
        assertEquals(sizeSp, style.fontSize.value, 0.001f)
        val tracking = if (style.letterSpacing.isUnspecified) 0f else style.letterSpacing.value
        assertEquals(trackingSp, tracking, 0.001f)
        if (lineHeightSp == null) {
            assertTrue(style.lineHeight.isUnspecified)
        } else {
            assertTrue(style.lineHeight.isSpecified)
            assertEquals(lineHeightSp, style.lineHeight.value, 0.01f)
        }
    }

    @Test
    fun everyPlanTypeStyleMatchesFamilyWeightSizeLineHeightAndTracking() {
        val mono = TmrFontFamilies.mono
        val grotesk = TmrFontFamilies.grotesk

        check(typography.headline, mono, 400, 28f, 30.8f, -1.0f)
        check(typography.headlineSmall, mono, 400, 26f, 28.6f, -1.0f)
        check(typography.title, mono, 400, 22f, 25.3f, -0.6f)
        check(typography.display, mono, 700, 34f, 34.0f, -1.5f)
        check(typography.displayLarge, mono, 700, 60f, 60.0f, -3.0f)
        check(typography.label, mono, 400, 12f, null, 0.6f)
        check(typography.labelWide, mono, 400, 12f, null, 0.8f)
        check(typography.button, mono, 400, 13f, null, 0f)
        check(typography.mono15, mono, 400, 15f, null, 0f)
        check(typography.mono14, mono, 400, 14f, null, 0f)
        check(typography.body, grotesk, 400, 15f, 21.75f, 0f)
        check(typography.bodyLarge, grotesk, 400, 16f, null, 0f)
        check(typography.bodySmall, grotesk, 400, 14f, 21.0f, 0f)
        check(typography.caption, grotesk, 400, 13f, null, 0f)
        check(typography.strongLarge, grotesk, 600, 15f, null, 0f)
        check(typography.strongSmall, grotesk, 700, 14f, null, 0f)
    }

    @Test
    fun groteskVariableFontCoversWeights400To700() {
        val family = TmrFontFamilies.grotesk
        assertTrue(family is FontListFontFamily, "grotesk was ${family::class.java.name}")
        val weights = family.fonts.map { it.weight.weight }.toSet()
        listOf(400, 500, 600, 700).forEach { weight ->
            assertTrue(weights.contains(weight), "grotesk had weights $weights")
        }
    }

    @Test
    fun monoFamilyHasRegularAndBold() {
        val family = TmrFontFamilies.mono
        assertTrue(family is FontListFontFamily, "mono was ${family::class.java.name}")
        val weights = family.fonts.map { it.weight.weight }.toSet()
        listOf(400, 700).forEach { weight ->
            assertTrue(weights.contains(weight), "mono had weights $weights")
        }
    }

    @Test
    fun legacyStylesStillExist() {
        val legacy: List<TextStyle> = listOf(
            typography.displayL,
            typography.displayM,
            typography.headlineL,
            typography.headlineM,
            typography.titleL,
            typography.titleM,
            typography.titleS,
            typography.bodyL,
            typography.bodyM,
            typography.labelL,
            typography.labelM,
            typography.bodyS,
            typography.numeralHero,
            typography.numeralM,
            typography.factId,
        )
        assertEquals(15, legacy.size)
        legacy.forEach { style ->
            assertNotNull(style)
            assertTrue(style.fontSize.isSpecified)
        }
    }
}
