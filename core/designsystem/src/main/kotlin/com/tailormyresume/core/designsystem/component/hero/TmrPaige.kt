package com.tailormyresume.core.designsystem.component.hero

import androidx.compose.animation.core.withInfiniteAnimationFrameNanos
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.tailormyresume.core.designsystem.theme.TmrColors
import com.tailormyresume.core.designsystem.theme.TmrMotion
import com.tailormyresume.core.designsystem.theme.TmrTheme
import kotlin.math.min
import kotlin.math.sin

enum class TmrPaigePose { Signin1, Signin2, Signin3, Upload, Job, Question, Tailoring, Fail, Home, Paywall, Delete }

@Immutable
internal class TmrPaigeSpec(
    val eyeWidth: Dp = 14.dp,
    val eyeHeight: Dp = 19.dp,
    val eyeShiftX: Dp = 0.dp,
    val eyeShiftY: Dp = 0.dp,
    val mouthWidth: Dp = 22.dp,
    val mouthHeight: Dp = 12.dp,
    val ticks: Boolean = false,
    val arms: Boolean = true,
    val sad: Boolean = false,
    val rotationDegrees: Float = 0f,
    val shiftX: Dp = 0.dp,
    val lift: Dp = 0.dp,
    val bobPhase: Int? = null,
    val wobble: Boolean = false,
)

internal fun TmrPaigePose.spec(): TmrPaigeSpec = when (this) {
    TmrPaigePose.Signin1 -> TmrPaigeSpec(ticks = true, rotationDegrees = 4f, lift = (-46).dp, bobPhase = 0)
    TmrPaigePose.Signin2 -> TmrPaigeSpec(
        eyeWidth = 11.dp,
        eyeHeight = 15.dp,
        eyeShiftX = (-4).dp,
        mouthWidth = 16.dp,
        mouthHeight = 8.dp,
        ticks = true,
        rotationDegrees = -8f,
        shiftX = 56.dp,
        lift = 10.dp,
        bobPhase = 0,
    )
    TmrPaigePose.Signin3 -> TmrPaigeSpec(
        eyeWidth = 18.dp,
        eyeHeight = 6.dp,
        mouthWidth = 30.dp,
        mouthHeight = 15.dp,
        ticks = true,
        rotationDegrees = -5f,
        shiftX = (-30).dp,
        lift = 16.dp,
        bobPhase = 0,
    )
    TmrPaigePose.Upload -> TmrPaigeSpec(ticks = true, rotationDegrees = 4f, bobPhase = 0)
    TmrPaigePose.Job -> TmrPaigeSpec(
        eyeWidth = 11.dp,
        eyeHeight = 15.dp,
        eyeShiftX = (-4).dp,
        mouthWidth = 16.dp,
        mouthHeight = 8.dp,
        rotationDegrees = -6f,
        bobPhase = 1,
    )
    TmrPaigePose.Question -> TmrPaigeSpec(
        eyeWidth = 13.dp,
        eyeHeight = 17.dp,
        eyeShiftY = (-3).dp,
        mouthWidth = 14.dp,
        mouthHeight = 8.dp,
        rotationDegrees = 6f,
        bobPhase = 2,
    )
    TmrPaigePose.Tailoring -> TmrPaigeSpec(
        eyeWidth = 16.dp,
        eyeHeight = 6.dp,
        mouthWidth = 20.dp,
        mouthHeight = 10.dp,
        ticks = true,
        arms = false,
        wobble = true,
    )
    TmrPaigePose.Fail -> TmrPaigeSpec(
        eyeWidth = 11.dp,
        eyeHeight = 13.dp,
        eyeShiftY = 4.dp,
        sad = true,
        rotationDegrees = -5f,
    )
    TmrPaigePose.Home -> TmrPaigeSpec(ticks = true, rotationDegrees = 5f, bobPhase = 3)
    TmrPaigePose.Paywall -> TmrPaigeSpec(rotationDegrees = 5f, bobPhase = 4)
    TmrPaigePose.Delete -> TmrPaigeSpec(
        eyeWidth = 11.dp,
        eyeHeight = 13.dp,
        eyeShiftY = 4.dp,
        arms = false,
        sad = true,
        rotationDegrees = -4f,
    )
}

internal fun paigeTranslationYDp(motion: TmrMotion, spec: TmrPaigeSpec, timeMs: Long): Float {
    val phase = spec.bobPhase
    return when {
        spec.wobble -> motion.idle.wobbleOffset.value * sin(timeMs / motion.idle.wobbleOffsetAngularPeriodMs)
        phase != null -> motion.idle.bobOffsetDp(timeMs, phase)
        else -> 0f
    }
}

internal fun paigeRotationDegrees(motion: TmrMotion, spec: TmrPaigeSpec, timeMs: Long): Float =
    if (spec.wobble) motion.idle.wobbleDegrees * sin(timeMs / motion.idle.wobbleAngularPeriodMs) else 0f

private val stillClock: () -> Long = { 0L }

@Composable
internal fun rememberPaigeTimeMs(motion: TmrMotion, animated: Boolean): () -> Long {
    val time = remember { mutableLongStateOf(0L) }
    val running = !motion.reduced && animated
    LaunchedEffect(running) {
        if (!running) return@LaunchedEffect
        val start = withInfiniteAnimationFrameNanos { it }
        while (true) {
            withInfiniteAnimationFrameNanos { time.longValue = (it - start) / NANOS_PER_MILLI }
        }
    }
    return if (running) remember { { time.longValue } } else stillClock
}

private const val NANOS_PER_MILLI = 1_000_000L

@Composable
fun TmrPaige(pose: TmrPaigePose, modifier: Modifier = Modifier) = TmrPaige(pose, modifier, ::rememberPaigeTimeMs)

@Composable
internal fun TmrPaige(
    pose: TmrPaigePose,
    modifier: Modifier,
    rememberTimeMs: @Composable (TmrMotion, Boolean) -> () -> Long,
) {
    val motion = TmrTheme.motion
    val colors = TmrTheme.colors
    val spec = remember(pose) { pose.spec() }
    val timeMs = rememberTimeMs(motion, spec.wobble || spec.bobPhase != null)
    Canvas(
        modifier = modifier
            .size(PageWidth, PageHeight)
            .clearAndSetSemantics { }
            .graphicsLayer {
                val now = timeMs()
                translationX = spec.shiftX.toPx()
                translationY = (spec.lift.value + paigeTranslationYDp(motion, spec, now)) * density
                rotationZ = spec.rotationDegrees + paigeRotationDegrees(motion, spec, now)
            },
    ) {
        drawPaige(spec, colors)
    }
}

private val PageWidth = 124.dp
private val PageHeight = 154.dp
private val Outline = 2.5.dp

private fun DrawScope.drawPaige(spec: TmrPaigeSpec, colors: TmrColors) {
    if (spec.ticks) drawTicks(colors)
    if (spec.arms) drawArms(colors)
    drawPage(colors)
    drawFold(colors)
    drawEyes(spec, colors)
    if (spec.sad) drawSadMouth(colors) else drawHappyFace(spec, colors)
    drawTextBars(colors)
}

private fun DrawScope.drawTicks(colors: TmrColors) {
    val originX = 22.dp.toPx()
    val originY = (-40).dp.toPx()
    listOf(
        Triple(6.dp to 12.dp, 18.dp, -35f),
        Triple(37.dp to 0.dp, 20.dp, 0f),
        Triple(69.dp to 12.dp, 18.dp, 35f),
    ).forEach { (position, height, degrees) ->
        val topLeft = Offset(originX + position.first.toPx(), originY + position.second.toPx())
        val size = Size(5.dp.toPx(), height.toPx())
        rotate(degrees, pivot = topLeft + Offset(size.width / 2, size.height / 2)) {
            drawRoundRect(colors.ink, topLeft, size, CornerRadius(3.dp.toPx()))
        }
    }
}

private fun DrawScope.drawArms(colors: TmrColors) {
    val diameter = 27.dp.toPx()
    val stroke = Outline.toPx()
    listOf((-16).dp.toPx(), PageWidth.toPx() + 16.dp.toPx() - diameter).forEach { left ->
        val center = Offset(left + diameter / 2, 92.dp.toPx() + diameter / 2)
        drawCircle(colors.paper, diameter / 2, center)
        drawCircle(colors.ink, diameter / 2 - stroke / 2, center, style = Stroke(stroke))
    }
}

private fun DrawScope.drawPage(colors: TmrColors) {
    val radius = 16.dp.toPx()
    val stroke = Outline.toPx()
    drawRoundRect(colors.paper, size = size, cornerRadius = CornerRadius(radius))
    drawRoundRect(
        colors.ink,
        topLeft = Offset(stroke / 2, stroke / 2),
        size = Size(size.width - stroke, size.height - stroke),
        cornerRadius = CornerRadius(radius - stroke / 2),
        style = Stroke(stroke),
    )
}

private fun DrawScope.drawFold(colors: TmrColors) {
    val stroke = Outline.toPx()
    val side = 32.5.dp.toPx()
    val left = size.width - stroke - side
    val top = stroke
    val bottom = top + side
    val right = left + side
    val lower = 10.dp.toPx()
    val fill = Path().apply {
        addRoundRect(
            RoundRect(
                Rect(left, top, right, bottom),
                topLeft = CornerRadius.Zero,
                topRight = CornerRadius(12.dp.toPx()),
                bottomRight = CornerRadius.Zero,
                bottomLeft = CornerRadius(lower),
            ),
        )
    }
    drawPath(fill, colors.paperFold)
    val edge = Path().apply {
        moveTo(left + stroke / 2, top)
        lineTo(left + stroke / 2, bottom - lower)
        quadraticTo(left + stroke / 2, bottom - stroke / 2, left + lower, bottom - stroke / 2)
        lineTo(right, bottom - stroke / 2)
    }
    drawPath(edge, colors.ink, style = Stroke(stroke))
}

private fun DrawScope.drawEyes(spec: TmrPaigeSpec, colors: TmrColors) {
    val width = spec.eyeWidth.toPx()
    val height = spec.eyeHeight.toPx()
    val gap = 26.dp.toPx()
    val startX = (size.width - (2 * width + gap)) / 2 + spec.eyeShiftX.toPx()
    val top = 48.dp.toPx() + spec.eyeShiftY.toPx()
    listOf(startX, startX + width + gap).forEach { left ->
        drawRoundRect(colors.ink, Offset(left, top), Size(width, height), CornerRadius(min(width, height) / 2))
    }
}

private fun DrawScope.drawHappyFace(spec: TmrPaigeSpec, colors: TmrColors) {
    val cheekSize = Size(16.dp.toPx(), 9.dp.toPx())
    drawOval(colors.cheek, Offset(16.dp.toPx(), 74.dp.toPx()), cheekSize)
    drawOval(colors.cheek, Offset(size.width - 16.dp.toPx() - cheekSize.width, 74.dp.toPx()), cheekSize)
    val stroke = Outline.toPx()
    val width = (spec.mouthWidth + 5.dp).toPx()
    val height = (spec.mouthHeight + 2.5.dp).toPx()
    val left = (size.width - width) / 2 + stroke / 2
    val right = (size.width + width) / 2 - stroke / 2
    val top = 72.dp.toPx()
    val bottom = top + height - stroke / 2
    val radius = min(width / 2, height) - stroke / 2
    val mouth = Path().apply {
        moveTo(left, top)
        lineTo(left, bottom - radius)
        arcTo(Rect(left, bottom - 2 * radius, left + 2 * radius, bottom), 180f, -90f, false)
        lineTo(right - radius, bottom)
        arcTo(Rect(right - 2 * radius, bottom - 2 * radius, right, bottom), 90f, -90f, false)
        lineTo(right, top)
    }
    drawPath(mouth, colors.ink, style = Stroke(stroke))
}

private fun DrawScope.drawSadMouth(colors: TmrColors) {
    val stroke = Outline.toPx()
    val width = 25.dp.toPx()
    val height = 12.5.dp.toPx()
    val left = (size.width - width) / 2 + stroke / 2
    val right = (size.width + width) / 2 - stroke / 2
    val boxTop = 78.dp.toPx()
    val top = boxTop + stroke / 2
    val radius = min(width / 2, height) - stroke / 2
    val mouth = Path().apply {
        moveTo(left, boxTop + height)
        lineTo(left, top + radius)
        arcTo(Rect(left, top, left + 2 * radius, top + 2 * radius), 180f, 90f, false)
        lineTo(right - radius, top)
        arcTo(Rect(right - 2 * radius, top, right, top + 2 * radius), 270f, 90f, false)
        lineTo(right, boxTop + height)
    }
    drawPath(mouth, colors.ink, style = Stroke(stroke))
}

private fun DrawScope.drawTextBars(colors: TmrColors) {
    val left = 22.dp.toPx()
    val barHeight = 4.dp.toPx()
    val full = size.width - 2 * left
    val lowerTop = size.height - 22.dp.toPx() - barHeight
    val upperTop = lowerTop - 7.dp.toPx() - barHeight
    drawRoundRect(colors.ink, Offset(left, upperTop), Size(full, barHeight), CornerRadius(2.dp.toPx()))
    drawRoundRect(colors.ink, Offset(left, lowerTop), Size(full * 0.6f, barHeight), CornerRadius(2.dp.toPx()))
}
