package com.tailormyresume.core.domain.coverage

import com.google.common.truth.Truth.assertThat
import com.tailormyresume.core.model.ApplicationKeywordCoverage
import com.tailormyresume.core.model.BulletDecision
import com.tailormyresume.core.model.JobRequirement
import com.tailormyresume.core.model.MatchStatus
import com.tailormyresume.core.model.QuickAnswer
import com.tailormyresume.core.model.RequirementMatch
import com.tailormyresume.core.model.RequirementPriority
import com.tailormyresume.core.model.RequirementType
import com.tailormyresume.core.model.TailoredBullet
import com.tailormyresume.core.model.TailoredResume
import org.junit.Test

class KeywordCoverageCalculatorTest {
    private fun requirement(id: String, text: String, vararg keywords: String) = JobRequirement(
        id = id,
        text = text,
        type = RequirementType.SKILL,
        priority = RequirementPriority.MUST_HAVE,
        keywords = keywords.toList(),
    )

    private fun match(requirement: JobRequirement, status: MatchStatus) = RequirementMatch(
        requirement = requirement,
        status = status,
        evidenceIds = emptyList(),
    )

    private fun bullet(
        id: String = "b1",
        originalText: String = "",
        proposedText: String = "",
        decision: BulletDecision,
    ) = TailoredBullet(
        id = id,
        entryId = "e1",
        originalText = originalText,
        proposedText = proposedText,
        sourceIds = emptyList(),
        editTypes = emptyList(),
        keywordsUsed = emptyList(),
        violations = emptyList(),
        decision = decision,
    )

    private fun resumeOf(bullet: TailoredBullet) = TailoredResume(listOf(bullet))

    private val acceptedText =
        "Built SQL and Excel models, Power BI dashboards and Python forecasting"

    private val acceptedTextWithStakeholderManagement =
        "$acceptedText and led stakeholder management for finance"

    private val stakeholderDetail =
        "Ran stakeholder management for the quarterly finance review"

    private val prototypeMatches = listOf(
        match(requirement("r1", "Strong SQL and Excel", "sql", "excel"), MatchStatus.MET),
        match(requirement("r2", "Power BI or Tableau", "power bi"), MatchStatus.MET),
        match(requirement("r3", "Python", "python"), MatchStatus.PARTIAL),
        match(requirement("r4", "Forecasting models", "forecasting"), MatchStatus.MET),
        match(
            requirement(
                "r5",
                "Financial reporting and variance analysis",
                "financial reporting",
                "variance analysis",
            ),
            MatchStatus.GAP,
        ),
        match(
            requirement("r6", "Experience presenting to senior stakeholders", "stakeholder management"),
            MatchStatus.GAP,
        ),
    )

    private val yesOnStakeholder =
        QuickAnswer("r6", "YES_REGULARLY", stakeholderDetail)

    private val acceptedResume = resumeOf(
        bullet(
            id = "b1",
            originalText = "Reworked the reporting packs",
            proposedText = acceptedText,
            decision = BulletDecision.ACCEPTED,
        ),
    )

    @Test
    fun prototypeJdAnsweredYesPlusDetailAssertsLiteralPercents() {
        val coverage = KeywordCoverageCalculator.compute(
            matches = prototypeMatches,
            quickAnswer = yesOnStakeholder,
            tailoredResume = acceptedResume,
        )

        assertThat(coverage).isEqualTo(ApplicationKeywordCoverage(now = 63, upTo = 75, final = 63))
    }

    @Test
    fun finalWithAnswerExceedsFinalWithout() {
        val withoutStakeholder = KeywordCoverageCalculator.compute(
            matches = prototypeMatches,
            quickAnswer = yesOnStakeholder,
            tailoredResume = acceptedResume,
        )
        val withStakeholder = KeywordCoverageCalculator.compute(
            matches = prototypeMatches,
            quickAnswer = yesOnStakeholder,
            tailoredResume = resumeOf(
                bullet(
                    id = "b1",
                    originalText = "Reworked the reporting packs",
                    proposedText = acceptedTextWithStakeholderManagement,
                    decision = BulletDecision.ACCEPTED,
                ),
            ),
        )

        assertThat(withStakeholder.final).isEqualTo(75)
        assertThat(withoutStakeholder.final).isEqualTo(63)
        assertThat(withStakeholder.final).isGreaterThan(withoutStakeholder.final)
    }

    @Test
    fun nowNeverExceedsUpTo() {
        val withAnswer = KeywordCoverageCalculator.compute(
            matches = prototypeMatches,
            quickAnswer = yesOnStakeholder,
            tailoredResume = acceptedResume,
        )
        val withoutAnswer = KeywordCoverageCalculator.compute(
            matches = prototypeMatches,
            quickAnswer = null,
            tailoredResume = acceptedResume,
        )
        val noResume = KeywordCoverageCalculator.compute(
            matches = prototypeMatches,
            quickAnswer = yesOnStakeholder,
            tailoredResume = null,
        )

        assertThat(withAnswer.now).isAtMost(withAnswer.upTo)
        assertThat(withAnswer.now).isEqualTo(63)
        assertThat(withAnswer.upTo).isEqualTo(75)
        assertThat(withoutAnswer.now).isEqualTo(63)
        assertThat(withoutAnswer.upTo).isEqualTo(withoutAnswer.now)
        assertThat(noResume.final).isNull()
    }

    @Test
    fun repeatedKeywordsDifferingInCaseCountOnce() {
        val matches = listOf(
            match(requirement("c1", "Strong SQL", "SQL"), MatchStatus.MET),
            match(requirement("c2", "SQL and Excel", "sql", "excel"), MatchStatus.MET),
            match(requirement("c3", "Sql reporting", "Sql"), MatchStatus.MET),
        )

        val coverage = KeywordCoverageCalculator.compute(
            matches = matches,
            quickAnswer = null,
            tailoredResume = TailoredResume(emptyList()),
        )

        assertThat(coverage.now).isEqualTo(100)
        assertThat(coverage.upTo).isEqualTo(100)
        assertThat(coverage.final).isEqualTo(0)
    }

    @Test
    fun noKeywordsGivesZeroPercents() {
        val withoutMatches = KeywordCoverageCalculator.compute(
            matches = emptyList(),
            quickAnswer = null,
            tailoredResume = TailoredResume(emptyList()),
        )
        val withKeywordlessRequirement = KeywordCoverageCalculator.compute(
            matches = listOf(match(requirement("k1", "Some nice to have"), MatchStatus.GAP)),
            quickAnswer = QuickAnswer("k1", "YES_REGULARLY", stakeholderDetail),
            tailoredResume = TailoredResume(emptyList()),
        )

        assertThat(withoutMatches.now).isEqualTo(0)
        assertThat(withoutMatches.upTo).isEqualTo(0)
        assertThat(withoutMatches.final).isEqualTo(0)
        assertThat(withKeywordlessRequirement.now).isEqualTo(0)
        assertThat(withKeywordlessRequirement.upTo).isEqualTo(0)
        assertThat(withKeywordlessRequirement.final).isEqualTo(0)
    }

    @Test
    fun upToCountsMetPartialAndAnsweredYesOrFew() {
        val matches = listOf(
            match(requirement("g1", "Advanced SQL", "sql"), MatchStatus.MET),
            match(requirement("g2", "Some Python", "python"), MatchStatus.PARTIAL),
            match(
                requirement(
                    "g3",
                    "Experience presenting to senior stakeholders",
                    "stakeholder management",
                    "forecasting",
                ),
                MatchStatus.GAP,
            ),
        )

        val coverage = KeywordCoverageCalculator.compute(
            matches = matches,
            quickAnswer = QuickAnswer("g3", "A_FEW_TIMES", stakeholderDetail),
            tailoredResume = null,
        )

        assertThat(coverage.now).isEqualTo(50)
        assertThat(coverage.upTo).isEqualTo(75)
    }

    @Test
    fun notYetAndSkippedAddNothing() {
        val notYet = KeywordCoverageCalculator.compute(
            matches = prototypeMatches,
            quickAnswer = QuickAnswer("r6", "NOT_YET", stakeholderDetail),
            tailoredResume = null,
        )
        val skipped = KeywordCoverageCalculator.compute(
            matches = prototypeMatches,
            quickAnswer = QuickAnswer("r6", "SKIPPED"),
            tailoredResume = null,
        )

        assertThat(notYet.now).isEqualTo(63)
        assertThat(notYet.upTo).isEqualTo(notYet.now)
        assertThat(skipped.now).isEqualTo(63)
        assertThat(skipped.upTo).isEqualTo(skipped.now)
    }

    @Test
    fun gapRequirementKeywordsOnlyViaAnswer() {
        val confirmed = KeywordCoverageCalculator.compute(
            matches = prototypeMatches,
            quickAnswer = yesOnStakeholder,
            tailoredResume = null,
        )
        val unknownRequirement = KeywordCoverageCalculator.compute(
            matches = prototypeMatches,
            quickAnswer = QuickAnswer("r99", "YES_REGULARLY", stakeholderDetail),
            tailoredResume = null,
        )

        assertThat(confirmed.now).isEqualTo(63)
        assertThat(confirmed.upTo).isEqualTo(75)
        assertThat(unknownRequirement.now).isEqualTo(63)
        assertThat(unknownRequirement.upTo).isEqualTo(unknownRequirement.now)
    }

    @Test
    fun finalCountsAcceptedTextOnly() {
        val coverage = KeywordCoverageCalculator.compute(
            matches = prototypeMatches,
            quickAnswer = null,
            tailoredResume = TailoredResume(
                listOf(
                    bullet(
                        id = "b1",
                        originalText = "Reworked the reporting packs",
                        proposedText = "Built SQL and Excel models",
                        decision = BulletDecision.ACCEPTED,
                    ),
                    bullet(
                        id = "b2",
                        originalText = "Owned a reporting pipeline",
                        proposedText = "Power BI dashboards and Python forecasting",
                        decision = BulletDecision.PENDING,
                    ),
                ),
            ),
        )

        assertThat(coverage.final).isEqualTo(25)
    }

    @Test
    fun rejectedChangeContributesOriginalText() {
        val coverage = KeywordCoverageCalculator.compute(
            matches = prototypeMatches,
            quickAnswer = null,
            tailoredResume = resumeOf(
                bullet(
                    id = "b1",
                    originalText = "Built SQL and Excel models",
                    proposedText = "Rebuilt dashboards in Power BI and Python forecasting",
                    decision = BulletDecision.REJECTED,
                ),
            ),
        )

        assertThat(coverage.final).isEqualTo(25)
    }

    @Test
    fun pendingChangeContributesOriginalText() {
        val coverage = KeywordCoverageCalculator.compute(
            matches = prototypeMatches,
            quickAnswer = null,
            tailoredResume = resumeOf(
                bullet(
                    id = "b1",
                    originalText = "Built SQL and Excel models",
                    proposedText = "Rebuilt dashboards in Power BI and Python forecasting",
                    decision = BulletDecision.PENDING,
                ),
            ),
        )

        assertThat(coverage.final).isEqualTo(25)
    }

    @Test
    fun blankDetailAnswerWithoutNamedSkillConfirmsNothing() {
        val experience = JobRequirement(
            id = "x1",
            text = "Experience presenting to senior stakeholders",
            type = RequirementType.EXPERIENCE,
            priority = RequirementPriority.MUST_HAVE,
            keywords = listOf("stakeholder management"),
        )
        val matches = listOf(
            match(requirement("r1", "Strong SQL", "sql"), MatchStatus.MET),
            match(experience, MatchStatus.GAP),
        )

        val coverage = KeywordCoverageCalculator.compute(matches, QuickAnswer("x1", "A_FEW_TIMES"), null)

        assertThat(coverage.now).isEqualTo(50)
        assertThat(coverage.upTo).isEqualTo(50)
    }

    @Test
    fun blankDetailAnswerConfirmsNamedSkillKeywords() {
        val matches = listOf(
            match(requirement("r1", "Strong SQL", "sql"), MatchStatus.MET),
            match(requirement("r2", "Python", "python"), MatchStatus.GAP),
        )

        val coverage = KeywordCoverageCalculator.compute(matches, QuickAnswer("r2", "YES_REGULARLY"), null)

        assertThat(coverage.now).isEqualTo(50)
        assertThat(coverage.upTo).isEqualTo(100)
    }

    private fun experienceRequirement(id: String, text: String, vararg keywords: String) = JobRequirement(
        id = id,
        text = text,
        type = RequirementType.EXPERIENCE,
        priority = RequirementPriority.MUST_HAVE,
        keywords = keywords.toList(),
    )

    private fun upToWithAnswer(requirement: JobRequirement, detail: String): Int {
        val matches = listOf(
            match(requirement("r1", "Strong SQL", "sql"), MatchStatus.MET),
            match(requirement, MatchStatus.GAP),
        )
        return KeywordCoverageCalculator.compute(matches, QuickAnswer(requirement.id, "YES_REGULARLY", detail), null).upTo
    }

    @Test
    fun placeholderDetailDoesNotLetRequirementTextConfirmKeyword() {
        val stakeholders = experienceRequirement("x1", "Experience presenting to senior stakeholders", "stakeholders")

        assertThat(upToWithAnswer(stakeholders, "n/a")).isEqualTo(50)
    }

    @Test
    fun detailStatingTheKeywordConfirmsIt() {
        val stakeholders = experienceRequirement("x1", "Experience presenting to senior stakeholders", "stakeholders")

        assertThat(upToWithAnswer(stakeholders, "Presented monthly results to stakeholders")).isEqualTo(100)
    }

    @Test
    fun powerBiIsConfirmedAsCanonicalAndMatchesAnswerFactsWhenDisplayCased() {
        val canonical = requirement("p1", "Power BI or Tableau", "power bi")
        val displayCased = requirement("p2", "Power BI or Tableau", "Power BI")

        assertThat(upToWithAnswer(canonical, "built Power BI dashboards")).isEqualTo(100)
        assertThat(upToWithAnswer(canonical, "")).isEqualTo(100)
        assertThat(upToWithAnswer(displayCased, "built Power BI dashboards")).isEqualTo(100)
    }

    @Test
    fun blankDetailStillConfirmsNamedSkills() {
        val matches = listOf(
            match(requirement("r1", "Strong SQL", "sql"), MatchStatus.GAP),
            match(requirement("r2", "Python", "python"), MatchStatus.GAP),
        )

        val sql = KeywordCoverageCalculator.compute(matches, QuickAnswer("r1", "YES_REGULARLY", " "), null)
        val python = KeywordCoverageCalculator.compute(matches, QuickAnswer("r2", "A_FEW_TIMES"), null)

        assertThat(sql.upTo).isEqualTo(50)
        assertThat(python.upTo).isEqualTo(50)
    }

    @Test
    fun skillKeywordAbsentFromRequirementTextIsNotConfirmedByAnyAnswer() {
        val forecasting = requirement("f1", "Experience presenting to senior stakeholders", "forecasting")

        assertThat(upToWithAnswer(forecasting, "")).isEqualTo(50)
        assertThat(upToWithAnswer(forecasting, "n/a")).isEqualTo(50)
    }
}
