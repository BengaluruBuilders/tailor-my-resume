package com.tailormyresume.core.domain.coverage

import com.google.common.truth.Truth.assertThat
import com.tailormyresume.core.domain.TailorResumeUseCase
import com.tailormyresume.core.domain.offline.OfflineFabricationGuard
import com.tailormyresume.core.domain.offline.OfflineGapMatcher
import com.tailormyresume.core.domain.offline.OfflineJobDescriptionAnalyzer
import com.tailormyresume.core.domain.offline.OfflineResumeTailor
import com.tailormyresume.core.domain.offline.entry
import com.tailormyresume.core.domain.offline.jobDescriptionResources
import com.tailormyresume.core.domain.offline.profileOf
import com.tailormyresume.core.domain.offline.resourceText
import com.tailormyresume.core.domain.offline.sampleProfile
import com.tailormyresume.core.model.CandidateProfile
import com.tailormyresume.core.model.EntryCategory
import com.tailormyresume.core.model.JobRequirement
import com.tailormyresume.core.model.MatchStatus
import com.tailormyresume.core.model.QuickAnswer
import com.tailormyresume.core.model.RequirementMatch
import com.tailormyresume.core.model.RequirementPriority
import com.tailormyresume.core.model.RequirementType
import kotlinx.coroutines.test.runTest
import org.junit.Test

class HonestCoverageTest {
    private val analyzer = OfflineJobDescriptionAnalyzer()
    private val matcher = OfflineGapMatcher()
    private val tailorUseCase = TailorResumeUseCase(OfflineResumeTailor(), OfflineFabricationGuard())

    private val evidenceProfile: CandidateProfile = profileOf(
        emptyList(),
        entry("exp-1", EntryCategory.EXPERIENCE, "Analyst", "Built dashboards in SQL for the finance team"),
    )

    private fun partial(vararg keywords: String, evidenceIds: List<String>) = RequirementMatch(
        requirement = JobRequirement(
            id = "r1",
            text = "SQL and Python",
            type = RequirementType.SKILL,
            priority = RequirementPriority.MUST_HAVE,
            keywords = keywords.toList(),
        ),
        status = MatchStatus.PARTIAL,
        evidenceIds = evidenceIds,
    )

    @Test
    fun partialKeywordCountsAsPresentOnlyWhenTheCitedEvidenceStatesIt() {
        val matches = listOf(partial("sql", "python", evidenceIds = listOf("exp-1-b1")))

        val screen = ScreenForKeywords(matches, evidenceProfile)

        assertThat(screen.have).containsExactly("SQL")
        assertThat(screen.missing).containsExactly("Python")
    }

    @Test
    fun partialKeywordWithNoCitedEvidenceIsMissing() {
        val screen = ScreenForKeywords(listOf(partial("sql", "python", evidenceIds = emptyList())), evidenceProfile)

        assertThat(screen.have).isEmpty()
        assertThat(screen.missing).containsExactly("SQL", "Python").inOrder()
    }

    @Test
    fun partialKeywordMatchesWholeTokensOnly() {
        val profile = profileOf(
            emptyList(),
            entry("exp-1", EntryCategory.EXPERIENCE, "Analyst", "Led go-to-market launches for R&D tools"),
        )

        val screen = ScreenForKeywords(listOf(partial("go", "r", evidenceIds = listOf("exp-1-b1"))), profile)

        assertThat(screen.have).isEmpty()
    }

    @Test
    fun coverageNumberUsesTheSamePartialRuleAsTheKeywordScreen() {
        val matches = listOf(partial("sql", "python", evidenceIds = listOf("exp-1-b1")))

        val coverage = KeywordCoverageCalculator.compute(matches, null, null, evidenceProfile)

        assertThat(coverage.now).isEqualTo(50)
        assertThat(coverage.upTo).isEqualTo(50)
    }

    @Test
    fun finalCountsKeywordsStatedAnywhereOnTheResumeNotOnlyInBullets() = runTest {
        val job = analyzer.analyze(resourceText("jd_data_analyst.txt"))
        val gap = matcher.match(sampleProfile, job)
        val tailored = tailorUseCase(sampleProfile, job, gap, "app-1", "run-1").withPendingAccepted()

        val coverage = KeywordCoverageCalculator.compute(gap.matches, null, tailored, sampleProfile)

        assertThat(coverage.final).isEqualTo(coverage.now)
    }

    @Test
    fun acceptingEveryChangeReachesTheUpToPromiseOnEachSampleJob() = runTest {
        jobDescriptionResources.forEach { resource ->
            val job = analyzer.analyze(resourceText(resource))
            val gap = matcher.match(sampleProfile, job)
            val answered = gap.matches.firstOrNull { it.status == MatchStatus.GAP }
                ?.let { QuickAnswer(it.requirement.id, "YES_REGULARLY", "") }

            listOf(null, answered).forEach { answer ->
                val promise = KeywordCoverageCalculator.compute(gap.matches, answer, null, sampleProfile)
                val tailored = tailorUseCase(sampleProfile, job, gap, "app-1", "run-1", answer).withPendingAccepted()
                val delivered = KeywordCoverageCalculator.compute(gap.matches, answer, tailored, sampleProfile)

                assertThat(promise.now).isAtMost(promise.upTo)
                assertThat(delivered.now).isEqualTo(promise.now)
                assertThat(delivered.upTo).isEqualTo(promise.upTo)
                assertThat(delivered.final).isEqualTo(promise.upTo)
            }
        }
    }

    @Test
    fun metKeywordWhoseOnlyCitedBulletWasDeletedIsMissingAndDropsTheNumber() {
        val met = RequirementMatch(
            requirement = JobRequirement(
                id = "r1",
                text = "Python",
                type = RequirementType.SKILL,
                priority = RequirementPriority.MUST_HAVE,
                keywords = listOf("python"),
            ),
            status = MatchStatus.MET,
            evidenceIds = listOf("exp-1-b9"),
        )

        val screen = ScreenForKeywords(listOf(met), evidenceProfile)
        val coverage = KeywordCoverageCalculator.compute(listOf(met), null, null, evidenceProfile)

        assertThat(screen.have).isEmpty()
        assertThat(screen.missing).containsExactly("Python")
        assertThat(coverage.now).isEqualTo(0)
        assertThat(coverage.upTo).isEqualTo(0)
    }
}
