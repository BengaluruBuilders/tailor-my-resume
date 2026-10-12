package com.tailormyresume.core.designsystem.component.chrome

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.tailormyresume.core.designsystem.R
import com.tailormyresume.core.designsystem.theme.TmrColors
import com.tailormyresume.core.designsystem.theme.TmrTheme

private val TRACK_HEIGHT = 4.dp

private val TRACK_GAP = 6.dp

private const val STEP_COUNT = 3

fun tmrStepTrackColor(step: Int, current: Int, colors: TmrColors): Color =
    if (step <= current) colors.lime else colors.surfaceHigh

@Composable
fun TmrStepBar(current: Int, modifier: Modifier = Modifier) {
    val names =
        listOf(
            stringResource(R.string.core_designsystem_chrome_step_profile),
            stringResource(R.string.core_designsystem_chrome_step_job),
            stringResource(R.string.core_designsystem_chrome_step_tailor),
        )
    val name = names[(current - 1).coerceIn(names.indices)]
    val description = stringResource(R.string.core_designsystem_chrome_step_description, current, name)
    Column(
        modifier = modifier.semantics(mergeDescendants = true) { contentDescription = description },
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Text(
            text = stringResource(R.string.core_designsystem_chrome_step_label, current, name),
            style = TmrTheme.typography.strongSmall,
            color = TmrTheme.colors.textSecondary,
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(TRACK_GAP),
        ) {
            repeat(STEP_COUNT) { index ->
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(TRACK_HEIGHT)
                        .background(
                            tmrStepTrackColor(index + 1, current, TmrTheme.colors),
                            RoundedCornerShape(TRACK_HEIGHT / 2),
                        ),
                )
            }
        }
    }
}
