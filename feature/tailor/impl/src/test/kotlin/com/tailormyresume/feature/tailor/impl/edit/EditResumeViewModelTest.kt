package com.tailormyresume.feature.tailor.impl.edit

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import com.tailormyresume.core.domain.ExportCheck
import com.tailormyresume.core.domain.ExportReadiness
import com.tailormyresume.core.model.BulletDecision
import com.tailormyresume.core.model.PageSize
import com.tailormyresume.core.model.TailoredBullet
import com.tailormyresume.core.model.TailoredText
import com.tailormyresume.core.testing.repository.TestApplicationRepository
import com.tailormyresume.core.testing.repository.TestProfileRepository
import com.tailormyresume.core.testing.repository.TestResumeSettingsRepository
import com.tailormyresume.core.testing.repository.TestTailoringReviewStateRepository
import com.tailormyresume.core.testing.util.MainDispatcherRule
import com.tailormyresume.core.testing.util.TestClock
import com.tailormyresume.feature.tailor.impl.EXPORT_ACCEPTED_AT
import com.tailormyresume.feature.tailor.impl.HandEditBulletUseCase
import com.tailormyresume.feature.tailor.impl.acceptedApplication
import com.tailormyresume.feature.tailor.impl.acceptedBullet
import com.tailormyresume.feature.tailor.impl.document.ResumeDocumentAssembler
import com.tailormyresume.feature.tailor.impl.document.TestResumeHeadings
import com.tailormyresume.feature.tailor.impl.entryFor
import com.tailormyresume.feature.tailor.impl.export.FakeResumePdfRenderer
import com.tailormyresume.feature.tailor.impl.result.ChangeSource
import com.tailormyresume.feature.tailor.impl.testProfile
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class EditResumeViewModelTest {
    @get:Rule
    val dispatcherRule = MainDispatcherRule()

    @get:Rule
    val temporaryFolder = TemporaryFolder()

    private val applicationRepository = TestApplicationRepository()
    private val profileRepository = TestProfileRepository()
    private val settingsRepository = TestResumeSettingsRepository()
    private val reviewStateRepository = TestTailoringReviewStateRepository()
    private val clock = TestClock()
    private val renderer by lazy { FakeResumePdfRenderer(temporaryFolder.root) }

    private val b1 = acceptedBullet("b1", original = "Built an internal tool", proposed = "Developed an internal tool")
    private val b2 = acceptedBullet(
        "b2",
        original = "Ran weekly reports",
        proposed = "Presented weekly reports",
        sourceId = "ans-1",
    )
    private val summary = TailoredText(
        text = "Tailored summary",
        original = "Old summary",
        decision = BulletDecision.ACCEPTED,
    )

    private fun viewModel() = EditResumeViewModel(
        applicationRepository = applicationRepository,
        profileRepository = profileRepository,
        settingsRepository = settingsRepository,
        assembler = ResumeDocumentAssembler(TestResumeHeadings),
        handEditBullet = HandEditBulletUseCase(applicationRepository, reviewStateRepository, clock),
        renderer = renderer,
        applicationId = "app-1",
    )

    private fun seed() {
        applicationRepository.sendApplications(listOf(acceptedApplication(listOf(b1, b2), summary = summary)))
        profileRepository.sendProfile(testProfile(listOf(entryFor("exp-1", b1, b2))))
    }

    private fun EditResumeViewModel.ready(): EditResumeUiState.Ready {
        val current = state.value
        assertThat(current).isInstanceOf(EditResumeUiState.Ready::class.java)
        return current as EditResumeUiState.Ready
    }

    private fun storedBullet(id: String): TailoredBullet? = runBlocking {
        applicationRepository.observeApplication("app-1").first()
            ?.tailoredResume?.bullets?.firstOrNull { it.id == id }
    }

    @Test
    fun readyStateShowsSummaryBulletsSourceTagsAndSkills() = runTest {
        val viewModel = viewModel()
        backgroundScope.launch(UnconfinedTestDispatcher()) { viewModel.state.collect() }
        seed()

        val ready = viewModel.ready()

        assertThat(ready.summary).isEqualTo("Tailored summary")
        assertThat(ready.roles).hasSize(1)
        assertThat(ready.roles.single().label).isEqualTo("Title exp-1 · Org exp-1")
        assertThat(ready.roles.single().bullets).containsExactly(
            EditBullet("b1", "Developed an internal tool", ChangeSource.YourResume),
            EditBullet("b2", "Presented weekly reports", ChangeSource.YourAnswer),
        ).inOrder()
        assertThat(ready.skills).isEqualTo("Kotlin, SQL")
        assertThat(ready.fitsOnOnePage).isTrue()
    }

    @Test
    fun saveStoresUserEditedThroughUseCaseAndEmitsChangesSaved() = runTest {
        val viewModel = viewModel()
        backgroundScope.launch(UnconfinedTestDispatcher()) { viewModel.state.collect() }
        seed()

        viewModel.events.test {
            viewModel.onBulletChange("b1", "My own words")
            viewModel.onSave()

            assertThat(awaitItem()).isEqualTo(EditResumeEvent.Saved)
            expectNoEvents()
            cancelAndIgnoreRemainingEvents()
        }

        assertThat(storedBullet("b1")?.proposedText).isEqualTo("My own words")
        assertThat(storedBullet("b1")?.decision).isEqualTo(BulletDecision.ACCEPTED)
        val edited = reviewStateRepository.observe("app-1").first().editedBulletIds
        assertThat(edited).contains("b1")
        assertThat(edited).doesNotContain("b2")
    }

    @Test
    fun editAfterAcceptKeepsExportAllowed() = runTest {
        val viewModel = viewModel()
        backgroundScope.launch(UnconfinedTestDispatcher()) { viewModel.state.collect() }
        seed()

        viewModel.onBulletChange("b1", "My own words")
        viewModel.onSave()

        val application = applicationRepository.observeApplication("app-1").first()
        assertThat(application).isNotNull()
        assertThat(application?.let { ExportReadiness.check(it) }).isEqualTo(ExportCheck.ALLOWED)
        assertThat(application?.changesAcceptedAt).isEqualTo(EXPORT_ACCEPTED_AT)
    }

    @Test
    fun overflowHidesStillFitsOnOnePage() = runTest {
        val viewModel = viewModel()
        backgroundScope.launch(UnconfinedTestDispatcher()) { viewModel.state.collect() }
        seed()
        assertThat(viewModel.ready().fitsOnOnePage).isTrue()

        renderer.pageCount = 2
        viewModel.onBulletChange("b1", "A much longer bullet")
        advanceTimeBy(FIT_SETTLE_MS)
        assertThat(viewModel.ready().fitsOnOnePage).isFalse()

        renderer.pageCount = 1
        viewModel.onBulletChange("b1", "Another long bullet")
        advanceTimeBy(FIT_SETTLE_MS)
        assertThat(viewModel.ready().fitsOnOnePage).isTrue()
    }

    @Test
    fun typingBurstRunsOneFitPassAfterTheLastKeystroke() = runTest {
        val viewModel = viewModel()
        backgroundScope.launch(UnconfinedTestDispatcher()) { viewModel.state.collect() }
        seed()
        val passesBefore = renderer.pageCountCalls

        "Typed bullet".forEachIndexed { index, _ ->
            viewModel.onBulletChange("b1", "Typed bullet".take(index + 1))
            advanceTimeBy(50)
        }
        assertThat(renderer.pageCountCalls).isEqualTo(passesBefore)

        advanceTimeBy(FIT_SETTLE_MS)

        assertThat(renderer.pageCountCalls).isEqualTo(passesBefore + 1)
    }

    @Test
    fun fitUsesTheSettingsPageSize() = runTest {
        val viewModel = viewModel()
        backgroundScope.launch(UnconfinedTestDispatcher()) { viewModel.state.collect() }
        seed()
        settingsRepository.update { it.copy(pageSize = PageSize.LETTER) }

        viewModel.onBulletChange("b1", "A much longer bullet")
        advanceTimeBy(FIT_SETTLE_MS)

        assertThat(renderer.lastPageSize).isEqualTo(PageSize.LETTER)
    }

    @Test
    fun rejectedSummaryFallsBackToTheProfileOriginal() = runTest {
        val rejected = summary.copy(decision = BulletDecision.REJECTED)
        applicationRepository.sendApplications(listOf(acceptedApplication(listOf(b1, b2), summary = rejected)))
        profileRepository.sendProfile(
            testProfile(listOf(entryFor("exp-1", b1, b2))).copy(summary = "My own words."),
        )
        val viewModel = viewModel()
        backgroundScope.launch(UnconfinedTestDispatcher()) { viewModel.state.collect() }

        assertThat(viewModel.ready().summary).isEqualTo("My own words.")
    }

    @Test
    fun blankEditIsIgnored() = runTest {
        val viewModel = viewModel()
        backgroundScope.launch(UnconfinedTestDispatcher()) { viewModel.state.collect() }
        seed()

        viewModel.onBulletChange("b1", "   ")
        viewModel.onSave()

        assertThat(storedBullet("b1")?.proposedText).isEqualTo("Developed an internal tool")
    }
}

private const val FIT_SETTLE_MS = 400L
