package com.tailormyresume.feature.tailor.impl.edit

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tailormyresume.core.data.repository.ApplicationRepository
import com.tailormyresume.core.data.repository.ProfileRepository
import com.tailormyresume.core.data.repository.ResumeSettingsRepository
import com.tailormyresume.core.data.repository.TailoringReviewStateRepository
import com.tailormyresume.core.domain.UpdateBulletDecisionUseCase
import com.tailormyresume.core.domain.coverage.KeywordCoverageCalculator
import com.tailormyresume.core.model.BulletDecision
import com.tailormyresume.core.model.CandidateProfile
import com.tailormyresume.core.model.JobApplication
import com.tailormyresume.core.model.ResumeSettings
import com.tailormyresume.core.model.TailoredBullet
import com.tailormyresume.core.model.TailoredResume
import com.tailormyresume.core.model.TailoredSkills
import com.tailormyresume.core.model.TailoredText
import com.tailormyresume.feature.tailor.impl.document.ResumeDocumentAssembler
import com.tailormyresume.feature.tailor.impl.document.ResumeEntry
import com.tailormyresume.feature.tailor.impl.export.ResumePdfRenderer
import com.tailormyresume.feature.tailor.impl.result.ChangeSource
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

@HiltViewModel(assistedFactory = EditResumeViewModel.Factory::class)
internal class EditResumeViewModel @AssistedInject constructor(
    private val applicationRepository: ApplicationRepository,
    private val profileRepository: ProfileRepository,
    private val settingsRepository: ResumeSettingsRepository,
    private val assembler: ResumeDocumentAssembler,
    private val reviewState: TailoringReviewStateRepository,
    private val clock: kotlin.time.Clock,
    private val renderer: ResumePdfRenderer,
    @Assisted val applicationId: String,
    private val workFence: com.tailormyresume.core.data.account.LocalWorkFence = com.tailormyresume.core.data.account.LocalWorkFence(),
) : ViewModel() {
    private val workTicket = workFence.ticket()


    private val drafts = MutableStateFlow(EditResumeDrafts())

    private val saveMutex = Mutex()

    private val eventChannel = Channel<EditResumeEvent>(Channel.BUFFERED)

    val state: StateFlow<EditResumeUiState> = combine(
        applicationRepository.observeApplication(applicationId),
        profileRepository.observeProfile(),
        settingsRepository.observeSettings(),
        drafts,
    ) { application, profile, settings, currentDrafts ->
        Triple(application, profile, settings) to currentDrafts
    }.mapLatest { (inputs, currentDrafts) ->
        val (application, profile, settings) = inputs
        buildState(application, profile, settings, currentDrafts)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = EditResumeUiState.Loading,
    )

    val events: Flow<EditResumeEvent> = eventChannel.receiveAsFlow()

    fun onBulletChange(bulletId: String, text: String) {
        workFence.editing(this, true)
        drafts.value = drafts.value.copy(bullets = drafts.value.bullets + (bulletId to text))
    }

    fun onSummaryChange(text: String) {
        workFence.editing(this, true)
        drafts.value = drafts.value.copy(summary = text)
    }

    fun onSkillsChange(text: String) {
        workFence.editing(this, true)
        drafts.value = drafts.value.copy(skills = text)
    }

    fun onSave() {
        viewModelScope.launch(workFence.context(workTicket)) {
            try {
                saveMutex.withLock {
                    val current = drafts.value
                    val application = applicationRepository.observeApplication(applicationId).first()
                    val profile = profileRepository.observeProfile().first()
                    val resume = application?.tailoredResume
                    if (application == null || profile == null || resume == null) return@withLock
                    val stored = resume.bullets
                    val editedBulletIds = mutableListOf<String>()
                    val bullets = stored.map { bullet ->
                        val text = current.bullets[bullet.id] ?: return@map bullet
                        val edited = text.trim()
                        if (edited.isEmpty() || edited == bullet.effectiveText()) return@map bullet
                        editedBulletIds += bullet.id
                        bullet.copy(
                            proposedText = edited,
                            decision = BulletDecision.ACCEPTED,
                            violations = emptyList(),
                            editTypes = emptyList(),
                            keywordsUsed = emptyList(),
                        )
                    }
                    val edited = applyDrafts(resume.copy(bullets = bullets), profile, current)
                    if (edited != resume) {
                        val keywordCoverage = application.gapAnalysis?.let { gap ->
                            KeywordCoverageCalculator.compute(gap.matches, application.quickAnswer, edited, profile)
                        } ?: application.keywordCoverage
                        workFence.check(workTicket)
                        applicationRepository.upsertApplication(
                            application.copy(
                                tailoredResume = edited,
                                keywordCoverage = keywordCoverage,
                                updatedAt = clock.now(),
                            ),
                        )
                        editedBulletIds.forEach { reviewState.markEdited(applicationId, it) }
                        if (current.summary != null && edited.summary != resume.summary) {
                            reviewState.markEdited(applicationId, UpdateBulletDecisionUseCase.SUMMARY_CHANGE_ID)
                        }
                        if (current.skills != null && edited.skills != resume.skills) {
                            reviewState.markEdited(applicationId, UpdateBulletDecisionUseCase.SKILLS_CHANGE_ID)
                        }
                    }
                    drafts.value = EditResumeDrafts()
                    workFence.editing(this, false)
                    eventChannel.send(EditResumeEvent.Saved)
                }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                eventChannel.send(EditResumeEvent.SaveFailed)
            }
        }
    }

    private suspend fun buildState(
        application: JobApplication?,
        profile: CandidateProfile?,
        settings: ResumeSettings,
        currentDrafts: EditResumeDrafts,
    ): EditResumeUiState {
        val resume = application?.tailoredResume
        if (application == null || profile == null || resume == null) return EditResumeUiState.NotFound
        if (currentDrafts.isNotEmpty) delay(FIT_DEBOUNCE_MS)
        val edited = applyDrafts(resume.withPendingAccepted(), profile, currentDrafts)
        val document = assembler.assemble(profile, edited)
        val used = mutableSetOf<String>()
        val roles = document.sections.flatMap { section ->
            section.entries.filter { it.bullets.isNotEmpty() }.map { entry ->
                EditRole(
                    label = entry.roleLabel(),
                    bullets = entry.bullets.map { text ->
                        val bullet = edited.match(text, used)
                        EditBullet(
                            id = bullet?.id,
                            text = text,
                            source = bullet.source(),
                        )
                    },
                )
            }
        }
        return EditResumeUiState.Ready(
            summary = document.summary,
            roles = roles,
            skills = document.skills.joinToString(", "),
            fitsOnOnePage = renderer.pageCount(document, settings.pageSize) == 1,
        )
    }

    private fun applyDrafts(
        resume: TailoredResume,
        profile: CandidateProfile,
        currentDrafts: EditResumeDrafts,
    ): TailoredResume =
        resume.copy(
            bullets = resume.bullets.map { bullet ->
                val draft = currentDrafts.bullets[bullet.id]?.trim()
                if (draft.isNullOrEmpty()) {
                    bullet
                } else {
                    bullet.copy(
                        proposedText = draft,
                        decision = BulletDecision.ACCEPTED,
                        violations = emptyList(),
                    )
                }
            },
            summary = currentDrafts.summary?.let { text ->
                TailoredText(
                    text = text.trim(),
                    original = resume.summary?.original ?: profile.summary.trim(),
                    sourceIds = resume.summary?.sourceIds.orEmpty(),
                    decision = BulletDecision.ACCEPTED,
                )
            } ?: resume.summary,
            skills = currentDrafts.skills?.let { text ->
                TailoredSkills(
                    skills = text.split(',').map(String::trim).filter(String::isNotEmpty),
                    original = resume.skills?.original ?: profile.skills,
                    decision = BulletDecision.ACCEPTED,
                )
            } ?: resume.skills,
        )

    private fun TailoredResume.match(text: String, used: MutableSet<String>): TailoredBullet? =
        bullets.firstOrNull { bullet -> bullet.id !in used && bullet.effectiveText() == text }
            ?.also { used += it.id }

    private fun TailoredBullet.effectiveText(): String =
        if (decision == BulletDecision.ACCEPTED && violations.isEmpty()) proposedText.trim() else originalText.trim()

    private fun TailoredBullet?.source(): ChangeSource =
        if (this?.sourceIds?.any { it.startsWith(ANSWER_SOURCE_PREFIX) } == true) {
            ChangeSource.YourAnswer
        } else {
            ChangeSource.YourResume
        }

    private fun ResumeEntry.roleLabel(): String =
        if (organization.isBlank()) title else "$title · $organization"

    @AssistedFactory
    interface Factory {
        fun create(applicationId: String): EditResumeViewModel
    }
    override fun onCleared() {
        workFence.editing(this, false)
        super.onCleared()
    }

}

private data class EditResumeDrafts(
    val bullets: Map<String, String> = emptyMap(),
    val summary: String? = null,
    val skills: String? = null,
) {
    val isNotEmpty: Boolean get() = bullets.isNotEmpty() || summary != null || skills != null
}

private const val ANSWER_SOURCE_PREFIX = "ans-"

private const val FIT_DEBOUNCE_MS = 300L
