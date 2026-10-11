package com.tailormyresume.feature.analysis.impl.question

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import com.tailormyresume.core.domain.coverage.KeywordCoverageCalculator
import com.tailormyresume.core.model.BulletDecision
import com.tailormyresume.core.model.JobApplication
import com.tailormyresume.core.model.QuickAnswer
import com.tailormyresume.core.model.TailoredBullet
import com.tailormyresume.core.model.TailoredResume
import com.tailormyresume.core.testing.repository.TestApplicationRepository
import com.tailormyresume.core.testing.util.MainDispatcherRule
import com.tailormyresume.feature.analysis.impl.ResultTestData
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test

class QuickQuestionViewModelTest {

    @get:Rule
    val main = MainDispatcherRule()

    private val applicationRepository = TestApplicationRepository()

    @Test
    fun startsWithNothingPicked() = runTest {
        val viewModel = seeded()
        val state = readyState(viewModel)

        assertThat(state.question).isEqualTo(ResultTestData.question.text)
        assertThat(state.why).isEqualTo(ResultTestData.question.why)
        assertThat(state.picked).isNull()
        assertThat(state.detail).isEmpty()
        assertThat(state.showDetail).isFalse()
        assertThat(state.canContinue).isFalse()

        viewModel.onPick(QuickChoice.A_FEW_TIMES)

        assertThat(readyState(viewModel).canContinue).isTrue()
    }

    @Test
    fun noQuestionForwardsToTailoring() = runTest {
        seed(ResultTestData.application(question = null))

        assertThat(viewModel().uiState.value).isEqualTo(QuickQuestionUiState.NoQuestion)

        seed(ResultTestData.application().copy(gapAnalysis = null))

        assertThat(viewModel().uiState.value).isEqualTo(QuickQuestionUiState.NoQuestion)
    }

    @Test
    fun continueWithoutPickShowsToastAndStoresNothing() = runTest {
        val viewModel = seeded()
        val before = stored()

        viewModel.events.test {
            viewModel.onContinue()
            assertThat(awaitItem()).isEqualTo(QuickQuestionEvent.ToastPickAnswer)
            expectNoEvents()
            cancelAndIgnoreRemainingEvents()
        }

        assertThat(stored()).isEqualTo(before)
        assertThat(stored().quickAnswer).isNull()
    }

    @Test
    fun detailOnlyForYesAndFewAndCappedAt400() = runTest {
        val viewModel = seeded()
        val typed = "a".repeat(450)

        viewModel.onDetailChange(ResultTestData.STAKEHOLDER_DETAIL)
        assertThat(readyState(viewModel).detail).isEmpty()

        viewModel.onPick(QuickChoice.YES_REGULARLY)
        assertThat(readyState(viewModel).showDetail).isTrue()
        viewModel.onDetailChange(typed)
        assertThat(readyState(viewModel).detail).hasLength(400)
        assertThat(readyState(viewModel).detail).isEqualTo(typed.take(400))

        viewModel.onPick(QuickChoice.A_FEW_TIMES)
        assertThat(readyState(viewModel).showDetail).isTrue()
        assertThat(readyState(viewModel).detail).isEqualTo(typed.take(400))

        viewModel.onPick(QuickChoice.NOT_YET)
        assertThat(readyState(viewModel).showDetail).isFalse()
        assertThat(readyState(viewModel).detail).isEmpty()

        viewModel.onDetailChange(ResultTestData.STAKEHOLDER_DETAIL)
        assertThat(readyState(viewModel).detail).isEmpty()
    }

    @Test
    fun continueStoresAnswerAndForwards() = runTest {
        val before = ResultTestData.application()
        seed(before)
        val viewModel = viewModel()
        val answer = QuickAnswer(ResultTestData.STAKEHOLDER_ID, "YES_REGULARLY", ResultTestData.STAKEHOLDER_DETAIL)
        viewModel.onPick(QuickChoice.YES_REGULARLY)
        viewModel.onDetailChange("  ${ResultTestData.STAKEHOLDER_DETAIL}   ")

        viewModel.events.test {
            viewModel.onContinue()
            assertThat(awaitItem()).isEqualTo(QuickQuestionEvent.Tailor(ResultTestData.APP_ID))
            expectNoEvents()
            cancelAndIgnoreRemainingEvents()
        }

        assertThat(stored().quickAnswer).isEqualTo(answer)
        assertThat(stored().keywordCoverage)
            .isEqualTo(KeywordCoverageCalculator.compute(ResultTestData.matches, answer, null))
        assertThat(stored()).isEqualTo(
            before.copy(
                quickAnswer = answer,
                keywordCoverage = KeywordCoverageCalculator.compute(ResultTestData.matches, answer, null),
            ),
        )
    }

    @Test
    fun doubleTapOnContinueEmitsOneForward() = runTest {
        val viewModel = seeded()
        viewModel.onPick(QuickChoice.A_FEW_TIMES)

        viewModel.events.test {
            viewModel.onContinue()
            viewModel.onContinue()
            assertThat(awaitItem()).isEqualTo(QuickQuestionEvent.Tailor(ResultTestData.APP_ID))
            expectNoEvents()
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun doubleTapOnSkipEmitsOneForward() = runTest {
        val viewModel = seeded()

        viewModel.events.test {
            viewModel.onSkip()
            viewModel.onSkip()
            assertThat(awaitItem()).isEqualTo(QuickQuestionEvent.Tailor(ResultTestData.APP_ID))
            expectNoEvents()
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun continueThenSkipEmitsOneForward() = runTest {
        val viewModel = seeded()
        viewModel.onPick(QuickChoice.A_FEW_TIMES)

        viewModel.events.test {
            viewModel.onContinue()
            viewModel.onSkip()
            assertThat(awaitItem()).isEqualTo(QuickQuestionEvent.Tailor(ResultTestData.APP_ID))
            expectNoEvents()
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun skipStoresNoAnswerAndForwards() = runTest {
        val answered = ResultTestData.application(
            quickAnswer = QuickAnswer(ResultTestData.STAKEHOLDER_ID, "A_FEW_TIMES", ResultTestData.STAKEHOLDER_DETAIL),
        )
        seed(answered)
        val viewModel = viewModel()

        viewModel.events.test {
            viewModel.onSkip()
            assertThat(awaitItem()).isEqualTo(QuickQuestionEvent.Tailor(ResultTestData.APP_ID))
            expectNoEvents()
            cancelAndIgnoreRemainingEvents()
        }

        assertThat(stored().quickAnswer).isNull()
        assertThat(stored().keywordCoverage)
            .isEqualTo(KeywordCoverageCalculator.compute(ResultTestData.matches, null, null))
    }

    @Test
    fun storedAnswerRecomputesUpToViaCalculator() = runTest {
        val nullAnswerUpTo = KeywordCoverageCalculator.compute(ResultTestData.matches, null, null).upTo

        val blank = storedFor(QuickChoice.YES_REGULARLY, "   ")
        assertThat(blank.keywordCoverage)
            .isEqualTo(KeywordCoverageCalculator.compute(ResultTestData.matches, blank.quickAnswer, null))
        assertThat(blank.keywordCoverage?.upTo).isEqualTo(nullAnswerUpTo)

        val detailed = storedFor(QuickChoice.YES_REGULARLY, ResultTestData.STAKEHOLDER_DETAIL)
        assertThat(detailed.keywordCoverage)
            .isEqualTo(KeywordCoverageCalculator.compute(ResultTestData.matches, detailed.quickAnswer, null))
        assertThat(detailed.keywordCoverage?.upTo).isGreaterThan(nullAnswerUpTo)

        val notYet = storedFor(QuickChoice.NOT_YET, null)
        assertThat(notYet.quickAnswer?.detail).isEmpty()
        assertThat(notYet.keywordCoverage)
            .isEqualTo(KeywordCoverageCalculator.compute(ResultTestData.matches, notYet.quickAnswer, null))
        assertThat(notYet.keywordCoverage?.upTo).isEqualTo(nullAnswerUpTo)
    }

    @Test
    fun alreadyTailoredApplicationKeepsItsFinalCoverage() = runTest {
        val resume = TailoredResume(
            listOf(
                TailoredBullet(
                    id = "b1",
                    entryId = "x1",
                    originalText = "Built reports",
                    proposedText = "Built SQL and Excel reports",
                    sourceIds = emptyList(),
                    editTypes = emptyList(),
                    keywordsUsed = emptyList(),
                    violations = emptyList(),
                    decision = BulletDecision.ACCEPTED,
                ),
            ),
        )
        val storedFinal = KeywordCoverageCalculator.compute(ResultTestData.matches, null, resume).final
        assertThat(storedFinal).isNotNull()
        seed(
            ResultTestData.application(tailoredResume = resume)
                .copy(keywordCoverage = KeywordCoverageCalculator.compute(ResultTestData.matches, null, resume)),
        )
        val viewModel = viewModel()
        viewModel.onPick(QuickChoice.A_FEW_TIMES)

        viewModel.onContinue()

        assertThat(stored().keywordCoverage?.final).isEqualTo(storedFinal)
        viewModel.onSkip()
        assertThat(stored().keywordCoverage?.final).isEqualTo(storedFinal)
    }

    @Test
    fun continueRereadsTheApplicationRightBeforeSaving() = runTest {
        val viewModel = seeded()
        val moved = ResultTestData.application(location = "Remote · Berlin")
        applicationRepository.sendApplications(listOf(moved))

        viewModel.onPick(QuickChoice.A_FEW_TIMES)
        viewModel.onContinue()

        assertThat(stored().location).isEqualTo("Remote · Berlin")
        assertThat(stored().id).isEqualTo(ResultTestData.APP_ID)
    }

    private suspend fun TestScope.storedFor(
        choice: QuickChoice,
        detail: String?,
    ): JobApplication {
        seed(ResultTestData.application())
        val viewModel = viewModel()
        viewModel.onPick(choice)
        if (detail != null) viewModel.onDetailChange(detail)
        viewModel.onContinue()
        return stored()
    }

    private fun seed(application: JobApplication) {
        applicationRepository.sendApplications(listOf(application))
    }

    private fun TestScope.seeded(): QuickQuestionViewModel {
        seed(ResultTestData.application())
        return viewModel().also { readyState(it) }
    }

    private fun TestScope.viewModel() =
        QuickQuestionViewModel(applicationRepository, ResultTestData.APP_ID).also {
            backgroundScope.launch(UnconfinedTestDispatcher()) { it.uiState.collect() }
        }

    private suspend fun stored(): JobApplication =
        requireNotNull(applicationRepository.observeApplication(ResultTestData.APP_ID).first())

    private fun readyState(viewModel: QuickQuestionViewModel): QuickQuestionUiState.Ready {
        val state = viewModel.uiState.value
        assertThat(state).isInstanceOf(QuickQuestionUiState.Ready::class.java)
        return state as QuickQuestionUiState.Ready
    }
}
