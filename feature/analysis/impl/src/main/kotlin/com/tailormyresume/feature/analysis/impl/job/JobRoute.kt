package com.tailormyresume.feature.analysis.impl.job

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboard
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.tailormyresume.core.navigation.Navigator
import com.tailormyresume.feature.analysis.api.navigation.JobLinkNavKey
import com.tailormyresume.feature.analysis.api.navigation.navigateToJobResult
import kotlinx.coroutines.launch

@Composable
internal fun JobRoute(
    navigator: Navigator,
    modifier: Modifier = Modifier,
    viewModel: JobViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val clipboard = LocalClipboard.current
    val scope = rememberCoroutineScope()
    JobEventHandler(
        events = viewModel.events,
        onAnalyzed = navigator::navigateToJobResult,
        onRetry = viewModel::onAnalyze,
    )
    JobScreen(
        state = state,
        onTextChange = viewModel::onTextChange,
        onPaste = {
            scope.launch {
                val clip = clipboard.getClipEntry()?.clipData
                val pasted = clip?.takeIf { it.itemCount > 0 }?.getItemAt(0)?.text?.toString()
                if (pasted != null) viewModel.onPaste(pasted)
            }
        },
        onUseLink = { navigator.navigate(JobLinkNavKey()) },
        onClear = viewModel::onClear,
        onAnalyze = viewModel::onAnalyze,
        modifier = modifier,
    )
}
