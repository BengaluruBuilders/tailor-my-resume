package com.tailormyresume.core.domain.profile

import com.tailormyresume.core.model.CandidateProfile
import com.tailormyresume.core.model.EntryCategory

data class RequiredGap(val entryId: String, val title: String)

object RequiredGaps {
    fun of(profile: CandidateProfile): List<RequiredGap> = profile.entries
        .filter { it.category == EntryCategory.EXPERIENCE && it.startDate.isNotBlank() && it.endDate.isBlank() }
        .map { RequiredGap(entryId = it.id, title = it.title) }
}
