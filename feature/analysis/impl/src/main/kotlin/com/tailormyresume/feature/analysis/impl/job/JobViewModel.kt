package com.tailormyresume.feature.analysis.impl.job

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tailormyresume.core.common.network.Dispatcher
import com.tailormyresume.core.common.network.TmrDispatchers
import com.tailormyresume.core.data.repository.ApplicationRepository
import com.tailormyresume.core.data.repository.ProfileRepository
import com.tailormyresume.core.domain.AiException
import com.tailormyresume.core.domain.AiFailure
import com.tailormyresume.core.domain.AnalyzeJobUseCase
import com.tailormyresume.core.domain.IdGenerator
import com.tailormyresume.core.domain.JobLabelProposal
import com.tailormyresume.core.domain.ProposeJobLabelUseCase
import com.tailormyresume.core.domain.coverage.KeywordCoverageCalculator
import com.tailormyresume.core.model.ApplicationStatus
import com.tailormyresume.core.model.JobApplication
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.transformLatest
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.time.Clock

@HiltViewModel
internal class JobViewModel @Inject constructor(
    private val analyzeJob: AnalyzeJobUseCase,
    private val proposeJobLabel: ProposeJobLabelUseCase,
    private val applicationRepository: ApplicationRepository,
    private val profileRepository: ProfileRepository,
    private val draftStore: JobDraftStore,
    private val progressTicker: AnalysisProgressTicker,
    private val idGenerator: IdGenerator,
    private val clock: Clock,
    @Dispatcher(TmrDispatchers.Default) private val defaultDispatcher: CoroutineDispatcher,
) : ViewModel() {

    private val text = MutableStateFlow("")
    private val notAJobPost = MutableStateFlow(false)
    private val percent = MutableStateFlow<Int?>(null)
    private val eventChannel = Channel<JobEvent>(Channel.BUFFERED)

    @OptIn(ExperimentalCoroutinesApi::class)
    private val detectedLabel: Flow<String?> = text
        .map { it.trim() }
        .distinctUntilChanged()
        .transformLatest { trimmed ->
            if (trimmed.length < MIN_JOB_CHARS) {
                emit(null)
            } else {
                delay(LABEL_DEBOUNCE_MILLIS)
                emit(proposeJobLabel(trimmed).displayLabel())
            }
        }
        .flowOn(defaultDispatcher)

    val uiState: StateFlow<JobUiState> = combine(
        text,
        notAJobPost,
        percent,
        detectedLabel,
    ) { currentText, flagged, currentPercent, label ->
        when {
            currentPercent != null -> JobUiState.Analyzing(currentText, currentPercent)
            currentText.isEmpty() && !flagged -> JobUiState.Empty
            else -> JobUiState.HasText(
                text = currentText,
                detected = label.takeIf { currentText.trim().length >= MIN_JOB_CHARS },
                notAJobPost = flagged,
            )
        }
    }.stateIn(viewModelScope, SharingStarted.Eagerly, JobUiState.Empty)

    val events: Flow<JobEvent> = eventChannel.receiveAsFlow()

    init {
        viewModelScope.launch {
            draftStore.draft.filterNotNull().collect { applyDraft(draftStore.consume()) }
        }
    }

    fun onTextChange(text: String) {
        setText(text)
    }

    fun onPaste(clipboardText: String) {
        setText(clipboardText)
    }

    fun onClear() {
        text.value = ""
        notAJobPost.value = false
    }

    fun onAnalyze() {
        val jobText = text.value
        if (percent.value != null || jobText.isEmpty()) return
        if (!isPlausibleJobPost(jobText)) {
            notAJobPost.value = true
            return
        }
        viewModelScope.launch {
            percent.value = 0
            val tickerJob = launch {
                progressTicker.percents().collect { step ->
                    if (percent.value != null) percent.value = step
                }
            }
            try {
                val profile = profileRepository.observeProfile().first()
                    ?: throw IllegalStateException("No profile saved")
                val result = analyzeJob(profile, jobText)
                tickerJob.cancel()
                percent.value = 100
                val now = clock.now()
                val application = JobApplication(
                    id = idGenerator.newId(),
                    job = result.job,
                    status = ApplicationStatus.SAVED,
                    gapAnalysis = result.gap,
                    tailoredResume = null,
                    createdAt = now,
                    updatedAt = now,
                    location = result.job.location.orEmpty(),
                    keywordCoverage = KeywordCoverageCalculator.compute(result.gap.matches, null, null),
                )
                applicationRepository.upsertApplication(application)
                draftStore.clear()
                text.value = ""
                notAJobPost.value = false
                percent.value = null
                eventChannel.send(JobEvent.Analyzed(application.id))
            } catch (e: CancellationException) {
                throw e
            } catch (e: AiException) {
                if (e.failure == AiFailure.NotAJobPost) {
                    notAJobPost.value = true
                } else {
                    eventChannel.send(JobEvent.AnalysisFailed)
                }
            } catch (e: Exception) {
                eventChannel.send(JobEvent.AnalysisFailed)
            } finally {
                tickerJob.cancel()
                percent.value = null
            }
        }
    }

    private fun setText(newText: String) {
        text.value = newText.take(MAX_JOB_CHARS)
        notAJobPost.value = false
    }

    private fun applyDraft(draft: JobDraft?) {
        when {
            draft == null -> Unit
            draft.notAJobPost -> {
                text.value = ""
                notAJobPost.value = true
            }
            draft.text != null -> {
                text.value = draft.text.take(MAX_JOB_CHARS)
                notAJobPost.value = false
                draft.importedFrom?.let { eventChannel.trySend(JobEvent.Imported(it)) }
            }
        }
    }

    private fun JobLabelProposal.displayLabel(): String? = when {
        role.isBlank() && company.isBlank() -> null
        company.isBlank() -> role
        role.isBlank() -> company
        else -> "$role · $company"
    }

    private companion object {
        const val LABEL_DEBOUNCE_MILLIS = 300L
    }
}
