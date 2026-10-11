package com.tailormyresume.feature.analysis.impl.job

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.stringResource
import com.tailormyresume.core.designsystem.component.chrome.LocalTmrToast
import com.tailormyresume.core.designsystem.component.chrome.TmrToastAction
import com.tailormyresume.feature.analysis.impl.R
import kotlinx.coroutines.flow.Flow

@Composable
internal fun JobEventHandler(
    events: Flow<JobEvent>,
    onAnalyzed: (applicationId: String) -> Unit,
    onRetry: () -> Unit,
) {
    val toast = LocalTmrToast.current
    val resources = LocalResources.current
    val failedMessage = stringResource(R.string.feature_analysis_impl_job_failed)
    val retryLabel = stringResource(R.string.feature_analysis_impl_job_retry)
    val currentOnAnalyzed by rememberUpdatedState(onAnalyzed)
    val currentOnRetry by rememberUpdatedState(onRetry)
    LaunchedEffect(events) {
        events.collect { event ->
            when (event) {
                is JobEvent.Analyzed -> currentOnAnalyzed(event.applicationId)
                is JobEvent.Imported ->
                    toast.show(resources.getString(R.string.feature_analysis_impl_job_imported, event.host))
                JobEvent.AnalysisFailed ->
                    toast.show(failedMessage, TmrToastAction(retryLabel) { currentOnRetry() })
            }
        }
    }
}
