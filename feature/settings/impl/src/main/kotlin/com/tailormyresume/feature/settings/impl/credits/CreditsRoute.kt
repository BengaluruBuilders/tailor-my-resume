package com.tailormyresume.feature.settings.impl.credits

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
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
    CreditsScreen(state = state, onBuyMore = onBuyMore, modifier = modifier)
}
