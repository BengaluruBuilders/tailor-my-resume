package com.tailormyresume.feature.applications.impl.detail

import android.content.Intent
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tailormyresume.core.data.repository.ApplicationRepository
import com.tailormyresume.core.data.repository.CreditsRepository
import com.tailormyresume.core.domain.SetApplicationStatusUseCase
import com.tailormyresume.core.model.ApplicationStatus
import com.tailormyresume.core.model.JobApplication
import com.tailormyresume.feature.applications.impl.keywordPercent
import com.tailormyresume.feature.tailor.api.ExportedPdfIntents
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.getAndUpdate
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.time.Instant as KotlinInstant

@HiltViewModel(assistedFactory = ApplicationDetailViewModel.Factory::class)
internal class ApplicationDetailViewModel @AssistedInject constructor(
    private val applicationRepository: ApplicationRepository,
    private val creditsRepository: CreditsRepository,
    private val setApplicationStatus: SetApplicationStatusUseCase,
    private val exportedPdfIntents: ExportedPdfIntents,
    @Assisted val applicationId: String,
) : ViewModel() {

    private val statusSheet = MutableStateFlow<ApplicationStatus?>(null)

    private val eventChannel = Channel<ApplicationDetailEvent>(Channel.BUFFERED)

    val events: Flow<ApplicationDetailEvent> = eventChannel.receiveAsFlow()

    val state: StateFlow<ApplicationDetailUiState> = combine(
        applicationRepository.observeApplication(applicationId),
        statusSheet,
        ::stateFor,
    ).stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = ApplicationDetailUiState.Loading,
    )

    fun onShare() = sendIntent(exportedPdfIntents::shareIntent, ApplicationDetailEvent::ShareFile)

    fun onOpen() = sendIntent(exportedPdfIntents::viewIntent, ApplicationDetailEvent::OpenFile)

    fun onChangeStatus() {
        val ready = state.value as? ApplicationDetailUiState.Ready ?: return
        statusSheet.value = ready.status
    }

    fun onPickStatus(status: ApplicationStatus) {
        statusSheet.value = status
    }

    fun onDismissStatusSheet() {
        statusSheet.value = null
    }

    fun onSaveStatus() {
        val pick = statusSheet.getAndUpdate { null } ?: return
        viewModelScope.launch {
            setApplicationStatus(applicationId, pick)
            statusSheet.value = null
            eventChannel.send(ApplicationDetailEvent.StatusSet(pick))
        }
    }

    fun onRetailor() {
        viewModelScope.launch {
            val event = when {
                creditsRepository.observeBalance().first() <= 0 -> ApplicationDetailEvent.OpenPaywall(applicationId)
                hasQuestion() -> ApplicationDetailEvent.OpenQuickQuestion(applicationId)
                else -> ApplicationDetailEvent.StartTailoring(applicationId)
            }
            eventChannel.send(event)
        }
    }

    fun onSoon() {
        eventChannel.trySend(ApplicationDetailEvent.ComingSoon)
    }

    private suspend fun hasQuestion(): Boolean =
        applicationRepository.observeApplication(applicationId).first()?.gapAnalysis?.question != null

    private fun sendIntent(
        build: suspend (String) -> Intent?,
        event: (Intent) -> ApplicationDetailEvent,
    ) {
        viewModelScope.launch {
            val intent = build(applicationId)
            eventChannel.send(intent?.let(event) ?: ApplicationDetailEvent.FileMissing)
        }
    }

    private fun stateFor(application: JobApplication?, sheet: ApplicationStatus?): ApplicationDetailUiState {
        if (application == null) return ApplicationDetailUiState.NotFound
        return ApplicationDetailUiState.Ready(
            title = application.job.title,
            company = application.job.company,
            place = application.location,
            status = application.status,
            appliedOn = appliedOnOf(application),
            file = application.exportFileName?.let { ApplicationFile(it, application.keywordCoverage.keywordPercent()) },
            statusSheet = sheet,
        )
    }

    private fun appliedOnOf(application: JobApplication): String? {
        val appliedOn = application.appliedOn ?: return null
        if (application.status == ApplicationStatus.SAVED) return null
        return formatDay(appliedOn)
    }

    private fun formatDay(instant: KotlinInstant): String =
        DateTimeFormatter.ofPattern(DAY_PATTERN, Locale.ENGLISH)
            .withZone(ZoneId.systemDefault())
            .format(Instant.ofEpochMilli(instant.toEpochMilliseconds()))

    private companion object {
        const val DAY_PATTERN = "d MMM"
    }

    @AssistedFactory
    interface Factory {
        fun create(applicationId: String): ApplicationDetailViewModel
    }
}
