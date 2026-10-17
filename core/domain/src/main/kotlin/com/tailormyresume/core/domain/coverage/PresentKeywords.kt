package com.tailormyresume.core.domain.coverage

import com.tailormyresume.core.domain.keywordsStatedIn
import com.tailormyresume.core.domain.offline.EvidenceSources
import com.tailormyresume.core.domain.offline.SkillLexicon
import com.tailormyresume.core.model.CandidateProfile
import com.tailormyresume.core.model.MatchStatus
import com.tailormyresume.core.model.RequirementMatch
import com.tailormyresume.core.model.confirmedWithinLimits

internal fun evidenceTextsOf(profile: CandidateProfile?): Map<String, String>? {
    val sendable = profile?.confirmedWithinLimits() ?: return null
    val fromEntries = sendable.entries.flatMap { entry ->
        listOf(entry.id to entry.title) + entry.bullets.map { it.id to it.text }
    }
    return (fromEntries + sendable.skills.map { "${EvidenceSources.SKILL_ID_PREFIX}$it" to it }).toMap()
}

internal fun RequirementMatch.presentKeywords(evidenceTexts: Map<String, String>?): List<String> {
    val cited = evidenceIds.mapNotNull { evidenceTexts?.get(it) }
    val stated = cited.takeIf { it.isNotEmpty() }?.let { keywordsStatedIn(requirement, it.joinToString("\n")) }
    val trustsStatus = evidenceTexts == null || evidenceIds.isEmpty()
    return when (status) {
        MatchStatus.MET -> stated ?: if (trustsStatus) requirement.keywords.map(SkillLexicon::displayName) else emptyList()
        MatchStatus.PARTIAL -> stated.orEmpty()
        MatchStatus.GAP -> emptyList()
    }
}
