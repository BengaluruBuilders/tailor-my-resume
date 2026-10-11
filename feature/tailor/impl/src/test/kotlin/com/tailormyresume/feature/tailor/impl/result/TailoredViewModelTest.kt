package com.tailormyresume.feature.tailor.impl.result

import com.google.common.truth.Truth.assertThat
import com.tailormyresume.core.domain.AcceptChangesUseCase
import com.tailormyresume.core.domain.UpdateBulletDecisionUseCase
import com.tailormyresume.core.domain.coverage.KeywordCoverageCalculator
import com.tailormyresume.core.model.BulletDecision
import com.tailormyresume.core.model.GapAnalysis
import com.tailormyresume.core.model.JobRequirement
import com.tailormyresume.core.model.KeywordCoverage
import com.tailormyresume.core.model.MatchStatus
import com.tailormyresume.core.model.RequirementMatch
import com.tailormyresume.core.model.RequirementPriority
import com.tailormyresume.core.model.RequirementType
import com.tailormyresume.core.model.TailoredResume
import com.tailormyresume.core.model.TailoredSkills
import com.tailormyresume.core.model.TailoredText
import com.tailormyresume.core.testing.repository.TestApplicationRepository
import com.tailormyresume.core.testing.repository.TestProfileRepository
import com.tailormyresume.core.testing.util.MainDispatcherRule
import com.tailormyresume.core.testing.util.TestClock
import com.tailormyresume.feature.tailor.impl.document.ResumeDocumentAssembler
import com.tailormyresume.feature.tailor.impl.document.TestResumeHeadings
import com.tailormyresume.feature.tailor.impl.entryFor
import com.tailormyresume.feature.tailor.impl.testApplication
import com.tailormyresume.feature.tailor.impl.testBullet
import com.tailormyresume.feature.tailor.impl.testProfile
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test

class TailoredViewModelTest {
    @get:Rule
    val dispatcherRule = MainDispatcherRule()

    private val applicationRepository = TestApplicationRepository()
    private val profileRepository = TestProfileRepository()
    private val clock = TestClock()

    private val bullet = testBullet("b1", original = "Built an internal tool", proposed = "Developed an internal tool")
        .copy(keywordsUsed = listOf("internal tool"))

    private val gap = GapAnalysis(
        matches = listOf(
            RequirementMatch(
                JobRequirement("req-1", "Internal tools", RequirementType.TOOL, RequirementPriority.MUST_HAVE, listOf("internal tool")),
                MatchStatus.MET,
                emptyList(),
            ),
        ),
        keywordCoverage = KeywordCoverage(1, 1),
    )

    private fun viewModel() = TailoredViewModel(
        applicationRepository = applicationRepository,
        profileRepository = profileRepository,
        assembler = ResumeDocumentAssembler(TestResumeHeadings),
        applicationId = "app-1",
    )

    private fun seed() {
        applicationRepository.sendApplications(listOf(testApplication(listOf(bullet), gap)))
        profileRepository.sendProfile(testProfile(listOf(entryFor("exp-1", bullet))))
    }

    private fun TailoredViewModel.ready(): TailoredUiState.Ready {
        val current = state.value
        assertThat(current).isInstanceOf(TailoredUiState.Ready::class.java)
        return current as TailoredUiState.Ready
    }

    @Test
    fun readyStateShowsChangesCoverageAndKeepsExportClosedUntilAccept() = runTest {
        val viewModel = viewModel()
        backgroundScope.launch(UnconfinedTestDispatcher()) { viewModel.state.collect() }
        seed()

        val before = viewModel.ready()
        assertThat(before.jobTitle).isEqualTo("Backend Engineer")
        assertThat(before.company).isEqualTo("Acme")
        assertThat(before.changes.map { it.id }).containsExactly("b1")
        assertThat(before.accepted).isFalse()
        assertThat(before.exportEnabled).isFalse()
        val pendingAccepted = TailoredResume(listOf(bullet)).withPendingAccepted()
        assertThat(before.coveragePercent)
            .isEqualTo(KeywordCoverageCalculator.compute(gap.matches, null, pendingAccepted).final)
        assertThat(before.blocks.flatMap { it.lines }.map { line -> line.joinToString("") { it.text } })
            .contains("Developed an internal tool")

        AcceptChangesUseCase(applicationRepository, clock)("app-1")

        val after = viewModel.ready()
        assertThat(after.accepted).isTrue()
        assertThat(after.exportEnabled).isTrue()
    }

    @Test
    fun undoneChangeShowsTheOriginalInThePaper() = runTest {
        val viewModel = viewModel()
        backgroundScope.launch(UnconfinedTestDispatcher()) { viewModel.state.collect() }
        seed()

        UpdateBulletDecisionUseCase(applicationRepository, clock)("app-1", "b1", BulletDecision.REJECTED)

        val ready = viewModel.ready()
        assertThat(ready.changes.single().undone).isTrue()
        assertThat(ready.blocks.flatMap { it.lines }.map { line -> line.joinToString("") { it.text } })
            .contains("Built an internal tool")
        assertThat(applicationRepository.observeApplication("app-1").first()?.tailoredResume?.bullets?.single()?.decision)
            .isEqualTo(BulletDecision.REJECTED)
    }

    @Test
    fun rejectedSummaryShowsTheProfilesOriginalOnTheResumeTab() = runTest {
        val viewModel = viewModel()
        backgroundScope.launch(UnconfinedTestDispatcher()) { viewModel.state.collect() }
        val summary = TailoredText("New summary", "Old", decision = BulletDecision.REJECTED)
        val skills = TailoredSkills(listOf("Kafka"), listOf("Kotlin", "SQL"), decision = BulletDecision.REJECTED)
        applicationRepository.sendApplications(
            listOf(
                testApplication(listOf(bullet), gap).copy(
                    tailoredResume = TailoredResume(listOf(bullet), summary = summary, skills = skills),
                ),
            ),
        )
        profileRepository.sendProfile(testProfile(listOf(entryFor("exp-1", bullet))).copy(summary = "My own words."))

        val lines = viewModel.ready().blocks.flatMap { it.lines }.map { line -> line.joinToString("") { it.text } }

        assertThat(lines).contains("My own words.")
        assertThat(lines).doesNotContain("New summary")
    }

    @Test
    fun missingApplicationIsNotFound() = runTest {
        val viewModel = viewModel()
        backgroundScope.launch(UnconfinedTestDispatcher()) { viewModel.state.collect() }
        profileRepository.sendProfile(testProfile(emptyList()))

        assertThat(viewModel.state.value).isEqualTo(TailoredUiState.NotFound)
    }
}
