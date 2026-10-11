package com.tailormyresume.feature.tailor.impl.tailoring

import com.google.common.truth.Truth.assertThat
import com.tailormyresume.core.designsystem.component.content.TmrProgressState
import com.tailormyresume.core.domain.ResumeTailor
import com.tailormyresume.core.domain.TailorResumeUseCase
import com.tailormyresume.core.model.CandidateProfile
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
import com.tailormyresume.core.testing.util.MainDispatcherRule
import com.tailormyresume.core.testing.util.TestClock
import com.tailormyresume.feature.tailor.impl.CleanFabricationGuard
import com.tailormyresume.feature.tailor.impl.entryFor
import com.tailormyresume.feature.tailor.impl.testApplication
import com.tailormyresume.feature.tailor.impl.testBullet
import com.tailormyresume.feature.tailor.impl.testProfile
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test

class TailoringViewModelTest {

    private val dispatcher = StandardTestDispatcher()

    @get:Rule
    val dispatcherRule = MainDispatcherRule(dispatcher)

    private val applicationRepository = TestApplicationRepository()
    private val profileRepository = TestProfileRepository()
    private val creditsRepository = TestCreditsRepository()
    private val clock = TestClock()

    private val gap = GapAnalysis(
        matches = listOf(
            RequirementMatch(
                requirement = JobRequirement(
                    id = "req-1",
                    text = "Must have SQL and Power BI reporting.",
                    type = RequirementType.TOOL,
                    priority = RequirementPriority.MUST_HAVE,
                    keywords = listOf("SQL", "Power BI"),
                ),
                status = MatchStatus.MET,
                evidenceIds = emptyList(),
            ),
        ),
        keywordCoverage = KeywordCoverage(covered = 1, total = 1),
    )

    private val profile = testProfile(listOf(entryFor("exp-1", testBullet("b1"))))

    private fun seed() {
        applicationRepository.sendApplications(listOf(testApplication(emptyList(), gap)))
        profileRepository.sendProfile(profile)
    }

    private fun fakeTailor(tailorMillis: Long, failing: Boolean): ResumeTailor = object : ResumeTailor {
        override suspend fun tailor(
            profile: CandidateProfile,
            job: JobDescription,
            gap: GapAnalysis,
            applicationId: String,
            answer: QuickAnswer?,
            runId: String,
        ): TailoredResume {
            delay(tailorMillis)
            if (failing) error("tailor failed")
            return TailoredResume(bullets = emptyList())
        }
    }

    private fun runner(tailorMillis: Long, failing: Boolean): TailoringRunner = TailoringRunner(
        applicationRepository = applicationRepository,
        profileRepository = profileRepository,
        creditsRepository = creditsRepository,
        tailorResume = TailorResumeUseCase(fakeTailor(tailorMillis, failing), CleanFabricationGuard),
        clock = clock,
        dispatcher = dispatcher,
    )

    private fun TestScope.collectUiState(viewModel: TailoringViewModel) {
        backgroundScope.launch(UnconfinedTestDispatcher()) { viewModel.uiState.collect() }
    }

    @Test
    fun progressRowsAtQuartersAndNeverFullBeforeResult() = runTest(dispatcher) {
        seed()
        val viewModel = TailoringViewModel(
            runner = runner(tailorMillis = 10_000, failing = false),
            applicationRepository = applicationRepository,
            applicationId = "app-1",
            runId = "run-1",
        )
        collectUiState(viewModel)
        runCurrent()

        assertThat(viewModel.progress.value).isEqualTo(0)
        assertThat(viewModel.uiState.value.rows.first().state).isEqualTo(TmrProgressState.Active)

        advanceTimeBy(1_000)
        runCurrent()
        assertThat(viewModel.progress.value).isEqualTo(25)
        assertThat(viewModel.uiState.value.rows.map { it.state }).containsExactly(
            TmrProgressState.Done,
            TmrProgressState.Active,
            TmrProgressState.Pending,
            TmrProgressState.Pending,
        ).inOrder()

        advanceTimeBy(1_000)
        runCurrent()
        assertThat(viewModel.progress.value).isEqualTo(50)

        advanceTimeBy(1_000)
        runCurrent()
        assertThat(viewModel.progress.value).isEqualTo(75)

        advanceTimeBy(6_000)
        runCurrent()
        assertThat(viewModel.progress.value).isEqualTo(99)
        assertThat(viewModel.uiState.value.rows.last().state).isEqualTo(TmrProgressState.Active)
        assertThat(viewModel.outcome.value).isNull()

        advanceTimeBy(1_000)
        runCurrent()
        assertThat(viewModel.progress.value).isEqualTo(100)
        assertThat(viewModel.outcome.value).isEqualTo(TailoringOutcome.Done)
        assertThat(viewModel.uiState.value.rows.map { it.state }).containsExactly(
            TmrProgressState.Done,
            TmrProgressState.Done,
            TmrProgressState.Done,
            TmrProgressState.Done,
        ).inOrder()
    }

    @Test
    fun successReplacesWithTailored() = runTest(dispatcher) {
        seed()
        val viewModel = TailoringViewModel(
            runner = runner(tailorMillis = 0, failing = false),
            applicationRepository = applicationRepository,
            applicationId = "app-1",
            runId = "run-1",
        )
        collectUiState(viewModel)
        runCurrent()

        assertThat(viewModel.outcome.value).isEqualTo(TailoringOutcome.Done)
        assertThat(viewModel.progress.value).isEqualTo(100)
        assertThat(viewModel.uiState.value.rows.map { it.state })
            .containsExactly(
                TmrProgressState.Done,
                TmrProgressState.Done,
                TmrProgressState.Done,
                TmrProgressState.Done,
            )
            .inOrder()
    }

    @Test
    fun failureEmitsFailedOutcome() = runTest(dispatcher) {
        seed()
        val viewModel = TailoringViewModel(
            runner = runner(tailorMillis = 0, failing = true),
            applicationRepository = applicationRepository,
            applicationId = "app-1",
            runId = "run-1",
        )
        collectUiState(viewModel)
        runCurrent()

        assertThat(viewModel.outcome.value).isEqualTo(TailoringOutcome.Failed)
        assertThat(viewModel.progress.value).isLessThan(100)
    }
}
