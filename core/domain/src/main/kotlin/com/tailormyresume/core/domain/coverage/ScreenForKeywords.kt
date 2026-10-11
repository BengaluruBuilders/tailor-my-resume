package com.tailormyresume.core.domain.coverage

import com.tailormyresume.core.domain.offline.SkillLexicon
import com.tailormyresume.core.model.CandidateProfile
import com.tailormyresume.core.model.RequirementMatch

data class KeywordScreen(val have: List<String>, val missing: List<String>) {
    val count: Int get() = have.size + missing.size
}

object ScreenForKeywords {
    operator fun invoke(matches: List<RequirementMatch>, profile: CandidateProfile? = null): KeywordScreen {
        val evidence = evidenceTextsOf(profile)
        val have = matches.flatMap { it.presentKeywords(evidence) }.distinctBy(String::lowercase)
        val haveKeys = have.map(String::lowercase).toSet()
        val missing = matches.flatMap { it.requirement.keywords }
            .map(SkillLexicon::displayName)
            .distinctBy(String::lowercase)
            .filter { it.lowercase() !in haveKeys }
        return KeywordScreen(have = have, missing = missing)
    }
}
