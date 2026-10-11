package com.tailormyresume.core.domain.coverage

import com.google.common.truth.Truth.assertThat
import com.tailormyresume.core.model.BulletDecision
import com.tailormyresume.core.model.CandidateProfile
import com.tailormyresume.core.model.EntryCategory
import com.tailormyresume.core.model.EvidenceBullet
import com.tailormyresume.core.model.FactSource
import com.tailormyresume.core.model.JobRequirement
import com.tailormyresume.core.model.MatchStatus
import com.tailormyresume.core.model.ProfileEntry
import com.tailormyresume.core.model.RequirementMatch
import com.tailormyresume.core.model.RequirementPriority
import com.tailormyresume.core.model.RequirementType
import com.tailormyresume.core.model.TailoredResume
import com.tailormyresume.core.model.TailoredSkills
import com.tailormyresume.core.model.TailoredText
import org.junit.Test

class ScreenForKeywordsTest {
    private val profile = CandidateProfile("Candidate", "", "", "", listOf("sql", "excel", "power bi", "python", "forecasting", "SQL"), emptyList())

    private fun match(id: String, status: MatchStatus, vararg keywords: String) = RequirementMatch(
        requirement = JobRequirement(
            id = id,
            text = id,
            type = RequirementType.SKILL,
            priority = RequirementPriority.MUST_HAVE,
            keywords = keywords.toList(),
        ),
        status = status,
        evidenceIds = keywords.map { "skill:" + it },
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
            profile,
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
            profile,
        )

        assertThat(screen.have).containsExactly("SQL")
        assertThat(screen.missing).isEmpty()
        assertThat(screen.count).isEqualTo(1)
    }

    @Test
    fun partialRequirementDoesNotPromoteMissingKeyword() {
        val matches = listOf(match("r1", MatchStatus.PARTIAL, "sql", "python").copy(evidenceIds = listOf("b1")))
        val source = CandidateProfile(
            "Candidate",
            "",
            "",
            "",
            emptyList(),
            listOf(
                ProfileEntry(
                    "e1", EntryCategory.EXPERIENCE, "Analyst", "Company", "2020", "2021",
                    listOf(EvidenceBullet("b1", "Used SQL to build reports")), FactSource.IMPORTED, true,
                ),
            ),
        )
        val screen = ScreenForKeywords(matches, source)
        assertThat(screen.have).containsExactly("SQL")
        assertThat(screen.missing).containsExactly("Python")
        val coverage = KeywordCoverageCalculator.compute(matches, null, null, source)
        assertThat(coverage.now).isEqualTo(50)
        assertThat(coverage.upTo).isEqualTo(50)
    }

    @Test
    fun partialRequirementWithoutCitedEvidenceIsMissing() {
        val matches = listOf(match("r1", MatchStatus.PARTIAL, "sql", "python").copy(evidenceIds = emptyList()))

        val screen = ScreenForKeywords(matches, profile)

        assertThat(screen.have).isEmpty()
        assertThat(screen.missing).containsExactly("SQL", "Python").inOrder()
    }

    @Test
    fun metRequirementWithUnknownCitedEvidenceIsMissing() {
        val matches = listOf(match("r1", MatchStatus.MET, "python").copy(evidenceIds = listOf("unknown")))

        val screen = ScreenForKeywords(matches, profile)

        assertThat(screen.have).isEmpty()
        assertThat(screen.missing).containsExactly("Python")
    }

    @Test
    fun missingUnconfirmedOrAnswerEvidenceDoesNotClaimResumeKeyword() {
        val entry = ProfileEntry(
            "e1", EntryCategory.EXPERIENCE, "Analyst", "Company", "2020", "2021",
            listOf(EvidenceBullet("b1", "Used Python")), FactSource.IMPORTED, true,
        )
        val matches = listOf(match("r1", MatchStatus.MET, "python").copy(evidenceIds = listOf("b1")))
        listOf(emptyList(), listOf(entry.copy(isConfirmed = false)), listOf(entry.copy(source = FactSource.USER_ANSWER))).forEach { entries ->
            val source = CandidateProfile("Candidate", "", "", "", emptyList(), entries)
            assertThat(ScreenForKeywords(matches, source).have).isEmpty()
            assertThat(KeywordCoverageCalculator.compute(matches, null, null, source).now).isEqualTo(0)
        }
    }

    @Test
    fun evidenceAliasesUseSharedTokenMatcher() {
        val source = CandidateProfile("Candidate", "", "", "", listOf("Structured Query Language"), emptyList())
        val matches = listOf(match("r1", MatchStatus.PARTIAL, "sql", "python").copy(evidenceIds = listOf("skill:Structured Query Language")))
        assertThat(ScreenForKeywords(matches, source).have).containsExactly("SQL")
    }

    @Test
    fun finalCoverageIncludesAcceptedSummaryAndSkillsEdits() {
        val matches = listOf(match("r1", MatchStatus.GAP, "sql", "python"))
        val source = CandidateProfile("Candidate", "", "", "", emptyList(), emptyList())
        val resume = TailoredResume(
            bullets = emptyList(),
            summary = TailoredText("Built SQL reports", "", decision = BulletDecision.ACCEPTED),
            skills = TailoredSkills(listOf("Python"), emptyList(), decision = BulletDecision.ACCEPTED),
        )
        assertThat(KeywordCoverageCalculator.compute(matches, null, resume, source).final).isEqualTo(100)
        val cleared = resume.copy(summary = resume.summary?.copy(text = ""), skills = resume.skills?.copy(skills = emptyList()))
        assertThat(KeywordCoverageCalculator.compute(matches, null, cleared, source).final).isEqualTo(0)
    }

    @Test
    fun finalCoverageExcludesEntriesThatAreNotInTheTailoredResume() {
        val included = ProfileEntry(
            "included", EntryCategory.EXPERIENCE, "Analyst", "Company", "2020", "2021",
            listOf(EvidenceBullet("included-b1", "Built SQL reports")), FactSource.IMPORTED, true,
        )
        val excluded = ProfileEntry(
            "excluded", EntryCategory.PROJECT, "Python project", "", "", "",
            listOf(EvidenceBullet("excluded-b1", "Built Python tools")), FactSource.IMPORTED, true,
        )
        val source = CandidateProfile("Candidate", "", "", "", emptyList(), listOf(included, excluded))
        val matches = listOf(match("r1", MatchStatus.GAP, "python"))
        val resume = TailoredResume(bullets = emptyList(), entryIds = listOf(included.id))

        val coverage = KeywordCoverageCalculator.compute(matches, null, resume, source)

        assertThat(coverage.final).isEqualTo(0)
    }
}
