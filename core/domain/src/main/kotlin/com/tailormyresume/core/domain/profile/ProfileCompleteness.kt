package com.tailormyresume.core.domain.profile

import com.tailormyresume.core.model.CandidateProfile

object ProfileCompleteness {
    private const val FULL = 100
    private const val MISSING_LINKEDIN = 4
    private const val PER_REQUIRED_GAP = 6

    fun percent(profile: CandidateProfile): Int {
        val linkedinPenalty = if (profile.linkedinUrl.isBlank()) MISSING_LINKEDIN else 0
        return (FULL - linkedinPenalty - PER_REQUIRED_GAP * RequiredGaps.of(profile).size).coerceAtLeast(0)
    }
}
