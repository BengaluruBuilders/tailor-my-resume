package com.tailormyresume.core.domain.coverage

import com.tailormyresume.core.domain.offline.SkillLexicon
import com.tailormyresume.core.model.MatchStatus
import com.tailormyresume.core.model.RequirementMatch

data class KeywordScreen(val have: List<String>, val missing: List<String>) {
    val count: Int get() = have.size + missing.size
}

object ScreenForKeywords {
    operator fun invoke(matches: List<RequirementMatch>): KeywordScreen {
        val (haveMatches, missingMatches) = matches.partition {
            it.status == MatchStatus.MET || it.status == MatchStatus.PARTIAL
        }
        val have = distinctDisplayNames(haveMatches)
        val haveKeys = have.map(String::lowercase).toSet()
        val missing = distinctDisplayNames(missingMatches).filter { it.lowercase() !in haveKeys }
        return KeywordScreen(have = have, missing = missing)
    }

    private fun distinctDisplayNames(matches: List<RequirementMatch>): List<String> =
        matches.flatMap { it.requirement.keywords }
            .map(SkillLexicon::displayName)
            .distinctBy(String::lowercase)
}
