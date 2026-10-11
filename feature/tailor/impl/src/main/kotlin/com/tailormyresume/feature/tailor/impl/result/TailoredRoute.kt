package com.tailormyresume.feature.tailor.impl.result

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation3.runtime.NavKey
import com.tailormyresume.core.designsystem.component.chrome.LocalTmrToast
import com.tailormyresume.feature.tailor.impl.R
import com.tailormyresume.feature.tailor.impl.TailorEvent
import com.tailormyresume.feature.tailor.impl.TailorViewModel

@Composable
internal fun TailoredRoute(
    tailorViewModel: TailorViewModel,
    tailoredViewModel: TailoredViewModel,
    onNavigate: (NavKey) -> Unit,
) {
    val state by tailoredViewModel.state.collectAsStateWithLifecycle()
    var tab by rememberSaveable { mutableStateOf(TailoredTab.Resume) }
    val toast = LocalTmrToast.current
    val exportBlocked = stringResource(R.string.feature_tailor_impl_result_export_blocked)
    LaunchedEffect(tailorViewModel) {
        tailorViewModel.events.collect { event ->
            when (event) {
                is TailorEvent.Navigate -> onNavigate(event.key)
                TailorEvent.ExportBlocked -> toast.show(exportBlocked)
            }
        }
    }
    (state as? TailoredUiState.Ready)?.let { ready ->
        TailoredScreen(
            state = ready,
            tab = tab,
            onTabChange = { tab = it },
            onUndo = tailorViewModel::onUndoChange,
            onAcceptChanges = tailorViewModel::onAcceptChanges,
            onEdit = tailorViewModel::onEditTapped,
            onExport = tailorViewModel::onExportTapped,
        )
    }
}
