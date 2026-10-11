package com.tailormyresume.core.domain.offline

import com.google.common.truth.Truth.assertThat
import com.tailormyresume.core.domain.TailorResumeUseCase
import com.tailormyresume.core.model.BulletDecision
import com.tailormyresume.core.model.CandidateProfile
import com.tailormyresume.core.model.EditType
import com.tailormyresume.core.model.EntryCategory
import com.tailormyresume.core.model.JobDescription
import com.tailormyresume.core.model.TailoredBullet
import com.tailormyresume.core.model.TailoredResume
import kotlinx.coroutines.test.runTest
import org.junit.Test

class OfflineResumeTailorTest {
    private val analyzer = OfflineJobDescriptionAnalyzer()
    private val matcher = OfflineGapMatcher()
    private val tailor = OfflineResumeTailor()

    private suspend fun tailorFor(
        jd: String,
        profile: CandidateProfile = sampleProfile,
    ): Pair<JobDescription, TailoredResume> {
        val job = analyzer.analyze(jd)
        return job to tailor.tailor(profile, job, matcher.match(profile, job), "app-1", null, "run-1")
    }

    private fun TailoredResume.bulletFor(sourceId: String): TailoredBullet = bullets.first { sourceId in it.sourceIds }

    private fun tokensOf(text: String) = TextTokens.words(text)

    @Test
    fun onlyConfirmedEntriesAreTailoredAndEveryBulletIsCovered() = runTest {
        val (_, resume) = tailorFor("Requirements\n- Kotlin")
        val expected = sampleProfile.entries.filter { it.isConfirmed }.flatMap { it.bullets }.map { it.id }
        assertThat(resume.bullets.flatMap { it.sourceIds }).containsExactlyElementsIn(expected)
    }

    @Test
    fun bulletsStartPendingWithSourceIdsAndNoViolations() = runTest {
        val (_, resume) = tailorFor(resourceText("jd_android.txt"))
        resume.bullets.forEach {
            assertThat(it.decision).isEqualTo(BulletDecision.PENDING)
            assertThat(it.violations).isEmpty()
            assertThat(it.sourceIds).hasSize(1)
            assertThat(it.id).isEqualTo("tailored-${it.sourceIds.single()}")
        }
    }

    @Test
    fun trapJobGapsNeverAppearInAnyTailoredBullet() = runTest {
        val (job, resume) = tailorFor(resourceText("jd_trap_cloud.txt"))
        val forbidden = listOf("kubernetes", "aws", "mba", "terraform", "docker")
        resume.bullets.forEach { bullet ->
            val words = tokensOf(bullet.proposedText)
            forbidden.forEach { assertThat(words).doesNotContain(it) }
            assertThat(bullet.keywordsUsed).containsNoneIn(forbidden)
        }
        assertThat(job.requirements.flatMap { it.keywords }).containsAtLeastElementsIn(forbidden)
    }

    @Test
    fun rewordsAliasToTheJobTermOnlyWhenTheBulletHasTheAlias() = runTest {
        val (_, resume) = tailorFor("Requirements\n- JavaScript\n- Excel")
        val js = resume.bulletFor("exp-1-b4")
        assertThat(js.proposedText).isEqualTo("Fixing bugs in the JavaScript frontend")
        assertThat(js.editTypes).containsAtLeast(EditType.REWORD, EditType.SHORTEN)
        assertThat(js.editTypes).doesNotContain(EditType.EMPHASISE)
        assertThat(js.keywordsUsed).contains("javascript")
        val excel = resume.bulletFor("proj-1-b3")
        assertThat(excel.proposedText).isEqualTo("Created charts in Excel to analyse spending data")
        assertThat(excel.editTypes).contains(EditType.REWORD)
        assertThat(excel.editTypes).doesNotContain(EditType.SHORTEN)
        assertThat(resume.bulletFor("exp-1-b2").proposedText).isEqualTo("Wrote unit tests with JUnit to improve reliability")
    }

    @Test
    fun pluralFormOfTheSameTermIsNotReworded() = runTest {
        val (_, resume) = tailorFor("Requirements\n- REST API")
        assertThat(resume.bulletFor("exp-1-b1").proposedText).contains("REST APIs")
    }

    @Test
    fun aliasIsNotRewordedWhenJobDoesNotAskForIt() = runTest {
        val (_, resume) = tailorFor("Requirements\n- Kotlin")
        assertThat(resume.bulletFor("exp-1-b4").proposedText).contains("js frontend")
        assertThat(resume.bulletFor("proj-1-b3").proposedText).contains("ms excel")
    }

    @Test
    fun looseAliasesAreNeverReworded() = runTest {
        val profile = profileOf(
            emptyList(),
            entry("e1", EntryCategory.EXPERIENCE, "Intern", "Wrote core java services for billing"),
        )
        val (_, resume) = tailorFor("Requirements\n- Java", profile)
        assertThat(resume.bullets.single().proposedText).isEqualTo("Wrote core java services for billing")
        assertThat(resume.bullets.single().editTypes).isEmpty()
        assertThat(resume.bullets.single().keywordsUsed).containsExactly("java")
    }

    @Test
    fun shortensFillerPhrasesWithoutAddingWords() = runTest {
        val (_, resume) = tailorFor("Requirements\n- Kotlin")
        val bullet = resume.bulletFor("exp-1-b1")
        assertThat(bullet.proposedText)
            .isEqualTo("Using Kotlin and Spring Boot, building REST APIs for the inventory module")
        assertThat(bullet.editTypes).containsExactly(EditType.SHORTEN, EditType.EMPHASISE).inOrder()
    }

    @Test
    fun emphasisesKeywordClauseByPureReordering() = runTest {
        val (_, resume) = tailorFor("Requirements\n- Jetpack Compose")
        val bullet = resume.bulletFor("proj-1-b1")
        assertThat(bullet.proposedText)
            .isEqualTo("Using Jetpack Compose and Room, built an Android app to track daily expenses")
        assertThat(bullet.editTypes).contains(EditType.EMPHASISE)
        assertThat(tokensOf(bullet.proposedText).sorted()).isEqualTo(tokensOf(bullet.originalText).sorted())
    }

    @Test
    fun doesNotEmphasiseWhenKeywordAlreadyLeads() = runTest {
        val profile = profileOf(
            emptyList(),
            entry("e1", EntryCategory.PROJECT, "App", "Kotlin developer who built tools using Git"),
        )
        val (_, resume) = tailorFor("Requirements\n- Kotlin\n- Git", profile)
        assertThat(resume.bullets.single().editTypes).doesNotContain(EditType.EMPHASISE)
    }

    @Test
    fun doesNotEmphasiseWhenClauseContinuesWithAnotherVerb() = runTest {
        val profile = profileOf(
            emptyList(),
            entry("e1", EntryCategory.PROJECT, "App", "Built dashboards using Excel and presented insights weekly"),
        )
        val (_, resume) = tailorFor("Requirements\n- Excel", profile)
        assertThat(resume.bullets.single().proposedText).isEqualTo("Built dashboards using Excel and presented insights weekly")
    }

    @Test
    fun reordersBulletsWithinAnEntryByJobRelevance() = runTest {
        val (_, resume) = tailorFor("Requirements\n- Git")
        val experience = resume.bullets.filter { it.entryId == "exp-1" }
        assertThat(experience.map { it.sourceIds.single() })
            .containsExactly("exp-1-b3", "exp-1-b1", "exp-1-b2", "exp-1-b4").inOrder()
        assertThat(experience[0].editTypes).contains(EditType.REORDER)
        assertThat(experience[3].editTypes).doesNotContain(EditType.REORDER)
    }

    @Test
    fun entriesKeepProfileOrder() = runTest {
        val (_, resume) = tailorFor("Requirements\n- Power BI")
        assertThat(resume.bullets.map { it.entryId }.distinct())
            .containsExactly("edu-1", "exp-1", "proj-1", "proj-2").inOrder()
    }

    @Test
    fun mustHaveKeywordsOutrankNiceToHaveWhenReordering() = runTest {
        val profile = profileOf(
            emptyList(),
            entry(
                "e1",
                EntryCategory.EXPERIENCE,
                "Intern",
                "Used Tableau for reports",
                "Used SQL for reports",
            ),
        )
        val (_, resume) = tailorFor("Requirements\n- SQL\nPreferred\n- Tableau", profile)
        assertThat(resume.bullets.map { it.sourceIds.single() }).containsExactly("e1-b2", "e1-b1").inOrder()
    }

    @Test
    fun unchangedBulletHasNoEditTypes() = runTest {
        val (_, resume) = tailorFor("Requirements\n- Kotlin")
        val bullet = resume.bulletFor("exp-1-b3")
        assertThat(bullet.proposedText).isEqualTo(bullet.originalText)
        assertThat(bullet.editTypes).isEmpty()
    }

    @Test
    fun keywordsUsedListsJobKeywordsPresentInProposedText() = runTest {
        val (_, resume) = tailorFor("Requirements\n- Kotlin, Python and SQL")
        assertThat(resume.bulletFor("exp-1-b1").keywordsUsed).containsExactly("kotlin")
        assertThat(resume.bulletFor("proj-2-b1").keywordsUsed).containsExactly("sql")
        assertThat(resume.bulletFor("edu-1-b1").keywordsUsed).isEmpty()
    }

    @Test
    fun tailoredTextNeverIntroducesWordsOutsideTheSourceBulletAcrossFiveJobs() = runTest {
        jobDescriptionResources.forEach { resource ->
            val (job, resume) = tailorFor(resourceText(resource))
            val jobTerms = job.requirements.flatMap { it.keywords }.distinct()
            val allowedExtras = jobTerms.flatMap { tokensOf(SkillLexicon.displayName(it)) }.toSet()
            resume.bullets.forEach { bullet ->
                val allowed = tokensOf(bullet.originalText).toSet() + allowedExtras
                val extras = tokensOf(bullet.proposedText).filterNot { it in allowed }
                assertThat(extras).isEmpty()
                assertThat(jobTerms).containsAtLeastElementsIn(bullet.keywordsUsed)
            }
        }
    }

    @Test
    fun aliasRewordingIntroducesOnlyTheCanonicalTermTokens() = runTest {
        val (_, resume) = tailorFor("Requirements\n- JavaScript\n- Excel")
        resume.bullets.forEach { bullet ->
            val allowed = tokensOf(bullet.originalText).toSet() + tokensOf("JavaScript Excel")
            assertThat(tokensOf(bullet.proposedText).filterNot { it in allowed }).isEmpty()
        }
    }

    @Test
    fun tailoringIsDeterministic() = runTest {
        val jd = resourceText("jd_data_analyst.txt")
        assertThat(tailorFor(jd).second).isEqualTo(tailorFor(jd).second)
    }

    @Test
    fun profileWithoutConfirmedEntriesGivesEmptyResume() = runTest {
        val profile = sampleProfile.copy(entries = sampleProfile.entries.map { it.copy(isConfirmed = false) })
        assertThat(tailorFor("Requirements\n- Kotlin", profile).second.bullets).isEmpty()
    }

    @Test
    fun fillerRemoverKeepsShortBulletsIntact() = runTest {
        assertThat(FillerRemover.shorten("Various tasks")).isEqualTo("Various tasks")
        assertThat(FillerRemover.shorten("Responsible for various reports and dashboards"))
            .isEqualTo("Reports and dashboards")
        assertThat(FillerRemover.shorten("Studied in order to learn Kotlin")).isEqualTo("Studied to learn Kotlin")
    }

    @Test
    fun unixIsNeverRewordedToLinux() = runTest {
        val profile = profileOf(
            emptyList(),
            entry("e1", EntryCategory.EXPERIENCE, "Intern", "Administered Unix servers"),
        )
        val (job, resume) = tailorFor("Requirements\n- Linux", profile)
        assertThat(resume.bullets.single().proposedText).isEqualTo("Administered Unix servers")
        assertThat(resume.bullets.single().editTypes).isEmpty()

        val gap = matcher.match(profile, job)
        val guarded = TailorResumeUseCase(tailor, OfflineFabricationGuard())(profile, job, gap, "app-1", "run-1")
        assertThat(guarded.bullets.single().proposedText).isEqualTo("Administered Unix servers")
    }

    @Test
    fun onlyLooselyRelatedAliasesStayUntouched() = runTest {
        listOf(
            Triple("Managed SCM using Git", "Supply Chain", "Managed SCM using Git"),
            Triple("Handled costing for orders", "Cost Accounting", "Handled costing for orders"),
            Triple("Ran Spark jobs on a cluster", "Apache Spark", "Ran Spark jobs on a cluster"),
            Triple("Cleared IPCC in 2023", "CA Inter", "Cleared IPCC in 2023"),
            Triple("Prepared ITR forms", "Income Tax", "Prepared ITR forms"),
        ).forEach { (bulletText, jobTerm, expected) ->
            val profile = profileOf(emptyList(), entry("e1", EntryCategory.EXPERIENCE, "Intern", bulletText))
            val (_, resume) = tailorFor("Requirements\n- $jobTerm", profile)
            assertThat(resume.bullets.single().proposedText).isEqualTo(expected)
        }
    }

    @Test
    fun strictSameToolAliasesAreReworded() = runTest {
        val profile = profileOf(
            emptyList(),
            entry(
                "e1",
                EntryCategory.EXPERIENCE,
                "Intern",
                "Wrote py scripts and ts modules deployed on k8s with postgres",
            ),
        )
        val (_, resume) = tailorFor(
            "Requirements\n- Python\n- TypeScript\n- Kubernetes\n- PostgreSQL",
            profile,
        )
        assertThat(resume.bullets.single().proposedText)
            .isEqualTo("Wrote Python scripts and TypeScript modules deployed on Kubernetes with PostgreSQL")
        assertThat(resume.bullets.single().editTypes).containsExactly(EditType.REWORD)
    }
}
