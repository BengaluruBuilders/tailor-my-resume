package com.tailormyresume.feature.tailor.impl

import com.google.common.truth.Truth.assertThat
import com.tailormyresume.core.domain.ExportCheck
import com.tailormyresume.core.domain.ExportReadiness
import com.tailormyresume.core.domain.UpdateBulletDecisionUseCase
import com.tailormyresume.core.model.BulletDecision
import com.tailormyresume.core.model.DebugScenario
import com.tailormyresume.core.model.EditType
import com.tailormyresume.core.model.EntryCategory
import com.tailormyresume.core.model.GapAnalysis
import com.tailormyresume.core.model.GuardrailViolation
import com.tailormyresume.core.model.JobRequirement
import com.tailormyresume.core.model.KeywordCoverage
import com.tailormyresume.core.model.MatchStatus
import com.tailormyresume.core.model.ReportedItemKind
import com.tailormyresume.core.model.RequirementMatch
import com.tailormyresume.core.model.RequirementPriority
import com.tailormyresume.core.model.RequirementType
import com.tailormyresume.core.model.TailoredBullet
import com.tailormyresume.core.testing.repository.TestApplicationRepository
import com.tailormyresume.core.testing.repository.TestContentReportRepository
import com.tailormyresume.core.testing.repository.TestProfileRepository
import com.tailormyresume.core.testing.repository.TestTailoringReviewStateRepository
import com.tailormyresume.core.testing.util.MainDispatcherRule
import com.tailormyresume.core.testing.util.TestClock
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test
import kotlin.time.Clock

class TailorViewModelTest {
    @get:Rule
    val dispatcherRule = MainDispatcherRule()

    private val applicationRepository = TestApplicationRepository()
    private val profileRepository = TestProfileRepository()
    private val reviewState = TestTailoringReviewStateRepository()
    private val reports = TestContentReportRepository()
    private val testClock = TestClock()

    private val reviewable = testBullet("r1", original = "Built a tool", proposed = "Developed a tool")
    private val alreadyRejected = testBullet(
        id = "r2",
        original = "Wrote tests",
        proposed = "Authored tests",
        decision = BulletDecision.REJECTED,
    )
    private val unchanged = testBullet("u1", original = "Wrote SQL", proposed = "Wrote SQL")
    private val repairFailed = testBullet(
        id = "v1",
        original = "Helped a team",
        proposed = "Helped a team",
        violations = listOf(GuardrailViolation.UnsupportedNumber("30")),
    )
    private val flagged = testBullet(
        id = "f1",
        original = "Made weekly reports",
        proposed = "Built weekly reports",
        violations = listOf(GuardrailViolation.VerbEscalation("made", "built")),
    )
    private val moveOnly = testBullet(
        id = "m1",
        original = "Presented at the fest",
        proposed = "Presented at the fest",
        editTypes = listOf(EditType.REORDER),
    )
    private val staleSource = testBullet("s1", original = "Text before the edit", proposed = "Reworded text")
    private val hidden = testBullet("h1", entryId = "exp-hidden", original = "Old job", proposed = "Older job")

    private val profile = testProfile(
        listOf(
            entryFor("exp-1", reviewable, alreadyRejected, unchanged, repairFailed, flagged, moveOnly, staleSource),
            testEntry("exp-hidden", isConfirmed = false, bullets = listOf(evidenceOf(hidden))),
            testEntry("edu-1", EntryCategory.EDUCATION),
        ),
    )

    private val gap = GapAnalysis(
        matches = listOf(
            RequirementMatch(
                requirement = JobRequirement(
                    id = "req-1",
                    text = "Must have Cloud data warehouse.",
                    type = RequirementType.TOOL,
                    priority = RequirementPriority.MUST_HAVE,
                    keywords = emptyList(),
                ),
                status = MatchStatus.GAP,
                evidenceIds = emptyList(),
            ),
        ),
        keywordCoverage = KeywordCoverage(covered = 0, total = 1),
    )

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

    private fun TestScope.collectUiState(viewModel: TailorViewModel) {
        backgroundScope.launch(UnconfinedTestDispatcher()) { viewModel.uiState.collect() }
    }

    private fun sendData(
        bullets: List<TailoredBullet>,
        gapAnalysis: GapAnalysis? = gap,
        entryIds: List<String>? = null,
    ) {
        applicationRepository.sendApplications(listOf(testApplication(bullets, gapAnalysis, entryIds)))
        profileRepository.sendProfile(profile)
    }

    private fun TailorViewModel.success(): TailorUiState.Success {
        val state = uiState.value
        assertThat(state).isInstanceOf(TailorUiState.Success::class.java)
        return state as TailorUiState.Success
    }

    private fun TailorViewModel.bulletOf(bulletId: String): TailorBulletUi =
        success().sections.filterIsInstance<ReviewSection.Entries>()
            .flatMap { it.entries }
            .flatMap { it.bullets }
            .first { it.bullet.id == bulletId }

    @Test
    fun applicationId_isTheAssistedKey() {
        assertThat(viewModel().applicationId).isEqualTo("app-1")
    }

    @Test
    fun uiState_startsLoading() {
        assertThat(viewModel().uiState.value).isEqualTo(TailorUiState.Loading())
    }

    @Test
    fun uiState_isNotFoundWhenTheApplicationIsMissing() = runTest {
        val viewModel = viewModel()
        collectUiState(viewModel)

        applicationRepository.sendApplications(emptyList())
        profileRepository.sendProfile(profile)

        assertThat(viewModel.uiState.value).isEqualTo(TailorUiState.NotFound)
    }

    @Test
    fun uiState_groupsBulletsByConfirmedEntryOnly() = runTest {
        val viewModel = viewModel()
        collectUiState(viewModel)

        sendData(listOf(reviewable, unchanged, hidden), entryIds = listOf("exp-1"))

        val entries = viewModel.success().sections.filterIsInstance<ReviewSection.Entries>().flatMap { it.entries }
        assertThat(entries.map { it.entryId }).containsExactly("exp-1")
        assertThat(entries.last().bullets.map { it.bullet.id }).containsExactly("r1", "u1").inOrder()
    }

    @Test
    fun uiState_leavesOutAConfirmedEntryTheTailoringNeverCovered() = runTest {
        val viewModel = viewModel()
        collectUiState(viewModel)

        sendData(listOf(reviewable), entryIds = listOf("exp-1"))

        val sections = viewModel.success().sections.filterIsInstance<ReviewSection.Entries>()
        assertThat(sections.map { it.category }).containsExactly(EntryCategory.EXPERIENCE)
    }

    @Test
    fun uiState_countsReviewableAndRepairFailedChangesAndTreatsRepairFailedAsReviewed() = runTest {
        val viewModel = viewModel()
        collectUiState(viewModel)

        sendData(listOf(reviewable, alreadyRejected, unchanged, repairFailed))

        assertThat(viewModel.success().totalCount).isEqualTo(3)
        assertThat(viewModel.success().reviewedCount).isEqualTo(2)
        assertThat(viewModel.success().openCount).isEqualTo(1)
    }

    @Test
    fun uiState_classifiesBulletKinds() = runTest {
        val viewModel = viewModel()
        collectUiState(viewModel)

        sendData(listOf(reviewable, unchanged, repairFailed, flagged, moveOnly))

        val kinds = viewModel.success().sections.filterIsInstance<ReviewSection.Entries>()
            .flatMap { it.entries }.flatMap { it.bullets }.associate { it.bullet.id to it.state }
        assertThat(kinds).containsExactly(
            "r1",
            BulletReviewState.TO_REVIEW,
            "u1",
            BulletReviewState.UNCHANGED,
            "v1",
            BulletReviewState.REPAIR_FAILED,
            "f1",
            BulletReviewState.FLAGGED,
            "m1",
            BulletReviewState.TO_REVIEW,
        )
        assertThat(viewModel.success().flaggedCount).isEqualTo(1)
    }

    @Test
    fun uiState_listsGapRequirementsAsNotAddedWithoutMarkerOrClosingPunctuation() = runTest {
        val viewModel = viewModel()
        collectUiState(viewModel)

        sendData(listOf(reviewable))

        assertThat(viewModel.success().notAdded).containsExactly("Cloud data warehouse")
    }

    @Test
    fun uiState_resolvesSourceFactsForEachBullet() = runTest {
        val viewModel = viewModel()
        collectUiState(viewModel)

        sendData(listOf(reviewable))

        val source = viewModel.bulletOf("r1").sources.single()
        assertThat(source.id).isEqualTo("src-r1")
        assertThat(source.text).isEqualTo("Built a tool")
    }

    @Test
    fun uiState_showsTheDesignIdOfTheSourceFactNotTheBulletId() = runTest {
        val viewModel = viewModel()
        collectUiState(viewModel)

        sendData(listOf(reviewable))

        val source = viewModel.bulletOf("r1").sources.single()
        assertThat(source.displayId).isEqualTo("W-01")
        assertThat(source.entryId).isEqualTo("exp-1")
    }

    @Test
    fun onReportBullet_savesTheReportAndMarksTheBulletAsReported() = runTest {
        val viewModel = viewModel()
        collectUiState(viewModel)
        sendData(listOf(reviewable))

        viewModel.onReportBullet("r1")

        val saved = reports.observeReports("app-1").first().single()
        assertThat(saved.itemKind).isEqualTo(ReportedItemKind.RESUME_BULLET)
        assertThat(saved.itemId).isEqualTo("r1")
        assertThat(saved.reportedAt).isEqualTo(testClock.instant)
        assertThat(viewModel.success().reportedIds).containsExactly("r1")
    }

    @Test
    fun onReportSection_marksTheSectionAsReported() = runTest {
        val viewModel = viewModel()
        collectUiState(viewModel)
        sendData(listOf(reviewable))

        viewModel.onReportSection("EXPERIENCE")

        val saved = reports.observeReports("app-1").first().single()
        assertThat(saved.itemKind).isEqualTo(ReportedItemKind.SECTION)
        assertThat(saved.itemId).isEqualTo("EXPERIENCE")
        assertThat(viewModel.success().reportedIds).containsExactly(sectionReportId("EXPERIENCE"))
    }

    @Test
    fun reportedBullet_staysReportedForANewViewModel() = runTest {
        sendData(listOf(reviewable))
        val first = viewModel()
        collectUiState(first)
        first.onReportBullet("r1")
        val reopened = viewModel()
        collectUiState(reopened)

        assertThat(reopened.success().reportedIds).containsExactly("r1")
    }

    @Test
    fun onUndoChange_marksTheBulletRejectedAndCountsItAsReviewed() = runTest {
        val viewModel = viewModel()
        collectUiState(viewModel)
        sendData(listOf(reviewable, unchanged))

        viewModel.onUndoChange("r1")

        assertThat(viewModel.bulletOf("r1").state).isEqualTo(BulletReviewState.ORIGINAL_KEPT)
        assertThat(viewModel.success().reviewedCount).isEqualTo(1)
    }

    @Test
    fun onAcceptChanges_acceptsEveryPendingBulletAndKeepsRejectedOnesRejected() = runTest {
        val viewModel = viewModel()
        collectUiState(viewModel)
        sendData(listOf(reviewable, alreadyRejected, flagged, unchanged))

        viewModel.onAcceptChanges()

        assertThat(viewModel.bulletOf("r1").state).isEqualTo(BulletReviewState.ACCEPTED)
        assertThat(viewModel.bulletOf("f1").state).isEqualTo(BulletReviewState.ACCEPTED)
        assertThat(viewModel.bulletOf("r2").state).isEqualTo(BulletReviewState.ORIGINAL_KEPT)
        assertThat(viewModel.bulletOf("u1").state).isEqualTo(BulletReviewState.UNCHANGED)
        assertThat(viewModel.success().isAllReviewed).isTrue()
    }

    @Test
    fun onAcceptChanges_afterAnUndoLeavesTheUndoneBulletRejected() = runTest {
        val viewModel = viewModel()
        collectUiState(viewModel)
        sendData(listOf(reviewable, flagged))

        viewModel.onUndoChange("r1")
        viewModel.onAcceptChanges()

        assertThat(viewModel.bulletOf("r1").state).isEqualTo(BulletReviewState.ORIGINAL_KEPT)
        assertThat(viewModel.bulletOf("f1").state).isEqualTo(BulletReviewState.ACCEPTED)
    }

    @Test
    fun exportStaysRefusedUntilChangesAreAcceptedThenOpensEvenAfterAnUndo() = runTest {
        val viewModel = viewModel()
        collectUiState(viewModel)
        sendData(listOf(reviewable, flagged))
        suspend fun check() = ExportReadiness.check(checkNotNull(applicationRepository.observeApplication("app-1").first()))

        assertThat(check()).isEqualTo(ExportCheck.NOT_ACCEPTED)

        viewModel.onUndoChange("r1")
        assertThat(check()).isEqualTo(ExportCheck.NOT_ACCEPTED)

        viewModel.onAcceptChanges()
        assertThat(check()).isEqualTo(ExportCheck.ALLOWED)
        assertThat(checkNotNull(applicationRepository.observeApplication("app-1").first()).changesAcceptedAt)
            .isEqualTo(testClock.instant)

        viewModel.onUndoChange("f1")
        assertThat(check()).isEqualTo(ExportCheck.ALLOWED)
    }

    @Test
    fun nextOpenChange_skipsReviewedChangesAndWrapsAround() = runTest {
        val viewModel = viewModel()
        collectUiState(viewModel)
        sendData(listOf(reviewable, alreadyRejected, flagged))

        assertThat(viewModel.success().nextOpenChange()?.bullet?.id).isEqualTo("r1")
        assertThat(viewModel.success().nextOpenChange("r1")?.bullet?.id).isEqualTo("f1")
        assertThat(viewModel.success().nextOpenChange("f1")?.bullet?.id).isEqualTo("r1")
    }

    @Test
    fun onEditByHand_storesTheTextAsAcceptedUserEditedAndIgnoresBlankText() = runTest {
        val viewModel = viewModel()
        collectUiState(viewModel)
        sendData(listOf(repairFailed))

        viewModel.onEditByHand("v1", "   ")
        assertThat(viewModel.bulletOf("v1").state).isEqualTo(BulletReviewState.REPAIR_FAILED)

        viewModel.onEditByHand("v1", " Coordinated a team ")

        val edited = viewModel.bulletOf("v1")
        assertThat(edited.state).isEqualTo(BulletReviewState.USER_EDITED)
        assertThat(edited.bullet.proposedText).isEqualTo("Coordinated a team")
        assertThat(edited.bullet.violations).isEmpty()
        assertThat(viewModel.success().changes.map { it.bullet.id }).containsExactly("v1")
    }

    @Test
    fun scenarioLoading_forcesTheLoadingStateWithCounts() = runTest {
        val viewModel = viewModel(DebugScenario.LOADING)
        collectUiState(viewModel)

        sendData(listOf(reviewable, unchanged))

        val state = viewModel.uiState.value as TailorUiState.Loading
        assertThat(state.lineCount).isEqualTo(2)
        assertThat(state.job?.company).isEqualTo("Acme")
    }

    @Test
    fun scenarioError_forcesTheFailedStateUntilRetry() = runTest {
        val viewModel = viewModel(DebugScenario.ERROR)
        collectUiState(viewModel)
        sendData(listOf(reviewable))
        assertThat(viewModel.uiState.value).isInstanceOf(TailorUiState.Failed::class.java)

        viewModel.onRetry()

        assertThat(viewModel.uiState.value).isInstanceOf(TailorUiState.Success::class.java)
    }
}
