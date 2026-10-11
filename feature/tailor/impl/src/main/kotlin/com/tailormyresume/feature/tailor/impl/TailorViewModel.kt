package com.tailormyresume.feature.tailor.impl

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tailormyresume.core.data.repository.ApplicationRepository
import com.tailormyresume.core.data.repository.ContentReportRepository
import com.tailormyresume.core.data.repository.ProfileRepository
import com.tailormyresume.core.data.repository.TailoringReviewStateRepository
import com.tailormyresume.core.domain.UpdateBulletDecisionUseCase
import com.tailormyresume.core.model.BulletDecision
import com.tailormyresume.core.model.CandidateProfile
import com.tailormyresume.core.model.ContentReport
import com.tailormyresume.core.model.DebugScenario
import com.tailormyresume.core.model.JobApplication
import com.tailormyresume.core.model.ReportedItemKind
import com.tailormyresume.core.model.TailoringReviewState
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlin.time.Clock

@HiltViewModel(assistedFactory = TailorViewModel.Factory::class)
internal class TailorViewModel @AssistedInject constructor(
    private val applicationRepository: ApplicationRepository,
    profileRepository: ProfileRepository,
    private val reviewStateRepository: TailoringReviewStateRepository,
    private val contentReportRepository: ContentReportRepository,
    private val clock: Clock,
    private val updateBulletDecision: UpdateBulletDecisionUseCase,
    private val handEditBullet: HandEditBulletUseCase,
    @Assisted val applicationId: String,
    @Assisted scenario: DebugScenario,
) : ViewModel() {

    private val decisionMutex = Mutex()

    private val activeScenario = MutableStateFlow(scenario)

    private val sessionState = combine(
        reviewStateRepository.observe(applicationId),
        contentReportRepository.observeReportedIds(applicationId, ReportedItemKind.RESUME_BULLET),
        contentReportRepository.observeReportedIds(applicationId, ReportedItemKind.SECTION),
    ) { review, bullets, sections -> ReviewSession(review, bullets + sections.map(::sectionReportId)) }

    val uiState: StateFlow<TailorUiState> = combine(
        applicationRepository.observeApplication(applicationId),
        profileRepository.observeProfile(),
        sessionState,
        activeScenario,
    ) { application, profile, session, forced ->
        stateFor(application, profile, session, forced)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = TailorUiState.Loading(),
    )

    fun onAccept(bulletId: String) = setDecision(bulletId, BulletDecision.ACCEPTED)

    fun onKeepOriginal(bulletId: String) = setDecision(bulletId, BulletDecision.REJECTED)

    fun onUndo(bulletId: String) = setDecision(bulletId, BulletDecision.PENDING)

    fun onEditByHand(bulletId: String, text: String) {
        viewModelScope.launch {
            decisionMutex.withLock { handEditBullet(applicationId, bulletId, text) }
        }
    }

    fun onReportBullet(bulletId: String) = report(ReportedItemKind.RESUME_BULLET, bulletId)

    fun onReportSection(sectionKey: String) = report(ReportedItemKind.SECTION, sectionKey)

    fun onRetry() {
        activeScenario.value = DebugScenario.DEFAULT
    }

    private fun report(kind: ReportedItemKind, itemId: String) {
        val success = uiState.value as? TailorUiState.Success ?: return
        val itemText = success.reportedText(kind, itemId) ?: return
        viewModelScope.launch {
            contentReportRepository.report(
                ContentReport(
                    applicationId = applicationId,
                    itemKind = kind,
                    itemId = itemId,
                    itemText = itemText,
                    reportedAt = clock.now(),
                    generationId = success.reportedGenerationId(kind, itemId),
                ),
            )
        }
    }

    private fun setDecision(bulletId: String, decision: BulletDecision) {
        viewModelScope.launch {
            decisionMutex.withLock { updateBulletDecision(applicationId, bulletId, decision) }
        }
    }

    private fun stateFor(
        application: JobApplication?,
        profile: CandidateProfile?,
        session: ReviewSession,
        forced: DebugScenario,
    ): TailorUiState {
        val job = application?.job?.let { JobHeader(title = it.title, company = it.company) }
        return when (forced) {
            DebugScenario.LOADING -> TailorUiState.Loading(
                job = job,
                factCount = application?.tailoredResume?.bullets?.flatMap { it.sourceIds }?.distinct()?.size,
                lineCount = application?.tailoredResume?.bullets?.size,
            )
            DebugScenario.ERROR -> TailorUiState.Failed(job)
            else -> buildTailorUiState(
                TailorInputs(
                    application = application,
                    profile = profile,
                    editedBulletIds = session.review.editedBulletIds,
                    reportedIds = session.reportedIds,
                ),
            )
        }
    }

    private data class ReviewSession(val review: TailoringReviewState, val reportedIds: Set<String>)

    @AssistedFactory
    interface Factory {
        fun create(applicationId: String, scenario: DebugScenario): TailorViewModel
    }
}
