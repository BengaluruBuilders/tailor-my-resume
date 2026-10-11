package com.tailormyresume.core.designsystem.theme

import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.VisibilityThreshold
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import kotlin.math.sin

@Immutable
class TmrProofSpecs(
    val spatial: FiniteAnimationSpec<Float>,
    val spatialFast: FiniteAnimationSpec<Float>,
    val offset: FiniteAnimationSpec<IntOffset>,
    val size: FiniteAnimationSpec<IntSize>,
    val fade: FiniteAnimationSpec<Float>,
    val color: FiniteAnimationSpec<Color>,
    val staggerMs: Int,
    val staggerMax: Int,
)

@Immutable
class TmrHopSpecs(
    val spatial: FiniteAnimationSpec<Float>,
    val scale: FiniteAnimationSpec<Float>,
)

@Immutable
class TmrIdleSpecs(
    val bobAmplitude: Dp,
    val bobAngularPeriodMs: Float,
    val bobPhaseMax: Int,
    val wobbleDegrees: Float,
    val wobbleAngularPeriodMs: Float,
    val wobbleOffset: Dp,
    val wobbleOffsetAngularPeriodMs: Float,
    val storyDurationMs: Int,
    val spinnerTurnMs: Int,
    val storyAutoAdvance: Boolean,
    val spinnerAnimated: Boolean,
) {
    fun bobOffsetDp(timeMs: Long, phase: Int): Float =
        if (bobAngularPeriodMs == 0f) {
            0f
        } else {
            bobAmplitude.value * sin(timeMs / bobAngularPeriodMs + phase)
        }
}

@Immutable
class TmrMotion(
    val proofSpecs: TmrProofSpecs,
    val hopSpecs: TmrHopSpecs,
    val idle: TmrIdleSpecs,
    val reduced: Boolean,
)

val LocalTmrMotion = staticCompositionLocalOf { TmrMotionDefaults.Default }

internal object TmrMotionDefaults {
    val Default = TmrMotion(
        proofSpecs = TmrProofSpecs(
            spatial = spring(dampingRatio = 0.9f, stiffness = 600f),
            spatialFast = spring(dampingRatio = 1f, stiffness = 1400f),
            offset = spring(0.9f, 600f, IntOffset.VisibilityThreshold),
            size = spring(0.9f, 600f, IntSize.VisibilityThreshold),
            fade = tween(durationMillis = 150, easing = LinearOutSlowInEasing),
            color = tween(durationMillis = 150, easing = LinearOutSlowInEasing),
            staggerMs = 30,
            staggerMax = 6,
        ),
        hopSpecs = TmrHopSpecs(
            spatial = spring(dampingRatio = 0.55f, stiffness = 380f),
            scale = spring(dampingRatio = 0.5f, stiffness = 500f),
        ),
        idle = TmrIdleSpecs(
            bobAmplitude = 5.dp,
            bobAngularPeriodMs = 420f,
            bobPhaseMax = 4,
            wobbleDegrees = 4f,
            wobbleAngularPeriodMs = 300f,
            wobbleOffset = 6.dp,
            wobbleOffsetAngularPeriodMs = 210f,
            storyDurationMs = 5000,
            spinnerTurnMs = 900,
            storyAutoAdvance = true,
            spinnerAnimated = true,
        ),
        reduced = false,
    )

    val Reduced = TmrMotion(
        proofSpecs = TmrProofSpecs(
            spatial = snap(),
            spatialFast = snap(),
            offset = snap(),
            size = snap(),
            fade = tween(durationMillis = 150, easing = LinearOutSlowInEasing),
            color = tween(durationMillis = 150, easing = LinearOutSlowInEasing),
            staggerMs = 0,
            staggerMax = 0,
        ),
        hopSpecs = TmrHopSpecs(spatial = snap(), scale = snap()),
        idle = TmrIdleSpecs(
            bobAmplitude = 0.dp,
            bobAngularPeriodMs = 420f,
            bobPhaseMax = 4,
            wobbleDegrees = 0f,
            wobbleAngularPeriodMs = 300f,
            wobbleOffset = 0.dp,
            wobbleOffsetAngularPeriodMs = 210f,
            storyDurationMs = 5000,
            spinnerTurnMs = 900,
            storyAutoAdvance = false,
            spinnerAnimated = false,
        ),
        reduced = true,
    )
}
