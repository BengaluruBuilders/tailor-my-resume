package com.tailormyresume.core.designsystem.component.content

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.unit.dp
import com.tailormyresume.core.designsystem.R
import com.tailormyresume.core.designsystem.theme.TmrColors
import com.tailormyresume.core.designsystem.theme.TmrTheme

private val BarHeight = 10.dp

private val LegendDot = 7.dp

@Immutable
data class TmrStatusBarSegment(val status: TmrApplicationStatus, val count: Int, val color: Color)

private fun TmrApplicationStatus.segmentColor(colors: TmrColors): Color =
    when (this) {
        TmrApplicationStatus.Saved -> colors.blue
        TmrApplicationStatus.Applied -> colors.amber
        TmrApplicationStatus.Interview -> colors.lime
        TmrApplicationStatus.Offer -> colors.segOffer
        TmrApplicationStatus.Rejected -> colors.segRejected
    }

internal fun tmrStatusBarSegments(counts: Map<TmrApplicationStatus, Int>, colors: TmrColors): List<TmrStatusBarSegment> =
    TmrApplicationStatus.entries.mapNotNull { status ->
        counts[status]?.takeIf { it > 0 }?.let { TmrStatusBarSegment(status, it, status.segmentColor(colors)) }
    }

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun TmrStatusBar(counts: Map<TmrApplicationStatus, Int>, modifier: Modifier = Modifier) {
    val segments = tmrStatusBarSegments(counts, TmrTheme.colors)
    if (segments.isEmpty()) return
    Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(TmrTheme.spacing.md)) {
        Row(
            modifier = Modifier.fillMaxWidth().height(BarHeight).clearAndSetSemantics {},
            horizontalArrangement = Arrangement.spacedBy(TmrTheme.spacing.xs),
        ) {
            segments.forEach { segment ->
                Box(
                    Modifier
                        .weight(segment.count.toFloat())
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(BarHeight / 2))
                        .background(segment.color),
                )
            }
        }
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            segments.forEach { segment ->
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Box(Modifier.size(LegendDot).background(segment.color, CircleShape))
                    Text(
                        text = stringResource(
                            R.string.core_designsystem_content_status_legend,
                            stringResource(segment.status.labelRes),
                            segment.count,
                        ),
                        style = TmrTheme.typography.caption,
                        color = TmrTheme.colors.textSecondary,
                    )
                }
            }
        }
    }
}
