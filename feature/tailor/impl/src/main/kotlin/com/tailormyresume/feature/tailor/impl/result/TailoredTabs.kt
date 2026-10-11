package com.tailormyresume.feature.tailor.impl.result

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.selectable
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.tailormyresume.core.designsystem.theme.TmrTheme
import com.tailormyresume.feature.tailor.impl.R

@Composable
internal fun TailoredTabs(
    selected: TailoredTab,
    changeCount: Int,
    onSelect: (TailoredTab) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = TmrTheme.colors
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(IntrinsicSize.Min)
            .clip(TmrTheme.shapes.pill)
            .background(colors.surfaceRaised)
            .padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        TailoredTab.entries.forEach { tab ->
            val isSelected = tab == selected
            val label = when (tab) {
                TailoredTab.Resume -> stringResource(R.string.feature_tailor_impl_result_tab_resume)
                TailoredTab.Changes ->
                    stringResource(R.string.feature_tailor_impl_result_tab_changes, changeCount)
            }
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .defaultMinSize(minHeight = 44.dp)
                    .clip(TmrTheme.shapes.pill)
                    .background(if (isSelected) colors.lime else Color.Transparent)
                    .selectable(
                        selected = isSelected,
                        role = Role.Tab,
                        onClick = { onSelect(tab) },
                    )
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = label,
                    style = TmrTheme.typography.mono14,
                    color = if (isSelected) colors.ink else colors.textSecondary,
                    textAlign = TextAlign.Center,
                )
            }
        }
    }
}
