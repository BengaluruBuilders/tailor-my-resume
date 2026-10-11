package com.tailormyresume.feature.onboarding.impl.review

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tailormyresume.core.data.repository.ProfileRepository
import com.tailormyresume.core.domain.onboarding.MarkProfileReviewedUseCase
import com.tailormyresume.core.domain.onboarding.MarkReviewedResult
import com.tailormyresume.core.domain.onboarding.hasNothingToReview
import com.tailormyresume.core.model.CandidateProfile
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.concurrent.atomic.AtomicBoolean
import javax.inject.Inject

private const val STOP_TIMEOUT_MS = 5_000L

private fun CandidateProfile?.toUiState(): ReviewUiState =
    this?.takeUnless { it.hasNothingToReview() }?.toReviewUiState() ?: ReviewUiState.Loading

@HiltViewModel
internal class ReviewViewModel @Inject constructor(
    private val profileRepository: ProfileRepository,
    private val markReviewed: MarkProfileReviewedUseCase,
) : ViewModel() {

    private val eventChannel = Channel<ReviewEvent>(Channel.BUFFERED)

    private val continuing = AtomicBoolean(false)

    val uiState: StateFlow<ReviewUiState> = profileRepository.observeProfile()
        .map { it.toUiState() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), ReviewUiState.Loading)

    val events: Flow<ReviewEvent> = eventChannel.receiveAsFlow()

    init {
        viewModelScope.launch {
            if (profileRepository.observeProfile().first().hasNothingToReview()) {
                eventChannel.trySend(ReviewEvent.NothingToReview)
            }
        }
    }

    fun onContinue() {
        if (!continuing.compareAndSet(false, true)) return
        viewModelScope.launch {
            try {
                eventChannel.trySend(
                    when (markReviewed()) {
                        is MarkReviewedResult.Reviewed -> ReviewEvent.Continue
                        is MarkReviewedResult.RequiredInfoMissing -> ReviewEvent.MissingEndDate
                        MarkReviewedResult.NothingToReview -> ReviewEvent.NothingToReview
                    },
                )
            } catch (cancellation: CancellationException) {
                throw cancellation
            } catch (failure: Exception) {
                return@launch
            } finally {
                continuing.set(false)
            }
        }
    }
}
