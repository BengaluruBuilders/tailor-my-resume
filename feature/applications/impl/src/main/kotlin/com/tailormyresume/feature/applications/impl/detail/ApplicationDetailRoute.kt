package com.tailormyresume.feature.applications.impl.detail

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.tailormyresume.core.designsystem.component.chrome.LocalTmrToast
import com.tailormyresume.core.navigation.Navigator
import com.tailormyresume.feature.analysis.api.navigation.navigateToJobResult
import com.tailormyresume.feature.analysis.api.navigation.navigateToQuickQuestion
import com.tailormyresume.feature.applications.impl.R
import com.tailormyresume.feature.applications.impl.toTmrStatus
import com.tailormyresume.feature.settings.api.navigation.navigateToPaywall
import com.tailormyresume.feature.tailor.api.navigation.navigateToTailored
import com.tailormyresume.feature.tailor.api.navigation.navigateToTailoring

@Composable
internal fun ApplicationDetailRoute(viewModel: ApplicationDetailViewModel, navigator: Navigator) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val toast = LocalTmrToast.current
    val balanceUnavailable = stringResource(R.string.feature_applications_impl_credits_unknown)
    val fileMissing = stringResource(R.string.feature_applications_impl_file_missing)
    val openFailed = stringResource(R.string.feature_applications_impl_open_failed)
    LaunchedEffect(viewModel) {
        viewModel.events.collect { event ->
            when (event) {
                is ApplicationDetailEvent.ShareFile -> context.startOrToast(event.intent) { toast.show(openFailed) }
                is ApplicationDetailEvent.OpenFile -> context.startOrToast(event.intent) { toast.show(openFailed) }
                ApplicationDetailEvent.FileMissing -> toast.show(fileMissing)
                ApplicationDetailEvent.BalanceUnavailable -> toast.show(balanceUnavailable)
                is ApplicationDetailEvent.StatusSet -> toast.show(
                    context.getString(
                        R.string.feature_applications_impl_status_set,
                        context.getString(event.status.toTmrStatus().labelRes),
                    ),
                )
                is ApplicationDetailEvent.OpenQuickQuestion -> navigator.navigateToQuickQuestion(event.applicationId)
                is ApplicationDetailEvent.StartTailoring -> navigator.navigateToTailoring(event.applicationId)
                is ApplicationDetailEvent.ReviewTailoredResume -> navigator.navigateToTailored(event.applicationId)
                is ApplicationDetailEvent.OpenPaywall -> navigator.navigateToPaywall(event.applicationId)
            }
        }
    }
    LaunchedEffect(state) {
        if (state == ApplicationDetailUiState.NotFound) navigator.goBack()
    }
    (state as? ApplicationDetailUiState.Ready)?.let { ready ->
        ApplicationDetailScreen(
            state = ready,
            onChangeStatus = viewModel::onChangeStatus,
            onPickStatus = viewModel::onPickStatus,
            onDismissStatusSheet = viewModel::onDismissStatusSheet,
            onSaveStatus = viewModel::onSaveStatus,
            onShare = viewModel::onShare,
            onOpen = viewModel::onOpen,
            onRetailor = viewModel::onRetailor,
            onReviewTailoredResume = viewModel::onReviewTailoredResume,
            onViewJobAnalysis = { navigator.navigateToJobResult(viewModel.applicationId) },
        )
    }
}

private fun Context.startOrToast(intent: Intent, onMissing: () -> Unit) {
    try {
        startActivity(intent)
    } catch (_: ActivityNotFoundException) {
        onMissing()
    }
}
