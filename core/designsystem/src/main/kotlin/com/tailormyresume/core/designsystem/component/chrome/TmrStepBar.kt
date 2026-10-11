package com.tailormyresume.core.designsystem.component.chrome

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.tailormyresume.core.designsystem.R
import com.tailormyresume.core.designsystem.theme.TmrColors
import com.tailormyresume.core.designsystem.theme.TmrTheme

class TmrStepPillStyle(val container: Color, val content: Color)

fun tmrStepPillStyle(step: Int, current: Int, colors: TmrColors): TmrStepPillStyle =
    when {
        step < current -> TmrStepPillStyle(colors.surfaceHigh, colors.lime)
        step == current -> TmrStepPillStyle(colors.lime, colors.ink)
        else -> TmrStepPillStyle(colors.surfaceHigh, colors.textMuted)
    }

@Composable
fun TmrStepBar(current: Int, modifier: Modifier = Modifier) {
    val names =
        listOf(
            stringResource(R.string.core_designsystem_chrome_step_profile),
            stringResource(R.string.core_designsystem_chrome_step_job),
            stringResource(R.string.core_designsystem_chrome_step_tailor),
        )
    val enlarged = LocalDensity.current.fontScale > 1f
    val labels = names.mapIndexed { index, name ->
        if (index + 1 < current) {
            stringResource(R.string.core_designsystem_chrome_step_done_label, name)
        } else {
            stringResource(R.string.core_designsystem_chrome_step_pending_label, index + 1, name)
        }
    }
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        names.forEachIndexed { index, name ->
            val step = index + 1
            val style = tmrStepPillStyle(step, current, TmrTheme.colors)
            val description = stringResource(R.string.core_designsystem_chrome_step_description, step, name)
            val semantics =
                if (step == current) {
                    Modifier.semantics(mergeDescendants = true) { contentDescription = description }
                } else {
                    Modifier
                }
            Box(
                modifier =
                Modifier
                    .then(if (enlarged) Modifier.weight(1f) else Modifier)
                    .background(style.container, CircleShape)
                    .padding(horizontal = 11.dp, vertical = 7.dp)
                    .then(semantics),
                contentAlignment = Alignment.Center,
            ) {
                TmrCapsText(
                    text = labels[index],
                    style = TmrTheme.typography.label,
                    color = style.content,
                    fitTexts = if (enlarged) labels else emptyList(),
                )
            }
        }
    }
}
