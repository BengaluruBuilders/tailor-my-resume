package com.tailormyresume.core.designsystem.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.isSpecified
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

class TmrPaigeColorValuesTest {
    @Test
    fun everyPlanColourValueIsExact() {
        assertEquals(Color(0xFF000000), TmrDarkColors.background)
        assertEquals(Color(0xFF111111), TmrDarkColors.surface)
        assertEquals(Color(0xFF161616), TmrDarkColors.surfaceRaised)
        assertEquals(Color(0xFF1C1C1C), TmrDarkColors.surfaceHigh)
        assertEquals(Color(0xFF141414), TmrDarkColors.sheet)
        assertEquals(Color(0xFF1A1A1A), TmrDarkColors.sheetOption)
        assertEquals(Color(0xFF0D0D0D), TmrDarkColors.uploadCard)
        assertEquals(Color(0xFF242424), TmrDarkColors.fill)
        assertEquals(Color(0xFF222222), TmrDarkColors.disabledFill)
        assertEquals(Color(0xFF2A2A2A), TmrDarkColors.line)
        assertEquals(Color(0xFF333333), TmrDarkColors.lineStrong)
        assertEquals(Color(0xFF3A3A3A), TmrDarkColors.lineHigher)
        assertEquals(Color(0xFF1A1A1A), TmrDarkColors.tabDivider)
        assertEquals(Color(0xFFFFFFFF), TmrDarkColors.text)
        assertEquals(Color(0xFFC8C8C8), TmrDarkColors.textSecondary)
        assertEquals(Color(0xFFA6A6A6), TmrDarkColors.textMuted)
        assertEquals(Color(0xFF7D7D7D), TmrDarkColors.textDisabled)
        assertEquals(Color(0xFF0A0A0A), TmrDarkColors.ink)
        assertEquals(Color(0xFFA3F43F), TmrDarkColors.lime)
        assertEquals(Color(0xFF141A0A), TmrDarkColors.limeSelected)
        assertEquals(Color(0xFFE4FBB8), TmrDarkColors.limeSoft)
        assertEquals(Color(0xFFF7A940), TmrDarkColors.amber)
        assertEquals(Color(0xFFFFE2C2), TmrDarkColors.amberHighlight)
        assertEquals(Color(0xFF5AA9F8), TmrDarkColors.blue)
        assertEquals(Color(0xFFFF6A2B), TmrDarkColors.cheek)
        assertEquals(Color(0xFFFFFFFF), TmrDarkColors.paper)
        assertEquals(Color(0xFFDCDCD5), TmrDarkColors.paperFold)
        assertEquals(Color(0xFFE9E8E4), TmrDarkColors.segOffer)
        assertEquals(Color(0xFF555555), TmrDarkColors.segRejected)
        assertEquals(Color(0xFF444444), TmrDarkColors.rejectedBorder)

        val scrim = TmrDarkColors.scrim
        assertEquals(0f, scrim.red, 0.0001f)
        assertEquals(0f, scrim.green, 0.0001f)
        assertEquals(0f, scrim.blue, 0.0001f)
        assertTrue(
            kotlin.math.abs(scrim.alpha - 0.65f) < 0.005f,
            "scrim alpha was ${scrim.alpha}",
        )
    }

    @Test
    fun disabledAndSecondaryTextAreDistinct() {
        assertNotEquals(TmrDarkColors.textDisabled, TmrDarkColors.textMuted)
        assertNotEquals(TmrDarkColors.disabledContent, TmrDarkColors.onSurfaceVariant)
        assertEquals(TmrDarkColors.textDisabled, TmrDarkColors.disabledContent)
        assertEquals(TmrDarkColors.textMuted, TmrDarkColors.onSurfaceVariant)
        assertEquals(TmrDarkColors.text, TmrDarkColors.onSurface)
        assertEquals(TmrDarkColors.lime, TmrDarkColors.primary)
        assertEquals(TmrDarkColors.ink, TmrDarkColors.onPrimary)
    }

    @Test
    fun everyLegacyMemberStillExists() {
        val colors: List<Color> = listOf(
            TmrDarkColors.header,
            TmrDarkColors.headerShape,
            TmrDarkColors.onHeader,
            TmrDarkColors.onHeaderVariant,
            TmrDarkColors.onHeaderControl,
            TmrDarkColors.headerControl,
            TmrDarkColors.primary,
            TmrDarkColors.onPrimary,
            TmrDarkColors.primaryContainer,
            TmrDarkColors.onPrimaryContainer,
            TmrDarkColors.card,
            TmrDarkColors.ground,
            TmrDarkColors.document,
            TmrDarkColors.tool,
            TmrDarkColors.onTool,
            TmrDarkColors.onToolVariant,
            TmrDarkColors.onToolSelected,
            TmrDarkColors.outline,
            TmrDarkColors.outlineVariant,
            TmrDarkColors.outlineSoft,
            TmrDarkColors.boundary,
            TmrDarkColors.body,
            TmrDarkColors.inverseSurface,
            TmrDarkColors.inverseOnSurface,
            TmrDarkColors.inversePrimary,
            TmrDarkColors.met,
            TmrDarkColors.metContainer,
            TmrDarkColors.onMetContainer,
            TmrDarkColors.partial,
            TmrDarkColors.partialContainer,
            TmrDarkColors.onPartialContainer,
            TmrDarkColors.gap,
            TmrDarkColors.gapContainer,
            TmrDarkColors.onGapContainer,
            TmrDarkColors.evidence,
            TmrDarkColors.evidenceLine,
            TmrDarkColors.error,
            TmrDarkColors.onError,
            TmrDarkColors.errorContainer,
            TmrDarkColors.onErrorContainer,
            TmrDarkColors.brand,
            TmrDarkColors.brandPressed,
            TmrDarkColors.sheetItemBorder,
            TmrDarkColors.onBrand,
            TmrDarkColors.coral,
            TmrDarkColors.onCoral,
            TmrDarkColors.special,
            TmrDarkColors.onSpecial,
            TmrDarkColors.onLogoTile,
            TmrDarkColors.neutralContainer,
            TmrDarkColors.onNeutralContainer,
        )
        assertEquals(51, colors.size)
        val tiles: List<Color> = TmrDarkColors.logoTiles
        assertEquals(4, tiles.size)
        assertTrue((colors + tiles).all { it.isSpecified })
    }
}
