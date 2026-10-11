package com.tailormyresume.core.designsystem.theme

import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ProvidableCompositionLocal
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp

@Immutable
class TmrShapes(
    val sheet: Shape,
    val heroCard: Shape,
    val card: Shape,
    val field: Shape,
    val pill: Shape,
    val tag: Shape,
    val banner: Shape,
    val pillRow: Shape,
    val statusRow: Shape,
    val modalSheet: Shape,
    val monogram: Shape,
    val logoTile: Shape,
    val hero: Shape,
    val cardLarge: Shape,
    val cardSmall: Shape,
    val toast: Shape,
    val paperCorner: Shape,
    val bar: Shape,
)

internal object TmrShapesDefaults {
    val Default = TmrShapes(
        sheet = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        heroCard = RoundedCornerShape(22.dp),
        card = RoundedCornerShape(20.dp),
        field = RoundedCornerShape(18.dp),
        pill = CircleShape,
        tag = RoundedCornerShape(8.dp),
        banner = RoundedCornerShape(18.dp),
        pillRow = RoundedCornerShape(24.dp),
        statusRow = RoundedCornerShape(18.dp),
        modalSheet = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        monogram = RoundedCornerShape(14.dp),
        logoTile = RoundedCornerShape(11.dp),
        hero = RoundedCornerShape(32.dp),
        cardLarge = RoundedCornerShape(24.dp),
        cardSmall = RoundedCornerShape(16.dp),
        toast = RoundedCornerShape(18.dp),
        paperCorner = RoundedCornerShape(12.dp),
        bar = RoundedCornerShape(2.dp),
    )
}

val LocalTmrShapes: ProvidableCompositionLocal<TmrShapes> =
    staticCompositionLocalOf { TmrShapesDefaults.Default }

internal val TmrMaterialShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(20.dp),
    extraLarge = RoundedCornerShape(24.dp),
)
