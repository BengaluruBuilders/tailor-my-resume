package com.tailormyresume.app.debug

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.tailormyresume.app.R
import com.tailormyresume.core.designsystem.component.TmrCard
import com.tailormyresume.core.designsystem.component.TmrFilterChip
import com.tailormyresume.core.designsystem.component.TmrInnerHeader
import com.tailormyresume.core.designsystem.component.TmrOutlineButton
import com.tailormyresume.core.designsystem.component.TmrPrimaryButton
import com.tailormyresume.core.designsystem.component.TmrScreen
import com.tailormyresume.core.designsystem.theme.TmrTheme
import com.tailormyresume.core.model.DebugScenario

data class DebugMenuActions(
    val onLoadSampleData: () -> Unit,
    val onResetAppData: () -> Unit,
    val onOpenApp: () -> Unit,
    val onTargetSelected: (DebugScenarioTarget) -> Unit,
    val onScenarioSelected: (DebugScenario) -> Unit,
    val onOpenScreen: () -> Unit,
)

@Composable
fun DebugScenarioMenu(
    uiState: DebugMenuUiState,
    selectedTarget: DebugScenarioTarget,
    selectedScenario: DebugScenario,
    actions: DebugMenuActions,
    modifier: Modifier = Modifier,
) {
    TmrScreen(
        modifier = modifier,
        sheet = false,
        header = {
            TmrInnerHeader(
                title = stringResource(R.string.debug_scenario_title),
                subtitle = stringResource(R.string.debug_scenario_intro),
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(padding)
                .padding(horizontal = TmrTheme.spacing.gutter),
            verticalArrangement = Arrangement.spacedBy(TmrTheme.spacing.md),
        ) {
            DebugDataCard(uiState = uiState, actions = actions)
            DebugScenarioCard(
                selectedTarget = selectedTarget,
                selectedScenario = selectedScenario,
                actions = actions,
            )
        }
    }
}

@Composable
private fun DebugDataCard(
    uiState: DebugMenuUiState,
    actions: DebugMenuActions,
) {
    TmrCard(modifier = Modifier.fillMaxWidth()) {
        DebugHeading(R.string.debug_data_heading)
        val status = when {
            uiState.busy -> stringResource(R.string.debug_data_busy)
            uiState.message == DebugDataMessage.SampleLoaded -> stringResource(R.string.debug_data_loaded)
            uiState.message == DebugDataMessage.DataReset -> stringResource(R.string.debug_data_cleared)
            uiState.message == DebugDataMessage.Failed -> stringResource(R.string.debug_data_failed)
            else -> null
        }
        if (status != null) {
            Text(text = status, style = TmrTheme.typography.bodyM, color = TmrTheme.colors.onSurfaceVariant)
        }
        TmrPrimaryButton(
            label = stringResource(R.string.debug_data_load),
            onClick = actions.onLoadSampleData,
            modifier = Modifier.fillMaxWidth(),
            enabled = !uiState.busy,
        )
        TmrOutlineButton(
            label = stringResource(R.string.debug_data_reset),
            onClick = actions.onResetAppData,
            modifier = Modifier.fillMaxWidth(),
            enabled = !uiState.busy,
        )
        TmrOutlineButton(
            label = stringResource(R.string.debug_data_open_app),
            onClick = actions.onOpenApp,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable
private fun DebugScenarioCard(
    selectedTarget: DebugScenarioTarget,
    selectedScenario: DebugScenario,
    actions: DebugMenuActions,
) {
    TmrCard(modifier = Modifier.fillMaxWidth()) {
        DebugHeading(R.string.debug_scenario_destination_heading)
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(TmrTheme.spacing.sm),
            verticalArrangement = Arrangement.spacedBy(TmrTheme.spacing.sm),
        ) {
            DebugScenarioTarget.entries.forEach { target ->
                TmrFilterChip(
                    label = stringResource(target.titleRes),
                    selected = target == selectedTarget,
                    onClick = { actions.onTargetSelected(target) },
                )
            }
        }
        DebugHeading(R.string.debug_scenario_state_heading)
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(TmrTheme.spacing.sm),
            verticalArrangement = Arrangement.spacedBy(TmrTheme.spacing.sm),
        ) {
            DebugScenario.entries.forEach { scenario ->
                TmrFilterChip(
                    label = scenario.name.lowercase().replace('_', ' ').replaceFirstChar { it.uppercase() },
                    selected = scenario == selectedScenario,
                    onClick = { actions.onScenarioSelected(scenario) },
                )
            }
        }
        TmrPrimaryButton(
            label = stringResource(R.string.debug_scenario_open),
            onClick = actions.onOpenScreen,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable
private fun DebugHeading(titleRes: Int) {
    Text(
        text = stringResource(titleRes),
        style = TmrTheme.typography.titleM,
        color = TmrTheme.colors.onSurface,
    )
}
