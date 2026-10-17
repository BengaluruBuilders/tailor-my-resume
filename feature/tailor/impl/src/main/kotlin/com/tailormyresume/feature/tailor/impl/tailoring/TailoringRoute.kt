package com.tailormyresume.feature.tailor.impl.tailoring

import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@Composable
internal fun TailoringRoute(
    viewModel: TailoringViewModel,
    onDone: () -> Unit,
    onFailed: () -> Unit,
    onNoCredit: () -> Unit,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val outcome by viewModel.outcome.collectAsStateWithLifecycle()
    BackHandler(enabled = true) {}
    LaunchedEffect(outcome) {
        when (outcome) {
            TailoringOutcome.Done -> onDone()
            TailoringOutcome.Failed -> onFailed()
            TailoringOutcome.NoCredit -> onNoCredit()
            null -> Unit
        }
    }
    TailoringScreen(state)
}
