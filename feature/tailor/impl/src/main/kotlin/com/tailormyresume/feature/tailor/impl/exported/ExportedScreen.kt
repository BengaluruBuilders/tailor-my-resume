package com.tailormyresume.feature.tailor.impl.exported

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.tailormyresume.core.designsystem.component.TmrButtonSize
import com.tailormyresume.core.designsystem.component.chrome.TmrStatusSheet
import com.tailormyresume.core.designsystem.component.content.TmrApplicationStatus
import com.tailormyresume.core.designsystem.component.content.TmrCard
import com.tailormyresume.core.designsystem.component.content.TmrFileCard
import com.tailormyresume.core.designsystem.component.content.TmrSoonRow
import com.tailormyresume.core.designsystem.component.content.TmrStatusChip
import com.tailormyresume.core.designsystem.component.input.TmrPrimaryButton
import com.tailormyresume.core.designsystem.component.input.TmrSecondaryButton
import com.tailormyresume.core.designsystem.icon.TmrIcons
import com.tailormyresume.core.designsystem.theme.TmrTheme
import com.tailormyresume.core.model.ApplicationStatus
import com.tailormyresume.feature.tailor.impl.R
import com.tailormyresume.core.designsystem.component.TmrSecondaryButton as TmrCompactButton

@Composable
internal fun ExportedScreen(
    state: ExportedUiState.Ready,
    onShare: () -> Unit,
    onOpen: () -> Unit,
    onMarkApplied: () -> Unit,
    onChangeStatus: () -> Unit,
    onPickStatus: (ApplicationStatus) -> Unit,
    onDismissStatusSheet: () -> Unit,
    onSaveStatus: () -> Unit,
    onSoon: () -> Unit,
    onGoToApplications: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(modifier = modifier.fillMaxSize().background(TmrTheme.colors.background)) {
        Column(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp)
                    .padding(top = 12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                ExportedHeader(state)
                TmrFileCard(
                    fileName = state.fileName,
                    meta = pluralStringResource(
                        R.plurals.feature_tailor_impl_exported_file_meta,
                        state.pageCount,
                        state.pageCount,
                        state.sizeKb,
                    ),
                    onShare = onShare,
                    onOpen = onOpen,
                )
                CreditsCard(state)
                Text(
                    text = stringResource(
                        R.string.feature_tailor_impl_exported_saved_to,
                        state.jobTitle,
                        state.company,
                    ),
                    style = TmrTheme.typography.bodySmall,
                    color = TmrTheme.colors.textMuted,
                )
                StatusCard(state, onMarkApplied, onChangeStatus)
                SoonCard(onSoon)
            }
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 24.dp),
            ) {
                TmrSecondaryButton(
                    label = stringResource(R.string.feature_tailor_impl_exported_go_to_applications),
                    onClick = onGoToApplications,
                    modifier = Modifier.fillMaxWidth(),
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

@Composable
private fun ExportedHeader(state: ExportedUiState.Ready) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier.size(48.dp).clip(CircleShape).background(TmrTheme.colors.lime),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = TmrIcons.Check,
                contentDescription = null,
                modifier = Modifier.size(20.dp),
                tint = TmrTheme.colors.ink,
            )
        }
        Text(
            text = stringResource(R.string.feature_tailor_impl_exported_title),
            style = TmrTheme.typography.headline,
            color = TmrTheme.colors.text,
            modifier = Modifier.weight(1f).semantics { heading() },
        )
    }
}

@Composable
private fun CreditsCard(state: ExportedUiState.Ready) {
    TmrCard {
        Row(
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = state.creditsLeft.toString(),
                style = TmrTheme.typography.display,
                color = TmrTheme.colors.text,
            )
            val line = if (state.freeResumeUsed) {
                stringResource(
                    if (state.creditsLeft == 0) {
                        R.string.feature_tailor_impl_exported_credits_free
                    } else {
                        R.string.feature_tailor_impl_exported_credits_free_with_balance
                    },
                )
            } else {
                pluralStringResource(
                    R.plurals.feature_tailor_impl_exported_credits_paid,
                    state.creditsLeft,
                    state.creditsLeft,
                    state.creditsLeft + 1,
                )
            }
            Text(
                text = line,
                style = TmrTheme.typography.body,
                color = TmrTheme.colors.text,
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun StatusCard(
    state: ExportedUiState.Ready,
    onMarkApplied: () -> Unit,
    onChangeStatus: () -> Unit,
) {
    TmrCard {
        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            itemVerticalAlignment = Alignment.CenterVertically,
        ) {
            if (state.status == ApplicationStatus.SAVED) {
                Text(
                    text = stringResource(R.string.feature_tailor_impl_exported_did_you_apply),
                    style = TmrTheme.typography.strongLarge,
                    color = TmrTheme.colors.text,
                )
                TmrPrimaryButton(
                    label = stringResource(R.string.feature_tailor_impl_exported_mark_applied),
                    onClick = onMarkApplied,
                )
            } else {
                TmrStatusChip(status = state.status.toTmrStatus())
                val markedOn = state.markedOn
                if (markedOn != null) {
                    Text(
                        text = stringResource(R.string.feature_tailor_impl_exported_marked_on, markedOn),
                        style = TmrTheme.typography.bodySmall,
                        color = TmrTheme.colors.textMuted,
                    )
                }
                TmrCompactButton(
                    label = stringResource(R.string.feature_tailor_impl_exported_change),
                    onClick = onChangeStatus,
                    size = TmrButtonSize.Compact,
                )
            }
        }
    }
}

@Composable
private fun SoonCard(onSoon: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(TmrTheme.colors.surfaceRaised, TmrTheme.shapes.card)
            .padding(horizontal = 16.dp),
    ) {
        TmrSoonRow(
            label = stringResource(R.string.feature_tailor_impl_exported_prep_questions),
            onClick = onSoon,
        )
        TmrSoonRow(
            label = stringResource(R.string.feature_tailor_impl_exported_cover_letter),
            onClick = onSoon,
            showDivider = false,
        )
    }
}

private fun ApplicationStatus.toTmrStatus(): TmrApplicationStatus = when (this) {
    ApplicationStatus.SAVED -> TmrApplicationStatus.Saved
    ApplicationStatus.APPLIED -> TmrApplicationStatus.Applied
    ApplicationStatus.INTERVIEW -> TmrApplicationStatus.Interview
    ApplicationStatus.OFFER -> TmrApplicationStatus.Offer
    ApplicationStatus.REJECTED -> TmrApplicationStatus.Rejected
}

private fun TmrApplicationStatus.toApplicationStatus(): ApplicationStatus = when (this) {
    TmrApplicationStatus.Saved -> ApplicationStatus.SAVED
    TmrApplicationStatus.Applied -> ApplicationStatus.APPLIED
    TmrApplicationStatus.Interview -> ApplicationStatus.INTERVIEW
    TmrApplicationStatus.Offer -> ApplicationStatus.OFFER
    TmrApplicationStatus.Rejected -> ApplicationStatus.REJECTED
}
