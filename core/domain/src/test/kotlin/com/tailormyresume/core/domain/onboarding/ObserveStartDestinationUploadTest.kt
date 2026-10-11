package com.tailormyresume.core.domain.onboarding

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import com.tailormyresume.core.model.SignInAccount
import com.tailormyresume.core.testing.data.PrototypeFixtures
import com.tailormyresume.core.testing.repository.TestProfileRepository
import com.tailormyresume.core.testing.repository.TestSessionRepository
import kotlinx.coroutines.test.runTest
import org.junit.Test

class ObserveStartDestinationUploadTest {
    private val session = TestSessionRepository()
    private val profiles = TestProfileRepository()
    private val useCase = ObserveStartDestinationUseCase(session, profiles)

    @Test
    fun uploadWhenSignedInAndNotReviewed() = runTest {
        session.saveAccount(SignInAccount.localAccount)
        profiles.sendProfile(PrototypeFixtures.fresh().profile)

        useCase().test {
            assertThat(awaitItem()).isEqualTo(StartDestination.Upload)
        }
    }

    @Test
    fun uploadWhenSignedInWithoutAProfile() = runTest {
        session.saveAccount(SignInAccount.localAccount)

        useCase().test {
            assertThat(awaitItem()).isEqualTo(StartDestination.Upload)
        }
    }

    @Test
    fun applicationsWhenReviewedAt() = runTest {
        session.saveAccount(SignInAccount.localAccount)
        profiles.sendProfile(PrototypeFixtures.returning().profile)

        useCase().test {
            assertThat(awaitItem()).isEqualTo(StartDestination.Applications)
        }
    }

    @Test
    fun applicationsForLegacyOnboardingComplete() = runTest {
        session.saveAccount(SignInAccount.localAccount)
        session.markOnboardingComplete()

        useCase().test {
            assertThat(awaitItem()).isEqualTo(StartDestination.Applications)
        }
    }

    @Test
    fun signInWithoutAnAccountEvenWhenReviewed() = runTest {
        profiles.sendProfile(PrototypeFixtures.returning().profile)

        useCase().test {
            assertThat(awaitItem()).isEqualTo(StartDestination.SignIn)
        }
    }
}
