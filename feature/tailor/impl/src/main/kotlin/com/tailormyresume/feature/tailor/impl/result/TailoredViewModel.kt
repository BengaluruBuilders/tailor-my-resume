package com.tailormyresume.feature.tailor.impl.result

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tailormyresume.core.data.repository.ApplicationRepository
import com.tailormyresume.core.data.repository.ProfileRepository
import com.tailormyresume.core.domain.ExportCheck
import com.tailormyresume.core.domain.ExportReadiness
import com.tailormyresume.core.domain.coverage.KeywordCoverageCalculator
import com.tailormyresume.core.model.CandidateProfile
import com.tailormyresume.core.model.JobApplication
import com.tailormyresume.feature.tailor.impl.document.ResumeDocumentAssembler
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

@HiltViewModel(assistedFactory = TailoredViewModel.Factory::class)
internal class TailoredViewModel @AssistedInject constructor(
    applicationRepository: ApplicationRepository,
    profileRepository: ProfileRepository,
    private val assembler: ResumeDocumentAssembler,
    @Assisted val applicationId: String,
) : ViewModel() {

    val state: StateFlow<TailoredUiState> = combine(
        applicationRepository.observeApplication(applicationId),
        profileRepository.observeProfile(),
        ::stateFor,
    ).stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = TailoredUiState.Loading,
    )

    private fun stateFor(application: JobApplication?, profile: CandidateProfile?): TailoredUiState {
        val resume = application?.tailoredResume
        if (application == null || profile == null || resume == null) return TailoredUiState.NotFound
        val previewed = resume.withPendingAccepted()
        val document = assembler.assemble(profile, previewed)
        return TailoredUiState.Ready(
            jobTitle = application.job.title,
            company = application.job.company,
            name = document.name,
            contact = document.contactLine,
            blocks = document.toPaperBlocks(previewed),
            coveragePercent = application.gapAnalysis
                ?.let { KeywordCoverageCalculator.compute(it.matches, application.quickAnswer, previewed).final }
                ?: 0,
            changes = resume.toChangeCards(profile),
            accepted = application.changesAcceptedAt != null,
            exportEnabled = ExportReadiness.check(application) == ExportCheck.ALLOWED,
        )
    }

    @AssistedFactory
    interface Factory {
        fun create(applicationId: String): TailoredViewModel
    }
}
