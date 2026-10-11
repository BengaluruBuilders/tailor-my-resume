package com.tailormyresume.core.domain.coverage

import com.tailormyresume.core.domain.keywordsStatedIn
import com.tailormyresume.core.domain.offline.SkillLexicon
import com.tailormyresume.core.model.CandidateProfile
import com.tailormyresume.core.model.MatchStatus
import com.tailormyresume.core.model.ProfileLimits
import com.tailormyresume.core.model.RequirementMatch
import com.tailormyresume.core.model.sendableFacts

internal fun evidencedKeywords(matches: List<RequirementMatch>, profile: CandidateProfile): Set<String> {
    val confirmed = profile.sendableFacts()
    val evidence = buildMap<String, List<String>> {
        confirmed.entries.forEach { entry ->
            put(entry.id, listOf(entry.title) + entry.bullets.map { it.text })
            entry.bullets.forEach { put(it.id, listOf(it.text)) }
        }
        confirmed.skills.forEach { put(ProfileLimits.SKILL_ID_PREFIX + it, listOf(SkillLexicon.displayName(it))) }
    }
    return matches.filter { it.status != MatchStatus.GAP }.flatMap { match ->
        match.evidenceIds.flatMap { id ->
            evidence[id].orEmpty().flatMap { keywordsStatedIn(match.requirement, it) }
        }
    }.map(String::lowercase).toSet()
}
