package com.tailormyresume.core.domain.coverage

import com.google.common.truth.Truth.assertThat
import com.tailormyresume.core.model.JobRequirement
import com.tailormyresume.core.model.MatchStatus
import com.tailormyresume.core.model.RequirementMatch
import com.tailormyresume.core.model.RequirementPriority
import com.tailormyresume.core.model.RequirementType
import org.junit.Test

class ScreenForKeywordsTest {
    private fun match(id: String, status: MatchStatus, vararg keywords: String) = RequirementMatch(
        requirement = JobRequirement(
            id = id,
            text = id,
            type = RequirementType.SKILL,
            priority = RequirementPriority.MUST_HAVE,
            keywords = keywords.toList(),
        ),
        status = status,
        evidenceIds = emptyList(),
    )

    @Test
    fun prototypeJdHasFiveHaveThreeMissingCountEight() {
        val screen = ScreenForKeywords(
            listOf(
                match("r1", MatchStatus.MET, "sql", "excel"),
                match("r2", MatchStatus.MET, "power bi"),
                match("r3", MatchStatus.PARTIAL, "python"),
                match("r4", MatchStatus.MET, "forecasting"),
                match("r5", MatchStatus.GAP, "financial reporting", "variance analysis"),
                match("r6", MatchStatus.GAP, "stakeholder management"),
            ),
        )

        assertThat(screen.have).containsExactly("SQL", "Excel", "Power BI", "Python", "Forecasting").inOrder()
        assertThat(screen.missing)
            .containsExactly("financial reporting", "variance analysis", "Stakeholder Management").inOrder()
        assertThat(screen.count).isEqualTo(8)
    }

    @Test
    fun aKeywordMetElsewhereIsNotMissing() {
        val screen = ScreenForKeywords(
            listOf(
                match("r1", MatchStatus.GAP, "sql"),
                match("r2", MatchStatus.MET, "SQL"),
            ),
        )

        assertThat(screen.have).containsExactly("SQL")
        assertThat(screen.missing).isEmpty()
        assertThat(screen.count).isEqualTo(1)
    }
}
