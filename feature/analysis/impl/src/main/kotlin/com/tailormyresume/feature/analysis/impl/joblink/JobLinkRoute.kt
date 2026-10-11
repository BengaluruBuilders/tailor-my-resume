package com.tailormyresume.feature.analysis.impl.joblink

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.tailormyresume.core.navigation.Navigator

@Composable
internal fun JobLinkRoute(
    navigator: Navigator,
    modifier: Modifier = Modifier,
    viewModel: JobLinkViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val goBack by rememberUpdatedState(navigator::goBack)
    LaunchedEffect(viewModel) {
        viewModel.events.collect { goBack() }
    }
    JobLinkScreen(
        state = state,
        onLinkChange = viewModel::onLinkChange,
        onImport = viewModel::onImport,
        modifier = modifier,
    )
}
