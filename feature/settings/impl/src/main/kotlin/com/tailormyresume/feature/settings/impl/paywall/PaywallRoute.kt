package com.tailormyresume.feature.settings.impl.paywall

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.tailormyresume.core.navigation.Navigator

@Composable
internal fun PaywallRoute(
    navigator: Navigator,
    returnToApplicationId: String?,
    modifier: Modifier = Modifier,
    viewModel: PaywallViewModel = hiltViewModel(),
) = Unit
