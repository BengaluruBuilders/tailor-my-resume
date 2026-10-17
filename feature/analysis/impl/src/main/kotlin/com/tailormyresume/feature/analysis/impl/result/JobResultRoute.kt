package com.tailormyresume.feature.analysis.impl.result

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.tailormyresume.core.designsystem.component.chrome.LocalTmrToast
import com.tailormyresume.core.navigation.Navigator
import com.tailormyresume.feature.analysis.impl.R

@Composable
internal fun JobResultRoute(
    navigator: Navigator,
    viewModel: JobResultViewModel,
    modifier: Modifier = Modifier,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val toast = LocalTmrToast.current
    val creditCheckFailed = stringResource(R.string.feature_analysis_impl_result_credit_check_failed)
    val navigation = remember(navigator) { JobResultNavigation(navigator) }
    LaunchedEffect(viewModel) {
        viewModel.onShown()
        viewModel.events.collect { event ->
            when (event) {
                JobResultEvent.CreditCheckFailed -> toast.show(creditCheckFailed)
                else -> navigation.handle(event)
            }
        }
    }
    JobResultScreen(state = state, onTailor = viewModel::onTailor, modifier = modifier)
}
