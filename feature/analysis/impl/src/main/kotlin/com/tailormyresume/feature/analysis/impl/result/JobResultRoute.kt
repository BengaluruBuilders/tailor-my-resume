package com.tailormyresume.feature.analysis.impl.result

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.tailormyresume.core.navigation.Navigator

@Composable
internal fun JobResultRoute(
    navigator: Navigator,
    viewModel: JobResultViewModel,
    modifier: Modifier = Modifier,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val navigation = remember(navigator) { JobResultNavigation(navigator) }
    LaunchedEffect(viewModel) {
        viewModel.onShown()
        viewModel.events.collect(navigation::handle)
    }
    JobResultScreen(state = state, onTailor = viewModel::onTailor, modifier = modifier)
}
