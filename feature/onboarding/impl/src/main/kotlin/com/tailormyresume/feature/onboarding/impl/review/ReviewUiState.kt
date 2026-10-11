package com.tailormyresume.feature.onboarding.impl.review

internal sealed interface ReviewDates {
    data object None : ReviewDates

    data class Range(val start: String, val end: String) : ReviewDates

    data class Current(val start: String) : ReviewDates

    data class EndMissing(val start: String) : ReviewDates
}

internal data class ReviewGap(val entryId: String, val title: String)

internal data class ReviewContact(val name: String, val email: String, val phoneAndCity: String)

internal data class ReviewRole(val title: String, val organization: String, val dates: ReviewDates)

internal data class ReviewEducation(val title: String, val detail: String)

internal data class ReviewSkills(val count: Int, val chips: List<String>, val more: Int)

internal sealed interface ReviewUiState {
    data object Loading : ReviewUiState

    data class Ready(
        val sourceFileName: String?,
        val percent: Int,
        val gap: ReviewGap?,
        val showLinkedinRow: Boolean,
        val contact: ReviewContact,
        val roles: List<ReviewRole>,
        val skills: ReviewSkills,
        val education: List<ReviewEducation>,
    ) : ReviewUiState {
        val canContinue: Boolean get() = gap == null
    }
}

internal sealed interface ReviewEvent {
    data object Continue : ReviewEvent

    data object NothingToReview : ReviewEvent

    data object MissingEndDate : ReviewEvent
}
