package com.tailormyresume.feature.tailor.impl

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import com.tailormyresume.core.domain.UpdateBulletDecisionUseCase
import com.tailormyresume.core.model.BulletDecision
import com.tailormyresume.core.model.DebugScenario
import com.tailormyresume.core.model.TailoredResume
import com.tailormyresume.core.model.TailoredSkills
import com.tailormyresume.core.model.TailoredText
import com.tailormyresume.core.testing.repository.TestApplicationRepository
import com.tailormyresume.core.testing.repository.TestContentReportRepository
import com.tailormyresume.core.testing.repository.TestProfileRepository
import com.tailormyresume.core.testing.repository.TestTailoringReviewStateRepository
import com.tailormyresume.core.testing.util.MainDispatcherRule
import com.tailormyresume.core.testing.util.TestClock
import com.tailormyresume.feature.tailor.api.navigation.EditResumeNavKey
import com.tailormyresume.feature.tailor.api.navigation.ExportedNavKey
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test
import kotlin.time.Clock

class TailorViewModelDecisionTest {

    @get:Rule
    val dispatcherRule = MainDispatcherRule()

    private val applicationRepository = TestApplicationRepository()
    private val profileRepository = TestProfileRepository()
    private val reviewState = TestTailoringReviewStateRepository()
    private val reports = TestContentReportRepository()
    private val testClock = TestClock()

    private val firstBullet = testBullet(
        id = "b1",
        original = "Built an internal tool",
        proposed = "Developed an internal tool",
    )
    private val secondBullet = testBullet(
        id = "b2",
        original = "Wrote SQL reports",
        proposed = "Authored SQL reports",
    )
    private val summary = TailoredText(
        text = "Backend engineer focused on data platforms.",
        original = "Backend engineer.",
        sourceIds = listOf("src-summary"),
        decision = BulletDecision.PENDING,
    )
    private val skills = TailoredSkills(
        skills = listOf("Kotlin", "SQL", "Kafka"),
        original = listOf("Kotlin", "SQL"),
        decision = BulletDecision.PENDING,
    )

    private val profile = testProfile(listOf(entryFor("exp-1", firstBullet, secondBullet)))

    private fun viewModel(scenario: DebugScenario = DebugScenario.DEFAULT): TailorViewModel {
        val clock = Clock.System
        return TailorViewModel(
            applicationRepository = applicationRepository,
            profileRepository = profileRepository,
            reviewStateRepository = reviewState,
            contentReportRepository = reports,
            clock = testClock,
            updateBulletDecision = UpdateBulletDecisionUseCase(applicationRepository, clock),
            handEditBullet = HandEditBulletUseCase(applicationRepository, reviewState, clock),
            applicationId = "app-1",
            scenario = scenario,
        )
    }

    private fun sendPendingApplication() {
        applicationRepository.sendApplications(
            listOf(
                testApplication(listOf(firstBullet, secondBullet), entryIds = listOf("exp-1")).copy(
                    tailoredResume = TailoredResume(
                        bullets = listOf(firstBullet, secondBullet),
                        entryIds = listOf("exp-1"),
                        summary = summary,
                        skills = skills,
                    ),
                ),
            ),
        )
        profileRepository.sendProfile(profile)
    }

    private suspend fun currentResume(): TailoredResume {
        val application = applicationRepository.observeApplication("app-1").first()
        return application?.tailoredResume ?: error("app-1 has no tailored resume")
    }

    private fun TailoredResume.decisionOf(changeId: String): BulletDecision = when (changeId) {
        UpdateBulletDecisionUseCase.SUMMARY_CHANGE_ID -> summary?.decision ?: error("no summary change")
        UpdateBulletDecisionUseCase.SKILLS_CHANGE_ID -> skills?.decision ?: error("no skills change")
        else -> bullets.first { it.id == changeId }.decision
    }

    @Test
    fun undoChangeSetsRejected() = runTest {
        val viewModel = viewModel()
        sendPendingApplication()

        viewModel.onUndoChange("b1")

        val resume = currentResume()
        assertThat(resume.decisionOf("b1")).isEqualTo(BulletDecision.REJECTED)
        assertThat(resume.decisionOf("b2")).isEqualTo(BulletDecision.PENDING)
    }

    @Test
    fun undoSummaryAndSkillsSetRejected() = runTest {
        val viewModel = viewModel()
        sendPendingApplication()

        viewModel.onUndoChange(UpdateBulletDecisionUseCase.SUMMARY_CHANGE_ID)
        viewModel.onUndoChange(UpdateBulletDecisionUseCase.SKILLS_CHANGE_ID)

        val resume = currentResume()
        assertThat(resume.decisionOf(UpdateBulletDecisionUseCase.SUMMARY_CHANGE_ID))
            .isEqualTo(BulletDecision.REJECTED)
        assertThat(resume.decisionOf(UpdateBulletDecisionUseCase.SKILLS_CHANGE_ID))
            .isEqualTo(BulletDecision.REJECTED)
        assertThat(resume.decisionOf("b1")).isEqualTo(BulletDecision.PENDING)
        assertThat(resume.decisionOf("b2")).isEqualTo(BulletDecision.PENDING)
    }

    @Test
    fun acceptChangesAcceptsEveryPendingAndStampsChangesAcceptedAt() = runTest {
        val viewModel = viewModel()
        sendPendingApplication()
        viewModel.onUndoChange("b1")

        viewModel.onAcceptChanges()

        val resume = currentResume()
        assertThat(resume.decisionOf("b1")).isEqualTo(BulletDecision.REJECTED)
        assertThat(resume.decisionOf("b2")).isEqualTo(BulletDecision.ACCEPTED)
        assertThat(resume.decisionOf(UpdateBulletDecisionUseCase.SUMMARY_CHANGE_ID))
            .isEqualTo(BulletDecision.ACCEPTED)
        assertThat(resume.decisionOf(UpdateBulletDecisionUseCase.SKILLS_CHANGE_ID))
            .isEqualTo(BulletDecision.ACCEPTED)
        assertThat(resume.decisions).doesNotContain(BulletDecision.PENDING)
        val application = applicationRepository.observeApplication("app-1").first()
        assertThat(application?.changesAcceptedAt).isNotNull()
        assertThat(application?.changesAcceptedAt).isEqualTo(testClock.instant)
    }

    @Test
    fun exportRefusedBeforeAcceptShowsToastAndDoesNotNavigate() = runTest {
        val viewModel = viewModel()
        sendPendingApplication()

        viewModel.events.test {
            viewModel.onExportTapped()

            assertThat(awaitItem()).isEqualTo(TailorEvent.ExportBlocked)
            expectNoEvents()
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun exportAllowedAfterAcceptNavigates() = runTest {
        val viewModel = viewModel()
        sendPendingApplication()
        viewModel.onAcceptChanges()

        viewModel.events.test {
            viewModel.onExportTapped()

            assertThat(awaitItem()).isEqualTo(TailorEvent.Navigate(ExportedNavKey("app-1")))
            expectNoEvents()
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun editPushesEditResumeAndExportPushesExported() = runTest {
        val viewModel = viewModel()
        sendPendingApplication()

        viewModel.events.test {
            viewModel.onEditTapped()
            assertThat(awaitItem()).isEqualTo(TailorEvent.Navigate(EditResumeNavKey("app-1")))

            viewModel.onAcceptChanges()
            viewModel.onExportTapped()
            assertThat(awaitItem()).isEqualTo(TailorEvent.Navigate(ExportedNavKey("app-1")))

            expectNoEvents()
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun acceptAndUndoNeverInterleave() = runTest {
        val viewModel = viewModel()
        sendPendingApplication()

        viewModel.onUndoChange("b1")
        viewModel.onAcceptChanges()

        val resume = currentResume()
        assertThat(resume.decisionOf("b1")).isEqualTo(BulletDecision.REJECTED)
        assertThat(resume.decisionOf("b2")).isEqualTo(BulletDecision.ACCEPTED)
        assertThat(resume.decisionOf(UpdateBulletDecisionUseCase.SUMMARY_CHANGE_ID))
            .isEqualTo(BulletDecision.ACCEPTED)
        assertThat(resume.decisionOf(UpdateBulletDecisionUseCase.SKILLS_CHANGE_ID))
            .isEqualTo(BulletDecision.ACCEPTED)
        val application = applicationRepository.observeApplication("app-1").first()
        assertThat(application?.changesAcceptedAt).isNotNull()
    }
}
