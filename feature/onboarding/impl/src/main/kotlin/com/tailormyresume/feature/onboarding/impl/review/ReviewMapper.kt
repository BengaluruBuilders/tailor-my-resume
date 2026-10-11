package com.tailormyresume.feature.onboarding.impl.review

import com.tailormyresume.core.model.CandidateProfile

internal fun CandidateProfile.toReviewUiState(): ReviewUiState.Ready =
    ReviewUiState.Ready(
        sourceFileName = null,
        percent = 0,
        gap = null,
        showLinkedinRow = false,
        contact = ReviewContact("", "", ""),
        roles = emptyList(),
        skills = ReviewSkills(0, emptyList(), 0),
        education = emptyList(),
    )
