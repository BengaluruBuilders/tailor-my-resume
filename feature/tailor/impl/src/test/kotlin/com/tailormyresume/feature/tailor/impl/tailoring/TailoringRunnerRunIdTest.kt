package com.tailormyresume.feature.tailor.impl.tailoring

import com.google.common.truth.Truth.assertThat
import com.tailormyresume.core.domain.ResumeTailor
import com.tailormyresume.core.domain.TailorResumeUseCase
import com.tailormyresume.core.model.CandidateProfile
import com.tailormyresume.core.model.CreditLedgerKind
import com.tailormyresume.core.model.GapAnalysis
import com.tailormyresume.core.model.JobDescription
import com.tailormyresume.core.model.JobRequirement
import com.tailormyresume.core.model.KeywordCoverage
import com.tailormyresume.core.model.MatchStatus
import com.tailormyresume.core.model.QuickAnswer
import com.tailormyresume.core.model.RequirementMatch
import com.tailormyresume.core.model.RequirementPriority
import com.tailormyresume.core.model.RequirementType
import com.tailormyresume.core.model.TailoredResume
import com.tailormyresume.core.testing.repository.TestApplicationRepository
import com.tailormyresume.core.testing.repository.TestCreditsRepository
import com.tailormyresume.core.testing.repository.TestProfileRepository
import com.tailormyresume.core.testing.util.TestClock
import com.tailormyresume.feature.tailor.impl.CleanFabricationGuard
import com.tailormyresume.feature.tailor.impl.entryFor
import com.tailormyresume.feature.tailor.impl.testApplication
import com.tailormyresume.feature.tailor.impl.testBullet
import com.tailormyresume.feature.tailor.impl.testProfile
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class TailoringRunnerRunIdTest {
    private val dispatcher = StandardTestDispatcher()
    private val applicationRepository = TestApplicationRepository()
    private val profileRepository = TestProfileRepository()
    private val clock = TestClock()
    private val bullet = testBullet("b1", original = "Built an internal tool", proposed = "Developed an internal tool")
    private val tailored = TailoredResume(bullets = listOf(bullet))
    private val quickAnswer = QuickAnswer("req-1", "YES_REGULARLY", "Led internal tools.")

    private val gap = GapAnalysis(
        matches = listOf(
            RequirementMatch(
                JobRequirement("req-1", "Must have internal tools.", RequirementType.TOOL, RequirementPriority.MUST_HAVE, listOf("internal tool")),
                MatchStatus.MET,
                emptyList(),
            ),
        ),
        keywordCoverage = KeywordCoverage(covered = 1, total = 1),
    )

    private fun seed(answer: QuickAnswer? = null) {
        applicationRepository.sendApplications(listOf(testApplication(emptyList(), gap).copy(quickAnswer = answer)))
        profileRepository.sendProfile(testProfile(listOf(entryFor("exp-1", bullet))))
    }

    private fun runner(credits: TestCreditsRepository, tailor: suspend (QuickAnswer?) -> TailoredResume) = TailoringRunner(
        applicationRepository = applicationRepository,
        profileRepository = profileRepository,
        creditsRepository = credits,
        tailorResume = TailorResumeUseCase(
            object : ResumeTailor {
                override suspend fun tailor(
                    profile: CandidateProfile,
                    job: JobDescription,
                    gap: GapAnalysis,
                    applicationId: String,
                    answer: QuickAnswer?,
                    runId: String,
                ): TailoredResume = tailor(answer)
            },
            CleanFabricationGuard,
        ),
        clock = clock,
        dispatcher = dispatcher,
    )

    private suspend fun TestCreditsRepository.spendKeys() =
        observeLedger().first().filter { it.kind == CreditLedgerKind.SPEND }.map { it.productId }

    @Test
    fun storedResultForTheRunSkipsTailoringWithAnEmptyLedger() = runTest(dispatcher) {
        seed()
        runner(TestCreditsRepository()) { tailored }("app-1", "run-1")
        var calls = 0

        val result = runner(TestCreditsRepository()) {
            calls++
            tailored
        }("app-1", "run-1")

        assertThat(result).isEqualTo(TailoringResult.Success)
        assertThat(calls).isEqualTo(0)
    }

    @Test
    fun theStoredResultCarriesTheRunId() = runTest(dispatcher) {
        seed()

        runner(TestCreditsRepository()) { tailored }("app-1", "run-1")

        assertThat(applicationRepository.observeApplication("app-1").first()?.tailoredResume?.runId).isEqualTo("run-1")
    }

    @Test
    fun aSkippedRunRecordsItsMissingSpendOnce() = runTest(dispatcher) {
        seed()
        runner(TestCreditsRepository()) { tailored }("app-1", "run-1")
        val freshLedger = TestCreditsRepository()

        runner(freshLedger) { error("must not run") }("app-1", "run-1")
        runner(freshLedger) { error("must not run") }("app-1", "run-1")

        assertThat(freshLedger.spendKeys()).containsExactly("run-run-1")
    }

    @Test
    fun aNewRunIdTailorsAgainEvenWhenAResultIsStored() = runTest(dispatcher) {
        seed()
        runner(TestCreditsRepository()) { tailored }("app-1", "run-1")
        var calls = 0

        runner(TestCreditsRepository()) {
            calls++
            tailored
        }("app-1", "run-2")

        assertThat(calls).isEqualTo(1)
        assertThat(applicationRepository.observeApplication("app-1").first()?.tailoredResume?.runId).isEqualTo("run-2")
    }

    @Test
    fun theApplicationsQuickAnswerReachesTheTailor() = runTest(dispatcher) {
        seed(answer = quickAnswer)
        var received: QuickAnswer? = null

        runner(TestCreditsRepository()) {
            received = it
            tailored
        }("app-1", "run-1")

        assertThat(received).isEqualTo(quickAnswer)
    }
}
