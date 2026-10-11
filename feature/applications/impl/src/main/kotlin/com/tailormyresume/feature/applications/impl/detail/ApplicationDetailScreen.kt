package com.tailormyresume.feature.applications.impl.detail

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.tailormyresume.core.model.ApplicationStatus

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
) = Unit
