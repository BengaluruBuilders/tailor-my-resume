package com.tailormyresume.feature.applications.impl

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.tailormyresume.core.designsystem.component.TmrInkButton
import com.tailormyresume.core.designsystem.component.TmrWholeWordText
import com.tailormyresume.core.designsystem.component.content.TmrCard
import com.tailormyresume.core.designsystem.component.content.TmrInitialDisc
import com.tailormyresume.core.designsystem.component.content.TmrSectionLabel
import com.tailormyresume.core.designsystem.component.content.TmrStatusBar
import com.tailormyresume.core.designsystem.component.content.TmrStatusChip
import com.tailormyresume.core.designsystem.component.hero.TmrPaige
import com.tailormyresume.core.designsystem.component.hero.TmrPaigePose
import com.tailormyresume.core.designsystem.theme.TmrTheme
import com.tailormyresume.feature.applications.impl.R

@Composable
internal fun ApplicationsScreen(
    state: ApplicationsUiState,
    onCredits: () -> Unit,
    onAvatar: () -> Unit,
    onAddJob: () -> Unit,
    onOpenApplication: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    if (state is ApplicationsUiState.Loading) return
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(TmrTheme.colors.background)
            .padding(horizontal = 16.dp),
    ) {
        when (state) {
            is ApplicationsUiState.Loading -> Unit
            is ApplicationsUiState.Empty -> EmptyState(state.header, onCredits, onAvatar, onAddJob)
            is ApplicationsUiState.Populated -> PopulatedState(state, onCredits, onAvatar, onOpenApplication)
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ApplicationsHeaderRow(
    header: ApplicationsHeader,
    onCredits: () -> Unit,
    onAvatar: () -> Unit,
) {
    FlowRow(
        modifier = Modifier
            .fillMaxWidth()
            .windowInsetsPadding(WindowInsets.statusBars),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        horizontalArrangement = Arrangement.spacedBy(HEADER_GAP),
        itemVerticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = stringResource(R.string.feature_applications_impl_title),
            style = TmrTheme.typography.title,
            color = TmrTheme.colors.text,
            modifier = Modifier
                .weight(1f, fill = false)
                .semantics { heading() },
        )
        Row(horizontalArrangement = Arrangement.spacedBy(HEADER_GAP)) {
            CreditsPill(header.credits, onCredits)
            AvatarDisc(header.initials, onAvatar)
        }
    }
}

private val HEADER_GAP = 6.dp

@Composable
private fun CreditsPill(credits: Int?, onCredits: () -> Unit) {
    Row(
        modifier = Modifier
            .clip(TmrTheme.shapes.pill)
            .background(TmrTheme.colors.fill)
            .clickable(role = Role.Button, onClick = onCredits)
            .heightIn(min = 48.dp)
            .padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = if (credits == null) {
                stringResource(R.string.feature_applications_impl_credits_unknown)
            } else {
                pluralStringResource(R.plurals.feature_applications_impl_credits, credits, credits)
            },
            style = TmrTheme.typography.caption,
            color = TmrTheme.colors.text,
        )
    }
}

@Composable
private fun AvatarDisc(initials: String, onAvatar: () -> Unit) {
    val profileLabel = stringResource(R.string.feature_applications_impl_profile)
    Box(
        modifier = Modifier
            .size(48.dp)
            .clip(CircleShape)
            .background(TmrTheme.colors.lime)
            .clickable(role = Role.Button, onClick = onAvatar)
            .semantics { contentDescription = profileLabel },
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = initials,
            style = TmrTheme.typography.mono14.copy(fontWeight = FontWeight.Bold),
            color = TmrTheme.colors.ink,
        )
    }
}

@Composable
private fun EmptyState(
    header: ApplicationsHeader,
    onCredits: () -> Unit,
    onAvatar: () -> Unit,
    onAddJob: () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        ApplicationsHeaderRow(header, onCredits, onAvatar)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 400.dp)
                .clip(TmrTheme.shapes.hero)
                .background(TmrTheme.colors.lime),
        ) {
            Column(
                modifier = Modifier
                    .padding(24.dp)
                    .padding(bottom = 154.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Text(
                    text = stringResource(R.string.feature_applications_impl_empty_title),
                    style = TmrTheme.typography.headline,
                    color = TmrTheme.colors.ink,
                )
                Text(
                    text = stringResource(R.string.feature_applications_impl_empty_body),
                    style = TmrTheme.typography.body,
                    color = TmrTheme.colors.ink,
                    modifier = Modifier.widthIn(max = 260.dp),
                )
                TmrInkButton(
                    label = stringResource(R.string.feature_applications_impl_add_job),
                    onClick = onAddJob,
                )
            }
            TmrPaige(
                pose = TmrPaigePose.Home,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .offset(y = 30.dp),
            )
        }
    }
}

@Composable
private fun PopulatedState(
    state: ApplicationsUiState.Populated,
    onCredits: () -> Unit,
    onAvatar: () -> Unit,
    onOpenApplication: (String) -> Unit,
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(14.dp),
        contentPadding = PaddingValues(bottom = 24.dp),
    ) {
        item(key = "header") {
            ApplicationsHeaderRow(state.header, onCredits, onAvatar)
        }
        item(key = "summary") { SummaryCard(state) }
        item(key = "recent") {
            TmrSectionLabel(text = stringResource(R.string.feature_applications_impl_recent_label))
        }
        items(items = state.rows, key = { it.id }) { row ->
            ApplicationRowItem(row = row, onClick = { onOpenApplication(row.id) })
        }
    }
}

@Composable
private fun SummaryCard(state: ApplicationsUiState.Populated) {
    TmrCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            TmrSectionLabel(
                text = stringResource(R.string.feature_applications_impl_total_label),
                modifier = Modifier.weight(1f),
            )
            Text(
                text = state.total.toString(),
                style = TmrTheme.typography.title,
                color = TmrTheme.colors.text,
            )
        }
        TmrStatusBar(counts = state.statusCounts)
    }
}

@Composable
private fun ApplicationRowItem(row: ApplicationRow, onClick: () -> Unit) {
    val statusLabel = stringResource(row.status.labelRes)
    val meta = row.keywordPercent?.let { percent ->
        stringResource(R.string.feature_applications_impl_row_meta_keywords, row.company, percent)
    } ?: row.company
    val stacked = LocalDensity.current.fontScale > CHIP_STACK_FONT_SCALE
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(TmrTheme.shapes.card)
            .clickable(role = Role.Button, onClick = onClick)
            .heightIn(min = 64.dp)
            .padding(vertical = 10.dp, horizontal = 4.dp),
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            TmrInitialDisc(initial = row.initial, color = row.discColor())
            Column(
                modifier = Modifier
                    .weight(1f)
                    .align(if (stacked) Alignment.Top else Alignment.CenterVertically),
                verticalArrangement = Arrangement.spacedBy(3.dp),
            ) {
                TmrWholeWordText(
                    text = row.title,
                    style = TmrTheme.typography.strongLarge,
                    color = TmrTheme.colors.text,
                )
                TmrWholeWordText(
                    text = meta,
                    style = TmrTheme.typography.bodySmall,
                    color = TmrTheme.colors.textMuted,
                )
                if (stacked) {
                    TmrStatusChip(status = row.status)
                }
            }
            if (!stacked) {
                TmrStatusChip(status = row.status, modifier = Modifier.align(Alignment.Top))
            }
        }
    }
}

private const val CHIP_STACK_FONT_SCALE = 1.3f

@Composable
private fun ApplicationRow.discColor(): Color =
    when (Math.floorMod(colorIndex, DISC_COLOR_COUNT)) {
        0 -> TmrTheme.colors.amber
        1 -> TmrTheme.colors.lime
        else -> TmrTheme.colors.blue
    }
