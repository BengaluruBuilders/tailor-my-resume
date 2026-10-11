package com.tailormyresume.core.domain

import com.google.common.truth.Truth.assertThat
import com.tailormyresume.core.domain.offline.OfflineFabricationGuard
import com.tailormyresume.core.domain.offline.OfflineGapMatcher
import com.tailormyresume.core.domain.offline.OfflineJobDescriptionAnalyzer
import com.tailormyresume.core.domain.offline.OfflineResumeTailor
import com.tailormyresume.core.domain.offline.jobDescriptionResources
import com.tailormyresume.core.domain.offline.profileOf
import com.tailormyresume.core.domain.offline.resourceText
import com.tailormyresume.core.domain.offline.sampleProfile
import com.tailormyresume.core.model.BulletDecision
import com.tailormyresume.core.model.CandidateProfile
import com.tailormyresume.core.model.EditType
import com.tailormyresume.core.model.EntryCategory
import com.tailormyresume.core.model.EvidenceBullet
import com.tailormyresume.core.model.FactSource
import com.tailormyresume.core.model.GapAnalysis
import com.tailormyresume.core.model.GuardrailViolation
import com.tailormyresume.core.model.JobDescription
import com.tailormyresume.core.model.JobRequirement
import com.tailormyresume.core.model.KeywordCoverage
import com.tailormyresume.core.model.ProfileEntry
import com.tailormyresume.core.model.QuickAnswer
import com.tailormyresume.core.model.RequirementPriority
import com.tailormyresume.core.model.RequirementType
import com.tailormyresume.core.model.TailoredBullet
import com.tailormyresume.core.model.TailoredResume
import com.tailormyresume.core.model.TailoredSkills
import com.tailormyresume.core.model.TailoredText
import kotlinx.coroutines.test.runTest
import org.junit.Test

class TailorResumeUseCaseTest {
    private val emptyJob = JobDescription("", "", "", emptyList())
    private val emptyGap = GapAnalysis(emptyList(), KeywordCoverage(0, 0))

    private fun bullet(
        id: String,
        sourceId: String,
        original: String,
        proposed: String,
        editTypes: List<EditType> = listOf(EditType.REWORD),
    ) = TailoredBullet(
        id = id,
        entryId = "exp-1",
        originalText = original,
        proposedText = proposed,
        sourceIds = listOf(sourceId),
        editTypes = editTypes,
        keywordsUsed = emptyList(),
        violations = emptyList(),
        decision = BulletDecision.PENDING,
    )

    private suspend fun useCaseReturning(vararg bullets: TailoredBullet): TailorResumeUseCase {
        val tailor = object : ResumeTailor {
            override suspend fun tailor(profile: CandidateProfile, job: JobDescription, gap: GapAnalysis, applicationId: String, answer: QuickAnswer?) =
                TailoredResume(bullets.toList())
        }
        return TailorResumeUseCase(tailor, OfflineFabricationGuard())
    }

    @Test
    fun cleanBulletKeepsProposedTextAndEditTypes() = runTest {
        val result = useCaseReturning(
            bullet("t1", "exp-1-b2", "Wrote unit tests with JUnit to improve reliability", "Wrote unit tests with JUnit to improve reliability"),
        )(sampleProfile, emptyJob, emptyGap, "app-1")

        assertThat(result.bullets.single().violations).isEmpty()
        assertThat(result.bullets.single().editTypes).containsExactly(EditType.REWORD)
    }

    @Test
    fun bulletWithFabricatedNumberFallsBackToOriginalButKeepsViolations() = runTest {
        val original = "Wrote unit tests with JUnit to improve reliability"
        val result = useCaseReturning(
            bullet("t1", "exp-1-b2", original, "Wrote 200 unit tests with JUnit to improve reliability"),
        )(sampleProfile, emptyJob, emptyGap, "app-1")

        val fallback = result.bullets.single()
        assertThat(fallback.proposedText).isEqualTo(original)
        assertThat(fallback.editTypes).isEmpty()
        assertThat(fallback.violations).containsExactly(GuardrailViolation.UnsupportedNumber("200"))
    }

    @Test
    fun bulletWithEscalatedVerbFallsBackToOriginal() = runTest {
        val original = "Assisted senior developers with code reviews on Git"
        val result = useCaseReturning(
            bullet("t1", "exp-1-b3", original, "Led senior developers with code reviews on Git"),
        )(sampleProfile, emptyJob, emptyGap, "app-1")

        assertThat(result.bullets.single().proposedText).isEqualTo(original)
        assertThat(result.bullets.single().violations)
            .contains(GuardrailViolation.VerbEscalation(from = "assisted", to = "led"))
    }

    @Test
    fun bulletWithUnknownSourceIdIsDropped() = runTest {
        val result = useCaseReturning(bullet("t1", "does-not-exist", "Original", "Changed text"))(
            sampleProfile,
            emptyJob,
            emptyGap,
            "app-1",
        )

        assertThat(result.bullets).isEmpty()
    }

    @Test
    fun bulletCitingAnUnconfirmedEntryIsDropped() = runTest {
        val text = "Deployed containers with Kubernetes on AWS"
        val result = useCaseReturning(bullet("t1", "unconfirmed-1-b1", text, text))(
            sampleProfile,
            emptyJob,
            emptyGap,
            "app-1",
        )

        assertThat(result.bullets).isEmpty()
    }

    @Test
    fun resultRecordsTheIdsOfTheConfirmedEntriesUsed() = runTest {
        val result = useCaseReturning()(sampleProfile, emptyJob, emptyGap, "app-1")

        assertThat(result.entryIds).containsExactlyElementsIn(sampleProfile.entries.filter { it.isConfirmed }.map { it.id })
        assertThat(result.entryIds).doesNotContain("unconfirmed-1")
    }

    @Test
    fun bulletWithNoSourceIdsIsDroppedAndOthersAreKept() = runTest {
        val text = "Wrote unit tests with JUnit to improve reliability"
        val result = useCaseReturning(
            bullet("t1", "exp-1-b2", text, text).copy(sourceIds = emptyList()),
            bullet("t2", "does-not-exist", "x", "y"),
            bullet("t3", "exp-1-b2", text, text),
        )(sampleProfile, emptyJob, emptyGap, "app-1")

        assertThat(result.bullets.map { it.id }).containsExactly("t3")
    }

    @Test
    fun fallbackUsesTheProfileSourceTextNotTheTailorsOriginalText() = runTest {
        val profileText = "Wrote unit tests with JUnit to improve reliability"
        val lying = bullet("t1", "exp-1-b2", "Led a team of 5", "Led a team of 50 to write tests")
            .copy(keywordsUsed = listOf("junit"))

        val fallback = useCaseReturning(lying)(sampleProfile, emptyJob, emptyGap, "app-1").bullets.single()

        assertThat(fallback.originalText).isEqualTo(profileText)
        assertThat(fallback.proposedText).isEqualTo(profileText)
        assertThat(fallback.editTypes).isEmpty()
        assertThat(fallback.keywordsUsed).isEmpty()
        assertThat(fallback.violations).isNotEmpty()
    }

    @Test
    fun cleanBulletTakesItsOriginalTextFromTheProfile() = runTest {
        val profileText = "Wrote unit tests with JUnit to improve reliability"
        val clean = bullet("t1", "exp-1-b2", "tailor supplied text", profileText)

        val result = useCaseReturning(clean)(sampleProfile, emptyJob, emptyGap, "app-1").bullets.single()

        assertThat(result.originalText).isEqualTo(profileText)
        assertThat(result.violations).isEmpty()
    }

    @Test
    fun onlyTheViolatingBulletFallsBack() = runTest {
        val cleanText = "Wrote unit tests with JUnit to improve reliability"
        val result = useCaseReturning(
            bullet("t1", "exp-1-b2", cleanText, cleanText),
            bullet("t2", "exp-1-b3", "Assisted senior developers with code reviews on Git", "Managed senior developers with code reviews on Git"),
        )(sampleProfile, emptyJob, emptyGap, "app-1")

        assertThat(result.bullets.map { it.violations.isEmpty() }).containsExactly(true, false).inOrder()
        assertThat(result.bullets[0].editTypes).containsExactly(EditType.REWORD)
        assertThat(result.bullets[1].editTypes).isEmpty()
    }

    @Test
    fun guardRunsOnEveryBulletWithItsOwnSources() = runTest {
        val calls = mutableListOf<Pair<String, List<EvidenceBullet>>>()
        val guard = object : FabricationGuard {
            override fun check(proposedText: String, sources: List<EvidenceBullet>, profile: CandidateProfile) =
                emptyList<GuardrailViolation>().also { calls += proposedText to sources }
        }
        val tailor = object : ResumeTailor {
            override suspend fun tailor(profile: CandidateProfile, job: JobDescription, gap: GapAnalysis, applicationId: String, answer: QuickAnswer?) = TailoredResume(
                listOf(
                    bullet("t1", "exp-1-b2", "a", "a1"),
                    bullet("t2", "proj-1-b2", "b", "b1"),
                ),
            )
        }

        TailorResumeUseCase(tailor, guard)(sampleProfile, emptyJob, emptyGap, "app-1")

        assertThat(calls.map { it.first }).containsExactly("a1", "b1").inOrder()
        assertThat(calls.map { it.second.single().id }).containsExactly("exp-1-b2", "proj-1-b2").inOrder()
    }

    @Test
    fun realTailorOutputPassesTheRealGuardForEveryBulletAcrossFiveJobs() = runTest {
        val analyzer = OfflineJobDescriptionAnalyzer()
        val matcher = OfflineGapMatcher()
        val useCase = TailorResumeUseCase(OfflineResumeTailor(), OfflineFabricationGuard())
        jobDescriptionResources.forEach { resource ->
            val job = analyzer.analyze(resourceText(resource))
            val resume = useCase(sampleProfile, job, matcher.match(sampleProfile, job), "app-1")
            assertThat(resume.bullets).isNotEmpty()
            resume.bullets.forEach { assertThat(it.violations).isEmpty() }
        }
    }

    private val infosys = ProfileEntry(
        id = "exp-infosys",
        category = EntryCategory.EXPERIENCE,
        title = "Business Analyst",
        organization = "Infosys",
        startDate = "Jul 2022",
        endDate = "Present",
        bullets = listOf(
            EvidenceBullet("exp-infosys-b1", "Built Power BI dashboards for monthly finance reports"),
            EvidenceBullet("exp-infosys-b2", "Wrote SQL pipelines over transaction data"),
        ),
        source = FactSource.IMPORTED,
        isConfirmed = true,
    )
    private val analystProfile = profileOf(listOf("SQL", "Excel", "Power BI"), infosys).copy(
        summary = "Analyst building dashboards.",
        userStatedSkills = listOf("Tableau"),
    )
    private val presenting = JobRequirement(
        id = "req-1",
        text = "Presented quarterly results to the board for 3+ years using Looker",
        type = RequirementType.EXPERIENCE,
        priority = RequirementPriority.MUST_HAVE,
        keywords = listOf("looker"),
    )
    private val analystJob = JobDescription("Analyst", "Northwind", "raw", listOf(presenting))
    private val cfoDetail = "Presented the monthly variance report to the CFO"

    private fun answer(choice: String = AnswerFacts.YES_REGULARLY, detail: String = cfoDetail) =
        QuickAnswer("req-1", choice, detail)

    private fun answerBullet(proposed: String, entryId: String = "exp-infosys") = bullet("t-ans", "ans-req-1", "", proposed)
        .copy(entryId = entryId, editTypes = listOf(EditType.EMPHASISE))

    private suspend fun run(
        proposed: TailoredResume,
        quickAnswer: QuickAnswer? = answer(),
        job: JobDescription = analystJob,
    ): TailoredResume {
        val tailor = object : ResumeTailor {
            override suspend fun tailor(profile: CandidateProfile, job: JobDescription, gap: GapAnalysis, applicationId: String, answer: QuickAnswer?) = proposed
        }
        return TailorResumeUseCase(tailor, OfflineFabricationGuard())(analystProfile, job, emptyGap, "app-1", quickAnswer = quickAnswer)
    }

    private fun summary(text: String, vararg sourceIds: String, decision: BulletDecision = BulletDecision.PENDING) =
        TailoredText(text, "", sourceIds.toList(), decision = decision)

    @Test
    fun unsupportedAnswerBulletFallsBackToOriginal() = runTest {
        val result = run(
            TailoredResume(listOf(answerBullet("$cfoDetail and cut reporting time by 40%"))),
            answer(detail = "$cfoDetail at Infosys"),
        )

        val fallback = result.bullets.single()
        assertThat(fallback.proposedText).isEqualTo(fallback.originalText)
        assertThat(fallback.proposedText).isEmpty()
        assertThat(fallback.violations).contains(GuardrailViolation.UnsupportedNumber("40%"))
    }

    @Test
    fun notYetAndSkippedAnswersCreateNoFactAndAnsBulletFailsWithMissingSource() = runTest {
        listOf("NOT_YET", "SKIPPED").forEach { choice ->
            val result = run(TailoredResume(listOf(answerBullet(cfoDetail))), answer(choice = choice))

            assertThat(result.bullets.single().violations).containsExactly(GuardrailViolation.MissingSource)
            assertThat(result.bullets.single().proposedText).isEmpty()
        }
        assertThat(run(TailoredResume(listOf(answerBullet(cfoDetail))), quickAnswer = null).bullets.single().violations)
            .containsExactly(GuardrailViolation.MissingSource)
    }

    @Test
    fun answerWithoutEmployerNameIsNotAttachedToInfosys() = runTest {
        val result = run(TailoredResume(listOf(answerBullet(cfoDetail))))

        assertThat(result.bullets).isEmpty()
    }

    @Test
    fun answerNamingInfosysOrTitleMayAttach() = runTest {
        val byEmployer = run(TailoredResume(listOf(answerBullet(cfoDetail))), answer(detail = "$cfoDetail at INFOSYS"))
        val byTitle = run(TailoredResume(listOf(answerBullet(cfoDetail))), answer(detail = "As a business analyst: $cfoDetail"))
        val partialWord = run(TailoredResume(listOf(answerBullet(cfoDetail))), answer(detail = "$cfoDetail for Infosystems"))

        assertThat(byEmployer.bullets.single().violations).isEmpty()
        assertThat(byTitle.bullets.map { it.id }).containsExactly("t-ans")
        assertThat(partialWord.bullets).isEmpty()
    }

    @Test
    fun prototypeAnswerGivesThreeChanges() = runTest {
        val result = run(
            TailoredResume(
                bullets = listOf(
                    bullet("t1", "exp-infosys-b1", "x", "Built Power BI dashboards for monthly reports").copy(entryId = "exp-infosys"),
                    bullet("t2", "exp-infosys-b2", "x", "Wrote SQL pipelines for transaction data").copy(entryId = "exp-infosys"),
                    answerBullet(cfoDetail),
                ),
                summary = summary("Built Power BI dashboards for monthly reports. $cfoDetail.", "exp-infosys-b1", "ans-req-1"),
            ),
        )

        assertThat(result.bullets.map { it.id }).containsExactly("t1", "t2").inOrder()
        assertThat(result.summary?.violations).isEmpty()
        assertThat(result.changeCount).isEqualTo(3)
    }

    @Test
    fun unsupportedSummarySentenceRestoresOriginalSummary() = runTest {
        val result = run(
            TailoredResume(
                emptyList(),
                summary = summary("Built Power BI dashboards for monthly reports. Led a team of 12 analysts.", "exp-infosys-b1"),
            ),
        )

        val kept = checkNotNull(result.summary)
        assertThat(kept.text).isEqualTo("Analyst building dashboards.")
        assertThat(kept.original).isEqualTo("Analyst building dashboards.")
        assertThat(kept.violations).isNotEmpty()
        assertThat(result.changeCount).isEqualTo(0)
    }

    @Test
    fun summaryWithoutAnyCitedSourceFallsBack() = runTest {
        val result = run(TailoredResume(emptyList(), summary = summary("Built Power BI dashboards for monthly reports.")))

        assertThat(checkNotNull(result.summary).violations).containsExactly(GuardrailViolation.MissingSource)
    }

    @Test
    fun supportedSummaryIsKept() = runTest {
        val proposedText = "Built Power BI dashboards for monthly reports."
        val result = run(TailoredResume(emptyList(), summary = summary(proposedText, "exp-infosys-b1")))

        val kept = checkNotNull(result.summary)
        assertThat(kept.text).isEqualTo(proposedText)
        assertThat(kept.original).isEqualTo("Analyst building dashboards.")
        assertThat(kept.violations).isEmpty()
    }

    @Test
    fun answerWithoutDetailCannotSupportRequirementNumbersInTheSummary() = runTest {
        val result = run(
            TailoredResume(emptyList(), summary = summary("Presented quarterly results to the board for 3 years.", "ans-req-1")),
            answer(choice = AnswerFacts.A_FEW_TIMES, detail = ""),
        )

        val kept = checkNotNull(result.summary)
        assertThat(kept.text).isEqualTo("Analyst building dashboards.")
        assertThat(kept.violations).contains(GuardrailViolation.UnsupportedNumber("3"))
    }

    @Test
    fun skillsReorderedFromProfileOnly() = runTest {
        val result = run(TailoredResume(emptyList(), skills = TailoredSkills(listOf("Power BI", "SQL", "Excel"), emptyList())))

        val skills = checkNotNull(result.skills)
        assertThat(skills.skills).containsExactly("Power BI", "SQL", "Excel").inOrder()
        assertThat(skills.original).containsExactly("SQL", "Excel", "Power BI").inOrder()
        assertThat(skills.violations).isEmpty()
    }

    @Test
    fun addedSkillFromUserStatedOrAnswerKept() = runTest {
        val result = run(
            TailoredResume(emptyList(), skills = TailoredSkills(listOf("SQL", "tableau", "Looker"), emptyList())),
            answer(detail = "Built weekly Looker dashboards"),
        )

        assertThat(checkNotNull(result.skills).skills).containsExactly("SQL", "tableau", "Looker").inOrder()
        assertThat(checkNotNull(result.skills).violations).isEmpty()
    }

    @Test
    fun otherAddedSkillRemovedWithViolation() = runTest {
        val result = run(
            TailoredResume(emptyList(), skills = TailoredSkills(listOf("SQL", "Kubernetes", "Looker"), emptyList())),
            answer(choice = "NOT_YET"),
        )

        val skills = checkNotNull(result.skills)
        assertThat(skills.skills).containsExactly("SQL")
        assertThat(skills.violations)
            .containsExactly(GuardrailViolation.UnsupportedTerm("Kubernetes"), GuardrailViolation.UnsupportedTerm("Looker"))
    }

    @Test
    fun newSuggestionsStartPending() = runTest {
        val result = run(
            TailoredResume(
                bullets = listOf(bullet("t1", "exp-infosys-b2", "x", "Wrote SQL pipelines for transaction data")),
                summary = summary("Built Power BI dashboards for monthly reports.", "exp-infosys-b1", decision = BulletDecision.ACCEPTED),
                skills = TailoredSkills(listOf("SQL"), emptyList(), decision = BulletDecision.ACCEPTED),
            ),
        )

        assertThat(result.decisions).containsExactly(BulletDecision.PENDING, BulletDecision.PENDING, BulletDecision.PENDING)
    }

    private suspend fun presentingJob(): Pair<JobDescription, String> {
        val job = OfflineJobDescriptionAnalyzer().analyze(
            "Analyst\nNorthwind\n\nRequirements\n- Experience presenting quarterly results to the board for 3+ years\n",
        )
        return job to job.requirements.first { it.text.contains("presenting", ignoreCase = true) }.id
    }

    @Test
    fun blankDetailAnswerDoesNotTurnRequirementWordsIntoFacts() = runTest {
        val (job, requirementId) = presentingJob()
        val blank = QuickAnswer(requirementId, AnswerFacts.A_FEW_TIMES, "")

        assertThat(AnswerFacts.factOf(blank, job)).isNull()
        val summaryResult = run(
            TailoredResume(emptyList(), summary = summary("Presenting quarterly results.", "ans-$requirementId")),
            blank,
            job,
        )
        val skillsResult = run(
            TailoredResume(emptyList(), skills = TailoredSkills(listOf("SQL", "Quarterly", "Results"), emptyList())),
            blank,
            job,
        )

        assertThat(checkNotNull(summaryResult.summary).text).isEqualTo("Analyst building dashboards.")
        assertThat(checkNotNull(summaryResult.summary).violations).isNotEmpty()
        assertThat(checkNotNull(skillsResult.skills).skills).containsExactly("SQL")
    }

    @Test
    fun blankDetailAnswerStillContributesTheNamedSkill() = runTest {
        val blank = answer(detail = "")

        assertThat(AnswerFacts.keywords(blank, analystJob)).containsExactly("Looker")
        assertThat(AnswerFacts.factOf(blank, analystJob)?.text).isEqualTo("Looker")
    }

    @Test
    fun summaryClauseFromAnotherFactCannotRideOnAnAnswer() = runTest {
        val result = run(
            TailoredResume(
                emptyList(),
                summary = summary("$cfoDetail with Power BI dashboards.", "exp-infosys-b1", "ans-req-1"),
            ),
        )

        val kept = checkNotNull(result.summary)
        assertThat(kept.text).isEqualTo("Analyst building dashboards.")
        assertThat(kept.violations).isNotEmpty()
    }

    @Test
    fun summaryClausesOfOneEntryAreKept() = runTest {
        val text = "Built Power BI dashboards for monthly finance reports and wrote SQL pipelines over transaction data."
        val result = run(TailoredResume(emptyList(), summary = summary(text, "exp-infosys-b1", "exp-infosys-b2")))

        assertThat(checkNotNull(result.summary).text).isEqualTo(text)
        assertThat(checkNotNull(result.summary).violations).isEmpty()
    }

    @Test
    fun summaryOfFunctionWordsOnlyFallsBack() = runTest {
        val result = run(TailoredResume(emptyList(), summary = summary("The and of. To, or with.", "exp-infosys-b1")))

        assertThat(checkNotNull(result.summary).text).isEqualTo("Analyst building dashboards.")
        assertThat(checkNotNull(result.summary).violations).containsExactly(GuardrailViolation.MissingSource)
    }

    @Test
    fun summaryNumberOnlyClauseIsStillChecked() = runTest {
        val result = run(
            TailoredResume(emptyList(), summary = summary("Built Power BI dashboards for monthly finance reports, by 40%.", "exp-infosys-b1")),
        )

        assertThat(checkNotNull(result.summary).violations).contains(GuardrailViolation.UnsupportedNumber("40%"))
    }

    @Test
    fun prototypeAnswerChoiceBacksAFact() = runTest {
        val prototypeAnswer = checkNotNull(
            com.tailormyresume.core.testing.data.PrototypeFixtures.returning().applications.firstNotNullOf { it.quickAnswer },
        )

        assertThat(AnswerFacts.factOf(prototypeAnswer.copy(detail = cfoDetail), emptyJob)?.text).isEqualTo(cfoDetail)
    }

    private suspend fun answerKeywordsFor(detail: String, requirementText: String, type: RequirementType, vararg keywords: String): List<String> {
        val requirement = JobRequirement("req-1", requirementText, type, RequirementPriority.MUST_HAVE, keywords.toList())
        val job = JobDescription("Analyst", "Northwind", "raw", listOf(requirement))
        return AnswerFacts.keywords(QuickAnswer("req-1", AnswerFacts.A_FEW_TIMES, detail), job)
    }

    @Test
    fun placeholderDetailAddsNoRequirementKeywords() = runTest {
        val presentingText = "Presenting quarterly results to senior leaders"
        val keywords = arrayOf("presenting", "quarterly", "results", "senior", "leaders")

        assertThat(answerKeywordsFor("Yes, a few times", presentingText, RequirementType.EXPERIENCE, *keywords)).isEmpty()
        assertThat(answerKeywordsFor("n/a", "Managing stakeholders", RequirementType.EXPERIENCE, "stakeholders")).isEmpty()

        val job = JobDescription(
            "Analyst",
            "Northwind",
            "raw",
            listOf(JobRequirement("req-1", presentingText, RequirementType.EXPERIENCE, RequirementPriority.MUST_HAVE, keywords.toList())),
        )
        val placeholder = QuickAnswer("req-1", AnswerFacts.A_FEW_TIMES, "Yes, a few times")
        val summaryResult = run(TailoredResume(emptyList(), summary = summary("Presenting quarterly results.", "ans-req-1")), placeholder, job)
        val skillsResult = run(
            TailoredResume(emptyList(), skills = TailoredSkills(listOf("SQL", "Quarterly", "Results"), emptyList())),
            placeholder,
            job,
        )
        assertThat(checkNotNull(summaryResult.summary).violations).isNotEmpty()
        assertThat(checkNotNull(skillsResult.skills).skills).containsExactly("SQL")
    }

    @Test
    fun detailKeywordsAreOnlyThoseTheDetailStates() = runTest {
        assertThat(
            answerKeywordsFor("Built it in Power BI", "Dashboards for finance teams", RequirementType.EXPERIENCE, "power bi", "dashboards"),
        ).containsExactly("Power BI")
        assertThat(answerKeywordsFor("", "Advanced SQL", RequirementType.SKILL, "sql")).containsExactly("SQL")
    }

    @Test
    fun oneLetterSkillClausesAreCheckedAgainstSources() = runTest {
        listOf("Built dashboards with R." to "R", "Shipped services in C++." to "C++").forEach { (text, skill) ->
            val lacking = run(TailoredResume(emptyList(), summary = summary(text, "exp-infosys-b1")))
            assertThat(checkNotNull(lacking.summary).violations).isNotEmpty()

            val stating = run(
                TailoredResume(emptyList(), summary = summary(text, "ans-req-1")),
                answer(detail = "Built dashboards and shipped services using $skill"),
            )
            assertThat(checkNotNull(stating.summary).violations).isEmpty()
        }
    }

    @Test
    fun honestOneFactSummariesStillPass() = runTest {
        listOf(
            "Built Power BI dashboards for monthly finance reports.",
            "Wrote SQL pipelines over transaction data.",
        ).forEach { text ->
            val result = run(TailoredResume(emptyList(), summary = summary(text, "exp-infosys-b1", "exp-infosys-b2")))
            assertThat(checkNotNull(result.summary).violations).isEmpty()
        }
    }
}
