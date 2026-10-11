package com.tailormyresume.feature.tailor.impl.exported

import android.content.ActivityNotFoundException
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.tailormyresume.core.designsystem.component.chrome.LocalTmrToast
import com.tailormyresume.core.designsystem.component.chrome.TmrToastAction
import com.tailormyresume.core.designsystem.component.content.TmrApplicationStatus
import com.tailormyresume.core.model.ApplicationStatus
import com.tailormyresume.feature.tailor.impl.R
import com.tailormyresume.feature.tailor.impl.export.createPdfShareIntent
import com.tailormyresume.feature.tailor.impl.export.createPdfViewIntent

@Composable
internal fun ExportedRoute(
    viewModel: ExportedViewModel,
    onGoToApplications: () -> Unit,
    onBack: () -> Unit,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val toast = LocalTmrToast.current
    val comingSoon = stringResource(R.string.feature_tailor_impl_exported_coming_soon)
    val fileMissing = stringResource(R.string.feature_tailor_impl_exported_file_missing)
    val openFailed = stringResource(R.string.feature_tailor_impl_exported_open_failed)
    val undo = stringResource(R.string.feature_tailor_impl_exported_undo)
    val shareSubject = stringResource(R.string.feature_tailor_impl_exported_share_subject)
    val shareTitle = stringResource(R.string.feature_tailor_impl_exported_share_title)
    val notAccepted = stringResource(R.string.feature_tailor_impl_result_export_blocked)
    val exportFailed = stringResource(R.string.feature_tailor_impl_exported_export_failed)
    LaunchedEffect(viewModel) {
        viewModel.events.collect { event ->
            when (event) {
                is ExportedEvent.Share -> context.startActivity(
                    createPdfShareIntent(context, event.file, shareSubject, shareTitle),
                )
                is ExportedEvent.Open -> try {
                    context.startActivity(createPdfViewIntent(context, event.file))
                } catch (_: ActivityNotFoundException) {
                    toast.show(openFailed)
                }
                ExportedEvent.FileMissing -> toast.show(fileMissing)
                ExportedEvent.ComingSoon -> toast.show(comingSoon)
                is ExportedEvent.MarkedApplied -> toast.show(
                    message = context.getString(R.string.feature_tailor_impl_exported_applied_toast, event.on),
                    action = TmrToastAction(undo, viewModel::onUndoApplied),
                )
                is ExportedEvent.StatusSet -> toast.show(
                    context.getString(
                        R.string.feature_tailor_impl_exported_status_set,
                        context.getString(event.status.toTmr().labelRes),
                    ),
                )
            }
        }
    }
    LaunchedEffect(state) {
        when (state) {
            ExportedUiState.NothingExported -> {
                toast.show(notAccepted)
                onBack()
            }
            ExportedUiState.ExportFailed -> {
                toast.show(exportFailed)
                onBack()
            }
            else -> Unit
        }
    }
    (state as? ExportedUiState.Ready)?.let { ready ->
        ExportedScreen(
            state = ready,
            onShare = viewModel::onShare,
            onOpen = viewModel::onOpen,
            onMarkApplied = viewModel::onMarkApplied,
            onChangeStatus = viewModel::onChangeStatus,
            onPickStatus = viewModel::onPickStatus,
            onDismissStatusSheet = viewModel::onDismissStatusSheet,
            onSaveStatus = viewModel::onSaveStatus,
            onSoon = viewModel::onSoon,
            onGoToApplications = onGoToApplications,
        )
    }
}

private fun ApplicationStatus.toTmr(): TmrApplicationStatus = when (this) {
    ApplicationStatus.SAVED -> TmrApplicationStatus.Saved
    ApplicationStatus.APPLIED -> TmrApplicationStatus.Applied
    ApplicationStatus.INTERVIEW -> TmrApplicationStatus.Interview
    ApplicationStatus.OFFER -> TmrApplicationStatus.Offer
    ApplicationStatus.REJECTED -> TmrApplicationStatus.Rejected
}
