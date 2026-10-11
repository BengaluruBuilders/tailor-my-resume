package com.tailormyresume.feature.onboarding.impl.review

import androidx.lifecycle.ViewModel
import com.tailormyresume.core.data.repository.ProfileRepository
import com.tailormyresume.core.domain.onboarding.MarkProfileReviewedUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.emptyFlow
import javax.inject.Inject

@HiltViewModel
internal class ReviewViewModel @Inject constructor(
    private val profileRepository: ProfileRepository,
    private val markReviewed: MarkProfileReviewedUseCase,
) : ViewModel() {

    val uiState: StateFlow<ReviewUiState> = MutableStateFlow(ReviewUiState.Loading)

    val events: Flow<ReviewEvent> = emptyFlow()

    fun onContinue() = Unit
}
