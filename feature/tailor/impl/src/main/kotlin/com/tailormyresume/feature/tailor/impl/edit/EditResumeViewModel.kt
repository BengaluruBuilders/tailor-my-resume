package com.tailormyresume.feature.tailor.impl.edit

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tailormyresume.core.data.repository.ApplicationRepository
import com.tailormyresume.core.data.repository.ProfileRepository
import com.tailormyresume.core.data.repository.ResumeSettingsRepository
import com.tailormyresume.core.model.BulletDecision
import com.tailormyresume.core.model.CandidateProfile
import com.tailormyresume.core.model.JobApplication
import com.tailormyresume.core.model.ResumeSettings
import com.tailormyresume.core.model.TailoredBullet
import com.tailormyresume.core.model.TailoredResume
import com.tailormyresume.feature.tailor.impl.HandEditBulletUseCase
import com.tailormyresume.feature.tailor.impl.document.ResumeDocumentAssembler
import com.tailormyresume.feature.tailor.impl.document.ResumeEntry
import com.tailormyresume.feature.tailor.impl.export.ResumePdfRenderer
import com.tailormyresume.feature.tailor.impl.result.ChangeSource
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
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
    private val handEditBullet: HandEditBulletUseCase,
    private val renderer: ResumePdfRenderer,
    @Assisted val applicationId: String,
) : ViewModel() {

    private val drafts = MutableStateFlow<Map<String, String>>(emptyMap())

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
        drafts.value = drafts.value + (bulletId to text)
    }

    fun onSave() {
        viewModelScope.launch {
            saveMutex.withLock {
                val current = drafts.value
                val application = applicationRepository.observeApplication(applicationId).first()
                val stored = application?.tailoredResume?.bullets.orEmpty()
                for ((bulletId, text) in current) {
                    val edited = text.trim()
                    if (edited.isEmpty()) continue
                    val bullet = stored.firstOrNull { it.id == bulletId } ?: continue
                    if (edited == bullet.effectiveText()) continue
                    handEditBullet(applicationId, bulletId, edited)
                }
                drafts.value = emptyMap()
                eventChannel.send(EditResumeEvent.Saved)
            }
        }
    }

    private suspend fun buildState(
        application: JobApplication?,
        profile: CandidateProfile?,
        settings: ResumeSettings,
        currentDrafts: Map<String, String>,
    ): EditResumeUiState {
        val resume = application?.tailoredResume
        if (application == null || profile == null || resume == null) return EditResumeUiState.NotFound
        if (currentDrafts.isNotEmpty()) delay(FIT_DEBOUNCE_MS)
        val edited = applyDrafts(resume.withPendingAccepted(), currentDrafts)
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

    private fun applyDrafts(resume: TailoredResume, currentDrafts: Map<String, String>): TailoredResume =
        resume.copy(
            bullets = resume.bullets.map { bullet ->
                val draft = currentDrafts[bullet.id]?.trim()
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
}

private const val ANSWER_SOURCE_PREFIX = "ans-"

private const val FIT_DEBOUNCE_MS = 300L
