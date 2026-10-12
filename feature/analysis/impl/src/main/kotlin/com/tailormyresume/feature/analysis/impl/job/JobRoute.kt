package com.tailormyresume.feature.analysis.impl.job

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.tailormyresume.core.navigation.Navigator
import com.tailormyresume.feature.analysis.api.navigation.JobLinkNavKey
import com.tailormyresume.feature.analysis.api.navigation.navigateToJobResult

@Composable
internal fun JobRoute(
    navigator: Navigator,
    modifier: Modifier = Modifier,
    viewModel: JobViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    JobEventHandler(
        events = viewModel.events,
        onAnalyzed = navigator::navigateToJobResult,
        onRetry = viewModel::onAnalyze,
    )
    JobScreen(
        state = state,
        onTextChange = viewModel::onTextChange,
        onUseLink = { navigator.navigate(JobLinkNavKey()) },
        onClear = viewModel::onClear,
        onAnalyze = viewModel::onAnalyze,
        modifier = modifier,
    )
}
