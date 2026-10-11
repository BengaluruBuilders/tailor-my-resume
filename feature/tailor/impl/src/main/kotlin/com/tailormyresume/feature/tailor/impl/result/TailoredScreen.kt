package com.tailormyresume.feature.tailor.impl.result

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import com.tailormyresume.core.designsystem.component.TmrButtonSize
import com.tailormyresume.core.designsystem.component.content.TmrCard
import com.tailormyresume.core.designsystem.component.content.TmrResumePaper
import com.tailormyresume.core.designsystem.component.content.TmrSource
import com.tailormyresume.core.designsystem.component.content.TmrSourceChip
import com.tailormyresume.core.designsystem.component.content.TmrTag
import com.tailormyresume.core.designsystem.component.content.TmrTagChip
import com.tailormyresume.core.designsystem.component.input.TmrPrimaryButton
import com.tailormyresume.core.designsystem.component.input.TmrSecondaryButton
import com.tailormyresume.core.designsystem.theme.TmrTheme
import com.tailormyresume.feature.tailor.impl.R
import com.tailormyresume.core.designsystem.component.TmrSecondaryButton as TmrCompactButton

@Composable
internal fun TailoredScreen(
    state: TailoredUiState.Ready,
    tab: TailoredTab,
    onTabChange: (TailoredTab) -> Unit,
    onUndo: (String) -> Unit,
    onAcceptChanges: () -> Unit,
    onEdit: () -> Unit,
    onExport: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = TmrTheme.colors
    Column(modifier = modifier.fillMaxSize().background(colors.background)) {
        LazyColumn(
            modifier = Modifier.weight(1f).fillMaxWidth(),
            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            tailoredItems(state, tab, onTabChange, onUndo)
        }
        TailoredFooter(state, onAcceptChanges, onEdit, onExport)
    }
}

private fun LazyListScope.tailoredItems(
    state: TailoredUiState.Ready,
    tab: TailoredTab,
    onTabChange: (TailoredTab) -> Unit,
    onUndo: (String) -> Unit,
) {
    item(key = "header") {
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(
                text = stringResource(R.string.feature_tailor_impl_result_tailored_title),
                style = TmrTheme.typography.headline,
                color = TmrTheme.colors.text,
            )
            Text(
                text = stringResource(
                    R.string.feature_tailor_impl_result_tailored_subtitle,
                    state.jobTitle,
                    state.company,
                ),
                style = TmrTheme.typography.bodySmall,
                color = TmrTheme.colors.textMuted,
            )
        }
    }
    item(key = "tabs") {
        TailoredTabs(
            selected = tab,
            changeCount = state.changes.size,
            onSelect = onTabChange,
        )
    }
    if (tab == TailoredTab.Resume) {
        item(key = "resume") {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = stringResource(R.string.feature_tailor_impl_result_coverage, state.coveragePercent),
                    style = TmrTheme.typography.mono14,
                    color = TmrTheme.colors.lime,
                )
                TmrResumePaper(
                    name = state.name,
                    contact = state.contact,
                    blocks = state.blocks,
                    coveragePercent = state.coveragePercent,
                )
            }
        }
    } else if (state.changes.isEmpty()) {
        item(key = "no-changes") {
            Text(
                text = stringResource(R.string.feature_tailor_impl_result_no_changes),
                style = TmrTheme.typography.body,
                color = TmrTheme.colors.textMuted,
            )
        }
    } else {
        items(state.changes.size, key = { state.changes[it].id }) { index ->
            ChangeCardItem(card = state.changes[index], onUndo = onUndo)
        }
    }
}

@Composable
private fun TailoredFooter(
    state: TailoredUiState.Ready,
    onAcceptChanges: () -> Unit,
    onEdit: () -> Unit,
    onExport: () -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        if (state.accepted) {
            AcceptedRow()
        } else {
            TmrSecondaryButton(
                label = stringResource(R.string.feature_tailor_impl_result_accept),
                onClick = onAcceptChanges,
                modifier = Modifier.fillMaxWidth(),
            )
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            TmrSecondaryButton(
                label = stringResource(R.string.feature_tailor_impl_result_edit),
                onClick = onEdit,
                modifier = Modifier.weight(1f),
            )
            TmrPrimaryButton(
                label = stringResource(R.string.feature_tailor_impl_result_export),
                onClick = onExport,
                modifier = Modifier.weight(1f),
                enabled = state.exportEnabled,
                onDisabledClick = onExport,
            )
        }
    }
}

@Composable
private fun AcceptedRow(modifier: Modifier = Modifier) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(10.dp)
                .clip(CircleShape)
                .background(TmrTheme.colors.lime),
        )
        Text(
            text = stringResource(R.string.feature_tailor_impl_result_accepted),
            style = TmrTheme.typography.body,
            color = TmrTheme.colors.textMuted,
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ChangeCardItem(card: ChangeCard, onUndo: (String) -> Unit) {
    val colors = TmrTheme.colors
    TmrCard {
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = card.area.areaLabel(),
                style = TmrTheme.typography.label,
                color = colors.textMuted,
            )
            TmrTagChip(tag = card.kind.tag())
            TmrSourceChip(source = card.source.source())
        }
        val before = card.before
        if (before != null) {
            Text(
                text = before,
                style = TmrTheme.typography.body,
                color = colors.textMuted,
                textDecoration = TextDecoration.LineThrough,
            )
        }
        Text(
            text = card.after,
            style = TmrTheme.typography.body,
            color = colors.text,
        )
        if (card.undone) {
            Text(
                text = stringResource(R.string.feature_tailor_impl_result_undone),
                style = TmrTheme.typography.label,
                color = colors.textMuted,
            )
        } else {
            TmrCompactButton(
                label = stringResource(R.string.feature_tailor_impl_result_undo),
                onClick = { onUndo(card.id) },
                size = TmrButtonSize.Compact,
            )
        }
    }
}

@Composable
private fun ChangeArea.areaLabel(): String = when (this) {
    ChangeArea.Summary -> stringResource(R.string.feature_tailor_impl_result_area_summary)
    ChangeArea.Skills -> stringResource(R.string.feature_tailor_impl_result_area_skills)
    is ChangeArea.Entry -> label
}

private fun ChangeKind.tag(): TmrTag = when (this) {
    ChangeKind.New -> TmrTag.New
    ChangeKind.Rewritten -> TmrTag.Rewritten
    ChangeKind.Added -> TmrTag.Added
    ChangeKind.Reordered -> TmrTag.Reordered
}

private fun ChangeSource.source(): TmrSource = when (this) {
    ChangeSource.YourResume -> TmrSource.YourResume
    ChangeSource.YourAnswer -> TmrSource.YourAnswer
}
