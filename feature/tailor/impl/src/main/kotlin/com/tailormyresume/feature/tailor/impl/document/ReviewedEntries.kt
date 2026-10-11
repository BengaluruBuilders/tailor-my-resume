package com.tailormyresume.feature.tailor.impl.document

import com.tailormyresume.core.model.CandidateProfile
import com.tailormyresume.core.model.ProfileEntry
import com.tailormyresume.core.model.TailoredResume

internal fun reviewedEntries(profile: CandidateProfile, resume: TailoredResume): List<ProfileEntry> {
    val tailoredByEntry = resume.bullets.groupBy { it.entryId }
    val recordedIds = resume.entryIds?.toSet()
    return profile.entries.filter { it.isConfirmed }.mapNotNull { entry ->
        val tailored = tailoredByEntry[entry.id]
        val recorded = recordedIds == null || entry.id in recordedIds
        if (tailored == null && !recorded) return@mapNotNull null
        val sourceIds = tailored?.flatMap { it.sourceIds }?.toSet().orEmpty()
        val bullets = if (tailored == null) entry.bullets else entry.bullets.filter { it.id in sourceIds }
        entry.copy(bullets = bullets)
    }
}
