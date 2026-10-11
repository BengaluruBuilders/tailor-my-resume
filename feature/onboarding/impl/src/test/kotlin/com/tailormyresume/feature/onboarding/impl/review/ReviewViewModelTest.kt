package com.tailormyresume.feature.onboarding.impl.review

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import com.tailormyresume.core.data.repository.ProfileRepository
import com.tailormyresume.core.domain.onboarding.MarkProfileReviewedUseCase
import com.tailormyresume.core.model.CandidateProfile
import com.tailormyresume.core.model.EntryCategory
import com.tailormyresume.core.testing.repository.TestProfileRepository
import com.tailormyresume.core.testing.util.MainDispatcherRule
import com.tailormyresume.core.testing.util.TestClock
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test

class ReviewViewModelTest {

    @get:Rule
    val dispatcherRule = MainDispatcherRule()

    private class RecordingProfileRepository(
        private val delegate: TestProfileRepository,
        private val gate: CompletableDeferred<Unit>? = null,
    ) : ProfileRepository by delegate {
        var saves = 0

        override suspend fun saveProfileIfUnchanged(expected: CandidateProfile, replacement: CandidateProfile): Boolean {
            saves += 1
            gate?.await()
            return delegate.saveProfileIfUnchanged(expected, replacement)
        }
    }

    private val store = TestProfileRepository()
    private val clock = TestClock()

    private fun viewModel(repository: ProfileRepository = store) =
        ReviewViewModel(repository, MarkProfileReviewedUseCase(repository, clock))

    private suspend fun ReviewViewModel.ready(): ReviewUiState.Ready =
        uiState.first { it is ReviewUiState.Ready } as ReviewUiState.Ready

    private suspend fun saved() = checkNotNull(store.observeProfile().first())

    @Test
    fun fixedProfileShowsDoneBannerEnabledCtaAnd96And100WithLinkedin() = runTest {
        store.sendProfile(fixedReviewProfile())
        val viewModel = viewModel()

        val fixed = viewModel.ready()
        assertThat(fixed.gap).isNull()
        assertThat(fixed.canContinue).isTrue()
        assertThat(fixed.percent).isEqualTo(96)

        viewModel.uiState.test {
            skipItems(1)
            store.sendProfile(fixedReviewProfile().copy(linkedinUrl = SAMPLE_LINKEDIN))
            assertThat((awaitItem() as ReviewUiState.Ready).percent).isEqualTo(100)
        }
    }

    @Test
    fun stateRecomputesWhenTheProfileChanges() = runTest {
        store.sendProfile(freshReviewProfile())
        val viewModel = viewModel()

        viewModel.uiState.test {
            val first = expectMostRecentItem() as ReviewUiState.Ready
            assertThat(first.gap).isNotNull()
            assertThat(first.percent).isEqualTo(90)

            store.sendProfile(fixedReviewProfile())

            val second = awaitItem() as ReviewUiState.Ready
            assertThat(second.gap).isNull()
            assertThat(second.percent).isEqualTo(96)
        }
    }

    @Test
    fun openingReviewWritesNothingAndConfirmsNothing() = runTest {
        val original = freshReviewProfile()
        store.sendProfile(original)
        val repository = RecordingProfileRepository(store)

        viewModel(repository).ready()

        assertThat(repository.saves).isEqualTo(0)
        assertThat(saved()).isEqualTo(original)
        assertThat(saved().reviewedAt).isNull()
    }

    @Test
    fun continueRefusedWithRequiredGapSavesNothingAndToasts() = runTest {
        val original = freshReviewProfile()
        store.sendProfile(original)
        val repository = RecordingProfileRepository(store)
        val viewModel = viewModel(repository)

        viewModel.events.test {
            viewModel.onContinue(viewModel.ready().profile)

            assertThat(awaitItem()).isEqualTo(ReviewEvent.MissingEndDate)
        }

        assertThat(repository.saves).isEqualTo(0)
        assertThat(saved()).isEqualTo(original)
    }

    @Test
    fun continueWithoutGapSetsReviewedAtAndEmitsContinue() = runTest {
        store.sendProfile(fixedReviewProfile())
        val viewModel = viewModel()

        viewModel.events.test {
            viewModel.onContinue(viewModel.ready().profile)

            assertThat(awaitItem()).isEqualTo(ReviewEvent.Continue)
        }

        assertThat(saved().reviewedAt).isEqualTo(clock.instant)
        assertThat(saved().entries.filter { it.category == EntryCategory.EXPERIENCE }.all { it.isConfirmed }).isTrue()
    }

    @Test
    fun saveFailureEmitsSaveFailedAndContinueWorksAgain() = runTest {
        store.sendProfile(fixedReviewProfile())
        val failing = object : ProfileRepository by store {
            var failNext = true

            override suspend fun saveProfileIfUnchanged(expected: CandidateProfile, replacement: CandidateProfile): Boolean {
                if (failNext) {
                    failNext = false
                    error("disk full")
                }
                return store.saveProfileIfUnchanged(expected, replacement)
            }
        }
        val viewModel = viewModel(failing)

        viewModel.events.test {
            viewModel.onContinue(viewModel.ready().profile)
            assertThat(awaitItem()).isEqualTo(ReviewEvent.SaveFailed)

            viewModel.onContinue(viewModel.ready().profile)
            assertThat(awaitItem()).isEqualTo(ReviewEvent.Continue)
        }
    }

    @Test
    fun doubleTapContinuesOnce() = runTest {
        store.sendProfile(fixedReviewProfile())
        val gate = CompletableDeferred<Unit>()
        val repository = RecordingProfileRepository(store, gate)
        val viewModel = viewModel(repository)

        viewModel.events.test {
            viewModel.onContinue(viewModel.ready().profile)
            viewModel.onContinue(viewModel.ready().profile)
            gate.complete(Unit)

            assertThat(awaitItem()).isEqualTo(ReviewEvent.Continue)
            expectNoEvents()
        }

        assertThat(repository.saves).isEqualTo(1)
    }

    @Test
    fun continueWorksAgainAfterComingBack() = runTest {
        store.sendProfile(fixedReviewProfile())
        val viewModel = viewModel()

        viewModel.events.test {
            viewModel.onContinue(viewModel.ready().profile)
            viewModel.onContinue(viewModel.ready().profile)

            assertThat(awaitItem()).isEqualTo(ReviewEvent.Continue)
            assertThat(awaitItem()).isEqualTo(ReviewEvent.Continue)
        }
    }

    @Test
    fun emptyProfileEmitsNothingToReview() = runTest {
        val viewModel = viewModel()

        viewModel.events.test {
            assertThat(awaitItem()).isEqualTo(ReviewEvent.NothingToReview)
        }
    }

    @Test
    fun continueOnAnEmptyProfileEmitsNothingToReviewAndSavesNothing() = runTest {
        val repository = RecordingProfileRepository(store)
        val viewModel = viewModel(repository)

        viewModel.events.test {
            assertThat(awaitItem()).isEqualTo(ReviewEvent.NothingToReview)
            viewModel.onContinue(null)
            assertThat(awaitItem()).isEqualTo(ReviewEvent.NothingToReview)
        }

        assertThat(repository.saves).isEqualTo(0)
    }

    @Test
    fun staleDisplayedProfileCannotConfirmLaterEdits() = runTest {
        store.sendProfile(fixedReviewProfile())
        val viewModel = viewModel()
        val displayed = viewModel.ready().profile
        val edited = fixedReviewProfile().copy(linkedinUrl = SAMPLE_LINKEDIN)
        store.sendProfile(edited)

        viewModel.events.test {
            viewModel.onContinue(displayed)
            expectNoEvents()
            assertThat(saved()).isEqualTo(edited)
            viewModel.onContinue(viewModel.ready().profile)
            assertThat(awaitItem()).isEqualTo(ReviewEvent.Continue)
        }
    }
}
