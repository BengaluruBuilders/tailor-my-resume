package com.tailormyresume.feature.settings.impl.credits

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.tailormyresume.core.navigation.Navigator

@Composable
internal fun CreditsRoute(
    navigator: Navigator,
    modifier: Modifier = Modifier,
    viewModel: CreditsViewModel = hiltViewModel(),
) = Unit
