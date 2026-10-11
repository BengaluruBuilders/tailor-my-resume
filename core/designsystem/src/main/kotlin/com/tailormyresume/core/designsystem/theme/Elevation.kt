package com.tailormyresume.core.designsystem.theme

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Immutable
data class TmrShadow(
    val elevation: Dp,
    val offsetY: Dp,
    val blurRadius: Dp,
    val cornerRadius: Dp,
    val ambientColor: Color,
    val spotColor: Color,
)

@Immutable
class TmrElevation(
    val level0: TmrShadow,
    val level1: TmrShadow,
    val level2: TmrShadow,
    val level3: TmrShadow,
    val level4: TmrShadow,
) {
    val hero: TmrShadow get() = level2
    val dock: TmrShadow get() = level3
    val modal: TmrShadow get() = level4
}

val LocalTmrElevation = staticCompositionLocalOf { TmrElevationDefaults.Dark }

fun Modifier.tmrShadow(shadow: TmrShadow, shape: Shape): Modifier =
    if (shadow.elevation == 0.dp) {
        this
    } else {
        shadow(
            elevation = shadow.elevation,
            shape = shape,
            ambientColor = shadow.ambientColor,
            spotColor = shadow.spotColor,
        )
    }

internal object TmrElevationDefaults {
    private fun step(elevation: Int, offsetY: Int, blur: Int, alpha: Float, ink: Color) = TmrShadow(
        elevation = elevation.dp,
        offsetY = offsetY.dp,
        blurRadius = blur.dp,
        cornerRadius = 0.dp,
        ambientColor = ink.copy(alpha = alpha),
        spotColor = ink.copy(alpha = alpha),
    )

    private val DarkInk = Color(0xFF000000)

    val Dark = TmrElevation(
        level0 = step(0, 0, 0, 0f, DarkInk),
        level1 = step(0, 0, 0, 0f, DarkInk),
        level2 = step(2, 2, 8, 0.32f, DarkInk),
        level3 = step(4, 4, 16, 0.42f, DarkInk),
        level4 = step(8, 8, 24, 0.48f, DarkInk),
    )
}
