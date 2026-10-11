package com.tailormyresume.feature.applications.impl.detail

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.tailormyresume.core.designsystem.component.TmrButtonSize
import com.tailormyresume.core.designsystem.component.TmrWholeWordText
import com.tailormyresume.core.designsystem.component.chrome.TmrStatusSheet
import com.tailormyresume.core.designsystem.component.content.TmrCard
import com.tailormyresume.core.designsystem.component.content.TmrFileCard
import com.tailormyresume.core.designsystem.component.content.TmrListRow
import com.tailormyresume.core.designsystem.component.content.TmrSectionLabel
import com.tailormyresume.core.designsystem.component.content.TmrSoonRow
import com.tailormyresume.core.designsystem.component.content.TmrStatusChip
import com.tailormyresume.core.designsystem.theme.TmrTheme
import com.tailormyresume.core.model.ApplicationStatus
import com.tailormyresume.feature.applications.impl.R
import com.tailormyresume.feature.applications.impl.toApplicationStatus
import com.tailormyresume.feature.applications.impl.toTmrStatus
import com.tailormyresume.core.designsystem.component.TmrSecondaryButton as TmrCompactButton

@Composable
internal fun ApplicationDetailScreen(
    state: ApplicationDetailUiState.Ready,
    onChangeStatus: () -> Unit,
    onPickStatus: (ApplicationStatus) -> Unit,
    onDismissStatusSheet: () -> Unit,
    onSaveStatus: () -> Unit,
    onShare: () -> Unit,
    onOpen: () -> Unit,
    onRetailor: () -> Unit,
    onViewJobAnalysis: () -> Unit,
    onSoon: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(modifier = modifier.fillMaxSize().background(TmrTheme.colors.background)) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp)
                .padding(top = 12.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Column(
                modifier = Modifier.padding(horizontal = 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                TmrWholeWordText(
                    text = state.title,
                    style = TmrTheme.typography.headline,
                    color = TmrTheme.colors.text,
                    modifier = Modifier.semantics { heading() },
                )
                TmrWholeWordText(
                    text = if (state.place.isBlank()) {
                        state.company
                    } else {
                        stringResource(
                            R.string.feature_applications_impl_company_place,
                            state.company,
                            state.place,
                        )
                    },
                    style = TmrTheme.typography.bodySmall,
                    color = TmrTheme.colors.textMuted,
                )
            }
            StatusCard(state, onChangeStatus)
            val file = state.file
            if (file != null) {
                TmrFileCard(
                    fileName = file.fileName,
                    meta = if (file.keywordPercent != null) {
                        stringResource(
                            R.string.feature_applications_impl_file_meta_keywords,
                            file.keywordPercent,
                        )
                    } else {
                        stringResource(R.string.feature_applications_impl_file_meta_pdf)
                    },
                    onShare = onShare,
                    onOpen = onOpen,
                )
            }
            TmrSectionLabel(
                text = stringResource(R.string.feature_applications_impl_next_steps),
                modifier = Modifier.padding(horizontal = 8.dp).padding(top = 6.dp),
            )
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(TmrTheme.colors.surfaceRaised, TmrTheme.shapes.card)
                    .padding(horizontal = 16.dp),
            ) {
                TmrListRow(
                    label = stringResource(R.string.feature_applications_impl_retailor),
                    meta = stringResource(R.string.feature_applications_impl_retailor_cost),
                    onClick = onRetailor,
                    showChevron = false,
                    minHeight = 56.dp,
                )
                TmrListRow(
                    label = stringResource(R.string.feature_applications_impl_view_job),
                    onClick = onViewJobAnalysis,
                    minHeight = 56.dp,
                )
                TmrSoonRow(
                    label = stringResource(R.string.feature_applications_impl_prep_questions),
                    onClick = onSoon,
                )
                TmrSoonRow(
                    label = stringResource(R.string.feature_applications_impl_cover_letter),
                    onClick = onSoon,
                    showDivider = false,
                )
            }
        }
        val statusSheet = state.statusSheet
        if (statusSheet != null) {
            TmrStatusSheet(
                selected = statusSheet.toTmrStatus(),
                onSelect = { onPickStatus(it.toApplicationStatus()) },
                onSave = onSaveStatus,
                onDismiss = onDismissStatusSheet,
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun StatusCard(
    state: ApplicationDetailUiState.Ready,
    onChangeStatus: () -> Unit,
) {
    TmrCard {
        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            itemVerticalAlignment = Alignment.CenterVertically,
        ) {
            Column(
                modifier = Modifier.weight(1f, fill = false),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                TmrSectionLabel(text = stringResource(R.string.feature_applications_impl_status_label))
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    TmrStatusChip(status = state.status.toTmrStatus())
                    Text(
                        text = if (state.appliedOn != null) {
                            stringResource(R.string.feature_applications_impl_applied_on, state.appliedOn)
                        } else {
                            stringResource(R.string.feature_applications_impl_not_applied)
                        },
                        style = TmrTheme.typography.bodySmall,
                        color = TmrTheme.colors.textMuted,
                    )
                }
            }
            TmrCompactButton(
                label = stringResource(R.string.feature_applications_impl_change),
                onClick = onChangeStatus,
                size = TmrButtonSize.Compact,
            )
        }
    }
}
