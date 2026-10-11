package com.tailormyresume.core.designsystem.component.content

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.State
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.SemanticsPropertyKey
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.unit.dp
import com.tailormyresume.core.designsystem.R
import com.tailormyresume.core.designsystem.component.TmrDivider
import com.tailormyresume.core.designsystem.icon.TmrIcons
import com.tailormyresume.core.designsystem.theme.TmrColors
import com.tailormyresume.core.designsystem.theme.TmrMotion
import com.tailormyresume.core.designsystem.theme.TmrTheme

internal val TmrSpinnerAnimatedKey = SemanticsPropertyKey<Boolean>("TmrSpinnerAnimated")

enum class TmrProgressState { Done, Active, Pending }

@Immutable
data class TmrProgressRow(val label: String, val state: TmrProgressState, val meta: String? = null)

@Immutable
data class TmrProgressRowColors(val label: Color, val meta: Color)

private val MarkerSize = 24.dp

private val MarkerIconSize = 14.dp

private val MarkerBorder = 2.5.dp

private val RowMinHeight = 52.dp

private val MarkerGap = 14.dp

private val RowsGap = 16.dp

private val CardHorizontalPadding = 16.dp

private val CardVerticalPadding = 4.dp

private const val SPINNER_SWEEP_DEGREES = 90f

private const val SPINNER_START_DEGREES = -90f

internal fun tmrProgressRowColors(state: TmrProgressState, colors: TmrColors): TmrProgressRowColors =
    when (state) {
        TmrProgressState.Done -> TmrProgressRowColors(colors.text, colors.lime)
        TmrProgressState.Active -> TmrProgressRowColors(colors.text, colors.textMuted)
        TmrProgressState.Pending -> TmrProgressRowColors(colors.textMuted, colors.textMuted)
    }

internal fun tmrSpinnerAngle(frameTimeMs: Long, motion: TmrMotion): Float =
    if (motion.reduced || !motion.idle.spinnerAnimated) {
        0f
    } else {
        (frameTimeMs % motion.idle.spinnerTurnMs) / motion.idle.spinnerTurnMs.toFloat() * 360f
    }

@Composable
private fun tmrProgressStateDescription(state: TmrProgressState): String =
    when (state) {
        TmrProgressState.Done -> stringResource(R.string.core_designsystem_content_progress_done)
        TmrProgressState.Active -> stringResource(R.string.core_designsystem_content_progress_active)
        TmrProgressState.Pending -> stringResource(R.string.core_designsystem_content_progress_pending)
    }

@Composable
private fun TmrProgressMarker(state: TmrProgressState, modifier: Modifier = Modifier) {
    val colors = TmrTheme.colors
    when (state) {
        TmrProgressState.Done -> Box(
            modifier = modifier.size(MarkerSize).background(colors.lime, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = TmrIcons.Check,
                contentDescription = null,
                tint = colors.ink,
                modifier = Modifier.size(MarkerIconSize),
            )
        }

        TmrProgressState.Pending -> Box(
            modifier = modifier.size(MarkerSize).border(MarkerBorder, colors.boundary, CircleShape),
        )

        TmrProgressState.Active -> ProgressSpinner(modifier)
    }
}

@Composable
private fun ProgressSpinner(modifier: Modifier = Modifier) {
    val colors = TmrTheme.colors
    val motion = TmrTheme.motion
    val turnMs = motion.idle.spinnerTurnMs
    val frame: State<Float>? = if (motion.idle.spinnerAnimated && !motion.reduced) {
        rememberInfiniteTransition(label = "ProgressSpinner").animateFloat(
            initialValue = 0f,
            targetValue = turnMs.toFloat(),
            animationSpec = infiniteRepeatable(tween(durationMillis = turnMs, easing = LinearEasing)),
            label = "ProgressSpinnerFrame",
        )
    } else {
        null
    }
    Canvas(
        modifier = modifier
            .size(MarkerSize)
            .clearAndSetSemantics { this[TmrSpinnerAnimatedKey] = frame != null }
            .graphicsLayer { rotationZ = frame?.let { tmrSpinnerAngle(it.value.toLong(), motion) } ?: 0f },
    ) {
        val stroke = MarkerBorder.toPx()
        val inset = stroke / 2f
        val arcSize = Size(size.width - stroke, size.height - stroke)
        val topLeft = Offset(inset, inset)
        drawArc(
            color = colors.lineStrong,
            startAngle = 0f,
            sweepAngle = 360f,
            useCenter = false,
            topLeft = topLeft,
            size = arcSize,
            style = Stroke(width = stroke),
        )
        drawArc(
            color = colors.lime,
            startAngle = SPINNER_START_DEGREES,
            sweepAngle = SPINNER_SWEEP_DEGREES,
            useCenter = false,
            topLeft = topLeft,
            size = arcSize,
            style = Stroke(width = stroke, cap = StrokeCap.Round),
        )
    }
}

@Composable
private fun TmrProgressRowItem(row: TmrProgressRow, modifier: Modifier = Modifier) {
    val colors = TmrTheme.colors
    val rowColors = tmrProgressRowColors(row.state, colors)
    val stateDescription = tmrProgressStateDescription(row.state)
    Row(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = RowMinHeight)
            .padding(vertical = 14.dp)
            .semantics(mergeDescendants = true) { this.stateDescription = stateDescription },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(MarkerGap),
    ) {
        TmrProgressMarker(state = row.state)
        Text(
            text = row.label,
            style = TmrTheme.typography.body,
            color = rowColors.label,
            modifier = Modifier.weight(1f),
        )
        if (row.meta != null) {
            Text(
                text = row.meta,
                style = TmrTheme.typography.caption,
                color = rowColors.meta,
            )
        }
    }
}

@Composable
fun TmrProgressRows(rows: List<TmrProgressRow>, percent: Int, modifier: Modifier = Modifier) {
    val percentText = stringResource(R.string.core_designsystem_content_percent, percent)
    val percentState = pluralStringResource(R.plurals.core_designsystem_content_progress_percent_state, percent, percent)
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(RowsGap),
    ) {
        Text(
            text = percentText,
            style = TmrTheme.typography.display,
            color = TmrTheme.colors.lime,
            modifier = Modifier
                .align(Alignment.End)
                .semantics {
                    progressBarRangeInfo = ProgressBarRangeInfo(percent.coerceIn(0, 100) / 100f, 0f..1f)
                    stateDescription = percentState
                },
        )
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(TmrTheme.colors.surfaceRaised, TmrTheme.shapes.cardLarge)
                .padding(horizontal = CardHorizontalPadding, vertical = CardVerticalPadding),
        ) {
            rows.forEachIndexed { index, row ->
                TmrProgressRowItem(row = row)
                if (index < rows.lastIndex) {
                    TmrDivider(color = TmrTheme.colors.line)
                }
            }
        }
    }
}
