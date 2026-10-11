package com.tailormyresume.core.designsystem.component.content

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.tailormyresume.core.designsystem.R
import com.tailormyresume.core.designsystem.theme.TmrTheme

private const val TRACK_ALPHA = 0.16f

@Composable
fun TmrStoryBars(count: Int, activeIndex: Int, activeFraction: Float, modifier: Modifier = Modifier) {
    val ink = TmrTheme.colors.ink
    val step = stringResource(R.string.core_designsystem_content_story_step, activeIndex + 1, count)
    Row(
        modifier = modifier
            .fillMaxWidth()
            .semantics(mergeDescendants = true) { contentDescription = step },
        horizontalArrangement = Arrangement.spacedBy(5.dp),
    ) {
        repeat(count) { index ->
            val fraction = when {
                index < activeIndex -> 1f
                index == activeIndex -> activeFraction.coerceIn(0f, 1f)
                else -> 0f
            }
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(2.dp)
                    .clip(TmrTheme.shapes.bar)
                    .background(ink.copy(alpha = TRACK_ALPHA)),
            ) {
                Box(Modifier.fillMaxHeight().fillMaxWidth(fraction).background(ink))
            }
        }
    }
}
