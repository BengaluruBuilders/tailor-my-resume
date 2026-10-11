package com.tailormyresume.feature.tailor.impl.tailoring

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tailormyresume.core.data.repository.ApplicationRepository
import com.tailormyresume.core.domain.coverage.ScreenForKeywords
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

internal sealed interface TailoringOutcome {
    data object Done : TailoringOutcome

    data object Failed : TailoringOutcome
}

private const val PROGRESS_STEP_MILLIS = 40L
private const val PROGRESS_RUNNING_MAX = 99
private const val PROGRESS_COMPLETE = 100
private const val STICKER_LIMIT = 3

@HiltViewModel(assistedFactory = TailoringViewModel.Factory::class)
internal class TailoringViewModel @AssistedInject constructor(
    private val runner: TailoringRunner,
    applicationRepository: ApplicationRepository,
    @Assisted("applicationId") val applicationId: String,
    @Assisted("runId") runId: String,
) : ViewModel() {

    private val progressState = MutableStateFlow(0)

    private val outcomeState = MutableStateFlow<TailoringOutcome?>(null)

    private val ramp = viewModelScope.launch {
        while (progressState.value < PROGRESS_RUNNING_MAX) {
            delay(PROGRESS_STEP_MILLIS)
            progressState.value = (progressState.value + 1).coerceAtMost(PROGRESS_RUNNING_MAX)
        }
    }

    init {
        viewModelScope.launch {
            when (runner(applicationId, runId)) {
                TailoringResult.Success -> {
                    ramp.cancel()
                    progressState.value = PROGRESS_COMPLETE
                    outcomeState.value = TailoringOutcome.Done
                }

                is TailoringResult.Failure -> {
                    ramp.cancel()
                    outcomeState.value = TailoringOutcome.Failed
                }
            }
        }
    }

    val progress: StateFlow<Int> = progressState

    val outcome: StateFlow<TailoringOutcome?> = outcomeState

    val uiState: StateFlow<TailoringUiState> = combine(
        progressState,
        applicationRepository.observeApplication(applicationId),
    ) { percent, application ->
        val keywords = application?.gapAnalysis?.let { ScreenForKeywords(it.matches) }
        TailoringUiState(
            percent = percent,
            rows = tailoringRows(percent, application?.quickAnswer.addsExample()),
            keywordCount = keywords?.count ?: 0,
            stickers = keywords?.let { (it.have + it.missing).take(STICKER_LIMIT) } ?: emptyList(),
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = TailoringUiState(
            percent = 0,
            rows = tailoringRows(0, false),
            keywordCount = 0,
            stickers = emptyList(),
        ),
    )

    @AssistedFactory
    interface Factory {
        fun create(
            @Assisted("applicationId") applicationId: String,
            @Assisted("runId") runId: String,
        ): TailoringViewModel
    }
}
