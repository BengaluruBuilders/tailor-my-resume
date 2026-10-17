package com.tailormyresume.feature.settings.impl.credits

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.tailormyresume.core.navigation.Navigator
import com.tailormyresume.feature.settings.api.navigation.navigateToPaywall

@Composable
internal fun CreditsRoute(
    navigator: Navigator,
    modifier: Modifier = Modifier,
    viewModel: CreditsViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val onBuyMore = remember(navigator) { { navigator.navigateToPaywall(returnToApplicationId = null) } }
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    DisposableEffect(viewModel, lifecycle) {
        val observer = LifecycleEventObserver { _, event -> if (event == Lifecycle.Event.ON_RESUME) viewModel.onResume() }
        lifecycle.addObserver(observer)
        onDispose { lifecycle.removeObserver(observer) }
    }
    CreditsScreen(state = state, onBuyMore = onBuyMore, modifier = modifier, onCheckTrial = viewModel::onCheckTrial, onClaimTrial = viewModel::onClaimTrial, onGoogle = viewModel::onGoogleAccount)
}
