package com.tailormyresume.feature.analysis.impl.result

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tailormyresume.core.data.repository.ApplicationRepository
import com.tailormyresume.core.data.repository.ProfileRepository
import com.tailormyresume.core.domain.TailorResumeUseCase
import com.tailormyresume.core.domain.account.AccountCreditBalance
import com.tailormyresume.core.domain.coverage.KeywordCoverageCalculator
import com.tailormyresume.core.domain.coverage.ScreenForKeywords
import com.tailormyresume.core.model.CandidateProfile
import com.tailormyresume.core.model.JobApplication
import com.tailormyresume.core.model.QuickAnswer
import com.tailormyresume.core.model.RequirementMatch
import com.tailormyresume.core.model.RequirementPriority
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@HiltViewModel(assistedFactory = JobResultViewModel.Factory::class)
internal class JobResultViewModel @AssistedInject constructor(
    private val applicationRepository: ApplicationRepository,
    private val creditBalance: AccountCreditBalance,
    private val profileRepository: ProfileRepository,
    private val tailorResume: TailorResumeUseCase,
    @Assisted val applicationId: String,
) : ViewModel() {

    private val eventChannel = Channel<JobResultEvent>(Channel.BUFFERED)
    private var tailorTapped = false

    val uiState: StateFlow<JobResultUiState> = combine(
        applicationRepository.observeApplication(applicationId),
        creditBalance.observeCredits(),
        profileRepository.observeProfile(),
    ) { application, credits, profile ->
        toUiState(application, credits, profile, tailorResume.hasUnfinishedRequest(applicationId))
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), JobResultUiState.Loading)

    val events: Flow<JobResultEvent> = eventChannel.receiveAsFlow()

    fun onTailor() {
        val state = uiState.value
        if (state !is JobResultUiState.Ready || tailorTapped) return
        tailorTapped = true
        viewModelScope.launch {
            val hasUnfinishedRequest = tailorResume.hasUnfinishedRequest(applicationId)
            val event = if (hasUnfinishedRequest) {
                JobResultEvent.Tailor(applicationId)
            } else {
                val credits = state.credits ?: run {
                    tailorTapped = false
                    return@launch
                }
                when {
                    credits <= 0 -> JobResultEvent.EarnCredits(applicationId)
                    state.asksQuestion -> JobResultEvent.QuickQuestion(applicationId)
                    else -> JobResultEvent.Tailor(applicationId)
                }
            }
            eventChannel.send(event)
        }
    }

    fun onShown() {
        tailorTapped = false
    }

    @AssistedFactory
    interface Factory {
        fun create(applicationId: String): JobResultViewModel
    }
}

private fun toUiState(
    application: JobApplication?,
    credits: Int?,
    profile: CandidateProfile?,
    hasUnfinishedRequest: Boolean,
): JobResultUiState {
    if (profile == null) return JobResultUiState.Unavailable
    val gap = application?.gapAnalysis ?: return JobResultUiState.Unavailable
    val coverage = KeywordCoverageCalculator.compute(gap.matches, application.quickAnswer, null, profile)
    val screen = ScreenForKeywords(gap.matches, profile)
    return JobResultUiState.Ready(
        title = application.job.title,
        company = application.job.company,
        location = application.location.ifBlank { application.job.location.orEmpty() },
        now = coverage.now,
        upTo = coverage.upTo,
        have = screen.have,
        missing = screen.missing,
        mustHaves = gap.matches
            .filter { it.requirement.priority == RequirementPriority.MUST_HAVE }
            .map { match ->
                MustHaveRow(
                    requirementId = match.requirement.id,
                    text = match.requirement.text,
                    status = match.status,
                    reason = match.keepReason(),
                    unclear = application.quickAnswer == null && match.requirement.id == gap.question?.requirementId,
                    answered = application.quickAnswer.confirms(match.requirement.id),
                )
            },
        credits = credits,
        asksQuestion = gap.question != null && application.quickAnswer == null,
        hasUnfinishedRequest = hasUnfinishedRequest,
    )
}

private fun RequirementMatch.keepReason(): String? = reason?.takeIf { it.isNotBlank() }

private fun QuickAnswer?.confirms(requirementId: String): Boolean =
    this?.requirementId == requirementId && choice in setOf("YES_REGULARLY", "A_FEW_TIMES")
