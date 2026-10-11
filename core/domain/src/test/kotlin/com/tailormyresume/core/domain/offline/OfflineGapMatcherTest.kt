package com.tailormyresume.core.domain.offline

import com.google.common.truth.Truth.assertThat
import com.tailormyresume.core.model.CandidateProfile
import com.tailormyresume.core.model.EntryCategory
import com.tailormyresume.core.model.GapAnalysis
import com.tailormyresume.core.model.JobDescription
import com.tailormyresume.core.model.MatchStatus
import com.tailormyresume.core.model.RequirementMatch
import kotlinx.coroutines.test.runTest
import org.junit.Test

class OfflineGapMatcherTest {
    private val analyzer = OfflineJobDescriptionAnalyzer()
    private val matcher = OfflineGapMatcher()

    private suspend fun analyse(jd: String, profile: CandidateProfile = sampleProfile): Pair<JobDescription, GapAnalysis> {
        val job = analyzer.analyze(jd)
        return job to matcher.match(profile, job)
    }

    private fun GapAnalysis.forKeyword(keyword: String): RequirementMatch =
        matches.first { keyword in it.requirement.keywords }

    @Test
    fun trapJobMarksMissingSkillsAsGap() = runTest {
        val (_, gap) = analyse(resourceText("jd_trap_cloud.txt"))
        listOf("mba", "kubernetes", "aws", "terraform").forEach {
            assertThat(gap.forKeyword(it).status).isEqualTo(MatchStatus.GAP)
            assertThat(gap.forKeyword(it).evidenceIds).isEmpty()
        }
        assertThat(gap.forKeyword("kotlin").status).isEqualTo(MatchStatus.MET)
    }

    @Test
    fun unconfirmedEntriesAreNotEvidence() = runTest {
        val (_, gap) = analyse("Requirements\n- Experience with Kubernetes on AWS")
        assertThat(gap.matches.single().status).isEqualTo(MatchStatus.GAP)
    }

    @Test
    fun confirmedEntryBecomesEvidence() = runTest {
        val confirmed = sampleProfile.copy(
            entries = sampleProfile.entries.map { if (it.id == "unconfirmed-1") it.copy(isConfirmed = true) else it },
        )
        val (_, gap) = analyse("Requirements\n- Experience with Kubernetes on AWS", confirmed)
        val match = gap.matches.single()
        assertThat(match.status).isEqualTo(MatchStatus.MET)
        assertThat(match.evidenceIds).containsExactly("unconfirmed-1", "unconfirmed-1-b1").inOrder()
    }

    @Test
    fun allKeywordsEvidencedIsMetAndSomeIsPartial() = runTest {
        val (_, gap) = analyse("Requirements\n- Kotlin and Spring Boot\n- Python and Kubernetes\n- Kubernetes")
        assertThat(gap.matches.map { it.status })
            .containsExactly(MatchStatus.MET, MatchStatus.PARTIAL, MatchStatus.GAP).inOrder()
    }

    @Test
    fun evidenceIdsReferenceBulletsEntryTitlesAndSkills() = runTest {
        val (_, gap) = analyse("Requirements\n- Kotlin\n- Jetpack Compose\n- Git")
        assertThat(gap.matches[0].evidenceIds).containsExactly("exp-1-b1", "skill:Kotlin").inOrder()
        assertThat(gap.matches[1].evidenceIds).containsExactly("proj-1-b1")
        assertThat(gap.matches[2].evidenceIds).containsExactly("exp-1-b3", "skill:Git").inOrder()
    }

    @Test
    fun entryTitleCountsAsEvidenceAndCitesTheEntryId() = runTest {
        val (_, gap) = analyse("Requirements\n- Experience with Android")
        assertThat(gap.matches.single().evidenceIds).containsAtLeast("proj-1", "proj-1-b1")
    }

    @Test
    fun profileSkillAloneIsEvidence() = runTest {
        val profile = profileOf(listOf("K8s"))
        val (_, gap) = analyse("Requirements\n- Kubernetes", profile)
        assertThat(gap.matches.single().status).isEqualTo(MatchStatus.MET)
        assertThat(gap.matches.single().evidenceIds).containsExactly("skill:K8s")
    }

    @Test
    fun aliasesInEvidenceMatchCanonicalRequirementKeywords() = runTest {
        val (_, gap) = analyse("Requirements\n- JavaScript\n- Excel")
        assertThat(gap.matches.map { it.status }).containsExactly(MatchStatus.MET, MatchStatus.MET)
    }

    @Test
    fun genericDegreeIsMetByASpecificDegree() = runTest {
        val (_, gap) = analyse("Requirements\n- Bachelor's degree in Computer Science")
        assertThat(gap.matches.single().status).isEqualTo(MatchStatus.MET)
    }

    @Test
    fun educationAlternativesNeedOnlyOneMatch() = runTest {
        val (_, gap) = analyse("Requirements\n- B.Tech / B.E. / BCA in Computer Science")
        assertThat(gap.matches.single().status).isEqualTo(MatchStatus.MET)
    }

    @Test
    fun missingSpecificDegreeIsGap() = runTest {
        val (_, gap) = analyse("Requirements\n- MBA in Finance")
        assertThat(gap.forKeyword("mba").status).isEqualTo(MatchStatus.GAP)
    }

    @Test
    fun keywordCoverageCountsDistinctKeywordsAcrossRequirements() = runTest {
        val (_, gap) = analyse("Requirements\n- Kotlin and Kubernetes\n- Kotlin and Terraform\n- Git")
        assertThat(gap.keywordCoverage.total).isEqualTo(4)
        assertThat(gap.keywordCoverage.covered).isEqualTo(2)
    }

    @Test
    fun matchesKeepAnalyzerOrder() = runTest {
        val (job, gap) = analyse(resourceText("jd_android.txt"))
        assertThat(gap.matches.map { it.requirement }).containsExactlyElementsIn(job.requirements).inOrder()
    }

    @Test
    fun requirementWithoutKeywordsIsGapWithoutEvidence() = runTest {
        val (_, gap) = analyse("Requirements\n- Should have 2 years of experience")
        assertThat(gap.matches.single().status).isEqualTo(MatchStatus.GAP)
        assertThat(gap.matches.single().evidenceIds).isEmpty()
        assertThat(gap.keywordCoverage.total).isEqualTo(0)
    }

    @Test
    fun nonLexiconKeywordsMatchByStem() = runTest {
        val profile = profileOf(
            emptyList(),
            entry("e1", EntryCategory.EXPERIENCE, "Support Intern", "Handled customer complaints over phone"),
        )
        val (_, gap) = analyse("Requirements\n- Prior experience handling customer complaints", profile)
        assertThat(gap.matches.single().status).isEqualTo(MatchStatus.PARTIAL)
        assertThat(gap.matches.single().evidenceIds).containsExactly("e1-b1")
    }

    @Test
    fun cRequirementIsNotSatisfiedByCppEvidence() = runTest {
        val profile = profileOf(listOf("C++"))
        val (_, gap) = analyse("Requirements\n- Knowledge of C, Python", profile)
        assertThat(gap.matches.single().status).isEqualTo(MatchStatus.GAP)
    }

    @Test
    fun javaRequirementIsNotSatisfiedByJavaScriptEvidence() = runTest {
        val profile = profileOf(listOf("JavaScript"))
        val (_, gap) = analyse("Requirements\n- Java", profile)
        assertThat(gap.matches.single().status).isEqualTo(MatchStatus.GAP)
    }

    @Test
    fun genericDegreeWithFieldIsNotMetByADegreeInAnotherField() = runTest {
        val profile = profileOf(emptyList(), entry("e1", EntryCategory.EDUCATION, "B.Com in Finance"))
        val (_, gap) = analyse("Requirements\n- Bachelor's degree in Computer Science", profile)
        assertThat(gap.matches.single().status).isEqualTo(MatchStatus.PARTIAL)
    }

    @Test
    fun genericDegreeWithFieldIsMetByADegreeInThatField() = runTest {
        val profile = profileOf(emptyList(), entry("e1", EntryCategory.EDUCATION, "B.Com in Finance"))
        val (_, gap) = analyse("Requirements\n- Bachelor's degree in Commerce", profile)
        assertThat(gap.matches.single().status).isEqualTo(MatchStatus.MET)
    }

    @Test
    fun fieldAlternativesNeedOnlyOneField() = runTest {
        val (_, gap) = analyse("Requirements\n- B.Tech in Computer Science or Information Technology")
        assertThat(gap.matches.single().status).isEqualTo(MatchStatus.MET)
    }

    @Test
    fun missingFieldOfStudyWithMatchingLevelIsPartial() = runTest {
        val profile = profileOf(emptyList(), entry("e1", EntryCategory.EDUCATION, "B.Tech in Mechanical Engineering"))
        val (_, gap) = analyse("Requirements\n- B.Tech in Computer Science", profile)
        assertThat(gap.matches.single().status).isEqualTo(MatchStatus.PARTIAL)
    }

    @Test
    fun yearsRequirementIsAtMostPartialWithAProjectOnlyResume() = runTest {
        val profile = profileOf(
            emptyList(),
            entry("p1", EntryCategory.PROJECT, "Quiz App", "Built a Java quiz app"),
        )
        val (_, gap) = analyse("Requirements\n- 5+ years of Java experience", profile)
        assertThat(gap.matches.single().status).isEqualTo(MatchStatus.PARTIAL)
        assertThat(gap.matches.single().evidenceIds).contains("p1-b1")
    }

    @Test
    fun yearsRequirementWithNoEvidenceStaysGap() = runTest {
        val (_, gap) = analyse("Requirements\n- 3 years of Kubernetes experience")
        assertThat(gap.matches.single().status).isEqualTo(MatchStatus.GAP)
    }

    @Test
    fun mustHaveGapProducesOneQuickQuestionAndReasonsForEveryMatch() = runTest {
        val (_, gap) = analyse(
            """
            Requirements
            - Strong SQL
            - Experience presenting to senior leaders
            """.trimIndent(),
        )

        assertThat(gap.question?.text).isEqualTo("Do you have experience with presenting to senior leaders?")
        assertThat(gap.question?.why).isEqualTo("It's a must-have for this role, and your resume doesn't mention it.")
        assertThat(gap.matches.map { it.reason }).doesNotContain(null)
        assertThat(gap.keywordCoverage.covered).isGreaterThan(0)
    }

    @Test
    fun freshersRangeStartingAtZeroYearsCanBeMet() = runTest {
        val (_, gap) = analyse("Requirements\n- 0-2 years of Kotlin experience")
        assertThat(gap.matches.single().status).isEqualTo(MatchStatus.MET)
    }

    @Test
    fun excelVerbInResumeIsNotEvidenceForExcelTool() = runTest {
        val profile = profileOf(
            emptyList(),
            entry("e1", EntryCategory.ACHIEVEMENT, "Awards", "Students who excel in academics get medals"),
        )
        val (_, gap) = analyse("Requirements\n- Excel", profile)
        assertThat(gap.matches.single().status).isEqualTo(MatchStatus.GAP)
    }

    @Test
    fun bulletOverTheLimitIsNeverCitedAsEvidence() = runTest {
        val longBullet = "Built Kubernetes clusters " + "a".repeat(400) + "."
        val profile = profileOf(
            emptyList(),
            entry("lab", EntryCategory.EXPERIENCE, "Cloud lab", longBullet, "Ran Kubernetes drills."),
        )
        val (_, gap) = analyse("Requirements\n- Experience with Kubernetes", profile)
        assertThat(gap.matches.single().evidenceIds).containsExactly("lab-b2")
    }
}
