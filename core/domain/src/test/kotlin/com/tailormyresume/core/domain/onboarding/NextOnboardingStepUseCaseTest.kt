package com.tailormyresume.core.domain.onboarding

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import com.tailormyresume.core.domain.FakeProfileRepository
import com.tailormyresume.core.model.CandidateProfile
import com.tailormyresume.core.model.KeptJobDescription
import com.tailormyresume.core.model.SignInAccount
import com.tailormyresume.core.testing.data.canonicalCandidateProfile
import com.tailormyresume.core.testing.data.canonicalProfileWithoutEntries
import com.tailormyresume.core.testing.repository.TestSessionRepository
import kotlinx.coroutines.test.runTest
import org.junit.Test

class NextOnboardingStepUseCaseTest {

    private val session = TestSessionRepository()
    private val profile = FakeProfileRepository()
    private val useCase = NextOnboardingStepUseCase(session, profile)
    private val job = KeptJobDescription(text = "Analyst\nSQL", company = "Northwind GCC", role = "Analyst")

    private suspend fun signedIn() {
        session.saveAccount(SignInAccount.localAccount)
    }

    private fun unconfirmed(): CandidateProfile = canonicalCandidateProfile.copy(
        entries = canonicalCandidateProfile.entries.map { it.copy(isConfirmed = false) },
    )

    @Test
    fun aSignedOutCandidateGoesToSignIn() = runTest {
        assertThat(useCase()).isEqualTo(OnboardingStep.SignIn)
    }

    @Test
    fun aCandidateWithNoProfileGoesToImportResume() = runTest {
        signedIn()

        assertThat(useCase()).isEqualTo(OnboardingStep.ImportResume)
    }

    @Test
    fun aProfileWithNoEntriesGoesToImportResume() = runTest {
        signedIn()
        profile.saveProfile(canonicalProfileWithoutEntries)

        assertThat(useCase()).isEqualTo(OnboardingStep.ImportResume)
    }

    @Test
    fun aProfileWithNoConfirmedFactGoesToConfirmFacts() = runTest {
        signedIn()
        profile.saveProfile(unconfirmed())

        assertThat(useCase()).isEqualTo(OnboardingStep.ConfirmFacts)
    }

    @Test
    fun aKeptJobDescriptionGoesToGapAnalysisWithThatJob() = runTest {
        signedIn()
        profile.saveProfile(canonicalCandidateProfile)
        session.keepJobDescription(job)

        assertThat(useCase()).isEqualTo(OnboardingStep.GapAnalysis(job))
    }

    @Test
    fun withNoKeptJobDescriptionTheFirstRunGoesToPasteJobDescription() = runTest {
        signedIn()
        profile.saveProfile(canonicalCandidateProfile)

        assertThat(useCase()).isEqualTo(OnboardingStep.PasteJobDescription)
    }

    @Test
    fun withOnboardingCompleteAndNoKeptJobDescriptionTheNextStepIsApplications() = runTest {
        signedIn()
        profile.saveProfile(canonicalCandidateProfile)
        session.markOnboardingComplete()

        assertThat(useCase()).isEqualTo(OnboardingStep.Applications)
    }

    @Test
    fun signInComesBeforeEveryOtherStep() = runTest {
        profile.saveProfile(canonicalCandidateProfile)
        session.keepJobDescription(job)
        session.markOnboardingComplete()

        assertThat(useCase()).isEqualTo(OnboardingStep.SignIn)
    }

    @Test
    fun theObservedStepFollowsTheSession() = runTest {
        profile.saveProfile(canonicalCandidateProfile)

        useCase.observe().test {
            assertThat(awaitItem()).isEqualTo(OnboardingStep.SignIn)
            session.saveAccount(SignInAccount.localAccount)
            assertThat(awaitItem()).isEqualTo(OnboardingStep.PasteJobDescription)
        }
    }
}
