package com.tailormyresume.feature.settings.impl.paywall

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

internal class PaywallActions(
    val onSelectPack: (String) -> Unit,
    val onBuy: () -> Unit,
    val onRestore: () -> Unit,
)

@Composable
internal fun PaywallScreen(state: PaywallUiState, actions: PaywallActions, modifier: Modifier = Modifier) = Unit
