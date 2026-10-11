package com.tailormyresume.core.designsystem.theme

import androidx.compose.foundation.shape.CornerBasedShape
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import org.junit.Test
import kotlin.test.assertEquals

class TmrPaigeShapesAndSpacingTest {
    private val reference = Size(1000f, 1000f)
    private val density = Density(1f)

    private fun topStartRadius(shape: Shape): Float =
        (shape as CornerBasedShape).topStart.toPx(reference, density)

    @Test
    fun radiiAndSizesMatchPlan() {
        val shapes = TmrShapesDefaults.Default
        assertEquals(32f, topStartRadius(shapes.hero))
        assertEquals(24f, topStartRadius(shapes.cardLarge))
        assertEquals(20f, topStartRadius(shapes.card))
        assertEquals(16f, topStartRadius(shapes.cardSmall))
        assertEquals(18f, topStartRadius(shapes.toast))
        assertEquals(12f, topStartRadius(shapes.paperCorner))
        assertEquals(2f, topStartRadius(shapes.bar))
        assertEquals(500f, topStartRadius(shapes.pill))

        val spacing = TmrSpacingDefaults.Default
        assertEquals(44.dp, spacing.topBarButton)
        assertEquals(56.dp, spacing.primaryButtonHeight)
        assertEquals(54.dp, spacing.tabItem)
        assertEquals(56.dp, spacing.tabCentreDisc)
        assertEquals(96.dp, spacing.toastTop)
        assertEquals(12.dp, spacing.sheetPaddingTop)
        assertEquals(18.dp, spacing.sheetPaddingHorizontal)
        assertEquals(40.dp, spacing.sheetPaddingBottom)
        assertEquals(44.dp, spacing.sheetHandleWidth)
        assertEquals(4.dp, spacing.sheetHandleHeight)
        assertEquals(14.dp, spacing.gutter)
    }
}
