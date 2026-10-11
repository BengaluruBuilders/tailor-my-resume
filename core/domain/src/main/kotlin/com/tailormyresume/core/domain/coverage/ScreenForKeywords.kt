package com.tailormyresume.core.domain.coverage

import com.tailormyresume.core.domain.offline.SkillLexicon
import com.tailormyresume.core.model.CandidateProfile
import com.tailormyresume.core.model.RequirementMatch

data class KeywordScreen(val have: List<String>, val missing: List<String>) {
    val count: Int get() = have.size + missing.size
}

object ScreenForKeywords {
    operator fun invoke(matches: List<RequirementMatch>, profile: CandidateProfile): KeywordScreen {
        val haveKeys = evidencedKeywords(matches, profile)
        val (have, missing) = distinctDisplayNames(matches).partition { it.lowercase() in haveKeys }
        return KeywordScreen(have = have, missing = missing)
    }

    private fun distinctDisplayNames(matches: List<RequirementMatch>): List<String> =
        matches.flatMap { it.requirement.keywords }
            .map(SkillLexicon::displayName)
            .distinctBy(String::lowercase)
}
