package com.tailormyresume.core.domain.onboarding

import com.tailormyresume.core.data.repository.ProfileRepository
import com.tailormyresume.core.domain.profile.RequiredGap
import com.tailormyresume.core.model.CandidateProfile
import javax.inject.Inject
import kotlin.time.Clock

sealed interface MarkReviewedResult {
    data class Reviewed(val profile: CandidateProfile) : MarkReviewedResult

    data class RequiredInfoMissing(val gaps: List<RequiredGap>) : MarkReviewedResult

    data object NothingToReview : MarkReviewedResult
}

fun CandidateProfile?.hasNothingToReview(): Boolean = true

class MarkProfileReviewedUseCase @Inject constructor(
    private val profileRepository: ProfileRepository,
    private val clock: Clock,
) {
    suspend operator fun invoke(): MarkReviewedResult = MarkReviewedResult.NothingToReview
}
