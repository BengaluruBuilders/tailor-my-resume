package com.tailormyresume.core.domain.onboarding

import com.tailormyresume.core.data.repository.ProfileRepository
import com.tailormyresume.core.domain.profile.RequiredGap
import com.tailormyresume.core.domain.profile.RequiredGaps
import com.tailormyresume.core.model.CandidateProfile
import com.tailormyresume.core.model.EntryCategory
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import kotlin.time.Clock

sealed interface MarkReviewedResult {
    data class Reviewed(val profile: CandidateProfile) : MarkReviewedResult

    data class RequiredInfoMissing(val gaps: List<RequiredGap>) : MarkReviewedResult

    data object NothingToReview : MarkReviewedResult
}

fun CandidateProfile?.hasNothingToReview(): Boolean =
    this == null || (entries.isEmpty() && skills.isEmpty() && fullName.isBlank())

private val CONFIRMED_ON_REVIEW = setOf(EntryCategory.EXPERIENCE, EntryCategory.EDUCATION)

class MarkProfileReviewedUseCase @Inject constructor(
    private val profileRepository: ProfileRepository,
    private val clock: Clock,
) {
    suspend operator fun invoke(): MarkReviewedResult {
        val profile = profileRepository.observeProfile().first() ?: return MarkReviewedResult.NothingToReview
        if (profile.hasNothingToReview()) return MarkReviewedResult.NothingToReview
        val gaps = RequiredGaps.of(profile)
        if (gaps.isNotEmpty()) return MarkReviewedResult.RequiredInfoMissing(gaps)
        val reviewed = profile.copy(
            entries = profile.entries.map { if (it.category in CONFIRMED_ON_REVIEW) it.copy(isConfirmed = true) else it },
            reviewedAt = clock.now(),
        )
        profileRepository.saveProfile(reviewed)
        return MarkReviewedResult.Reviewed(reviewed)
    }
}
