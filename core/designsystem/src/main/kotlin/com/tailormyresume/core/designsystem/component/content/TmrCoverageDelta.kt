package com.tailormyresume.core.designsystem.component.content

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.tailormyresume.core.designsystem.R
import com.tailormyresume.core.designsystem.component.readAs
import com.tailormyresume.core.designsystem.icon.TmrIcons
import com.tailormyresume.core.designsystem.theme.TmrTheme

private val ArrowSize = 24.dp

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun TmrCoverageDelta(now: Int, upTo: Int, modifier: Modifier = Modifier) {
    val summary = stringResource(R.string.core_designsystem_content_coverage_summary, now, upTo)
    Column(
        modifier = modifier
            .fillMaxWidth()
            .semantics(mergeDescendants = true) { contentDescription = summary },
        verticalArrangement = Arrangement.spacedBy(TmrTheme.spacing.md),
    ) {
        val label = stringResource(R.string.core_designsystem_content_keywords_matched)
        Text(
            text = label.uppercase(),
            style = TmrTheme.typography.label,
            color = TmrTheme.colors.textMuted,
            modifier = Modifier.readAs(label),
        )
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalArrangement = Arrangement.spacedBy(TmrTheme.spacing.sm),
            itemVerticalAlignment = Alignment.CenterVertically,
        ) {
            CoverageValue(now, stringResource(R.string.core_designsystem_content_coverage_now), TmrTheme.colors.textMuted)
            Icon(
                imageVector = TmrIcons.ArrowForward,
                contentDescription = null,
                tint = TmrTheme.colors.textDisabled,
                modifier = Modifier.size(ArrowSize).clearAndSetSemantics {},
            )
            CoverageValue(upTo, stringResource(R.string.core_designsystem_content_coverage_up_to), TmrTheme.colors.lime)
        }
    }
}

@Composable
private fun CoverageValue(percent: Int, caption: String, color: Color) {
    Column(verticalArrangement = Arrangement.spacedBy(TmrTheme.spacing.xxs)) {
        Text(
            text = stringResource(R.string.core_designsystem_content_percent, percent),
            style = TmrTheme.typography.display,
            color = color,
        )
        Text(text = caption, style = TmrTheme.typography.caption, color = color)
    }
}
