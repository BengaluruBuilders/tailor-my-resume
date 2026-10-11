package com.tailormyresume.feature.onboarding.impl.review

import com.tailormyresume.core.model.CandidateProfile

internal sealed interface ReviewDates {
    data object None : ReviewDates

    data class Range(val start: String, val end: String) : ReviewDates

    data class Current(val start: String) : ReviewDates

    data class EndMissing(val start: String) : ReviewDates
}

internal data class ReviewGap(val entryId: String, val title: String)

internal data class ReviewContact(val name: String, val email: String, val phoneAndCity: String)

internal data class ReviewRole(val title: String, val organization: String, val dates: ReviewDates, val bullets: List<String>)

internal data class ReviewEducation(val title: String, val detail: String, val bullets: List<String>)

internal data class ReviewSkills(val count: Int, val chips: List<String>)

internal sealed interface ReviewUiState {
    data object Loading : ReviewUiState

    data class Ready(
        val sourceFileName: String?,
        val gap: ReviewGap?,
        val inviteWorkExample: Boolean,
        val contact: ReviewContact,
        val roles: List<ReviewRole>,
        val skills: ReviewSkills,
        val education: List<ReviewEducation>,
        val profile: CandidateProfile? = null,
    ) : ReviewUiState {
        val canContinue: Boolean get() = gap == null
    }
}

internal sealed interface ReviewEvent {
    data object Continue : ReviewEvent

    data object NothingToReview : ReviewEvent

    data object MissingEndDate : ReviewEvent

    data object SaveFailed : ReviewEvent
}
