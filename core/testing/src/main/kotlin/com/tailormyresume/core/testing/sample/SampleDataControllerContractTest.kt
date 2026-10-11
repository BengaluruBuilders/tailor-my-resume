package com.tailormyresume.core.testing.sample

import com.google.common.truth.Truth.assertThat
import com.tailormyresume.core.data.repository.ApplicationRepository
import com.tailormyresume.core.data.repository.ProfileRepository
import com.tailormyresume.core.data.repository.SessionRepository
import com.tailormyresume.core.domain.PaymentGateway
import com.tailormyresume.core.domain.sample.SampleDataController
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Test

abstract class SampleDataControllerContractTest {

    protected class Fixture(
        val controller: SampleDataController,
        val sessionRepository: SessionRepository,
        val profileRepository: ProfileRepository,
        val applicationRepository: ApplicationRepository,
        val paymentGateway: PaymentGateway,
    )

    protected abstract fun createFixture(): Fixture

    @Test
    fun loadSignsInAnAccountAndCompletesOnboarding() = runTest {
        val fixture = createFixture()

        fixture.controller.load()

        assertThat(fixture.sessionRepository.observeAccount().first()).isNotNull()
        assertThat(fixture.sessionRepository.observeOnboardingComplete().first()).isTrue()
    }

    @Test
    fun loadFillsTheProfileApplicationsAndCredits() = runTest {
        val fixture = createFixture()

        fixture.controller.load()

        assertThat(fixture.profileRepository.observeProfile().first()?.entries).isNotEmpty()
        assertThat(fixture.applicationRepository.observeApplications().first()).hasSize(4)
        assertThat(fixture.paymentGateway.entitlement().totalCredits).isEqualTo(5)
        assertThat(fixture.paymentGateway.purchaseHistory()).hasSize(1)
    }

    @Test
    fun keepingTheSampleJobDescriptionStoresItAndClearingRemovesIt() = runTest {
        val fixture = createFixture()

        fixture.controller.keepSampleJobDescription()
        val kept = fixture.sessionRepository.observeKeptJobDescription().first()

        assertThat(kept?.company).isEqualTo("Northwind GCC")
        assertThat(kept?.text).contains("Strong SQL")

        fixture.controller.clearSampleJobDescription()

        assertThat(fixture.sessionRepository.observeKeptJobDescription().first()).isNull()
    }

    @Test
    fun loadingTwiceGivesTheSameDataset() = runTest {
        val fixture = createFixture()
        fixture.controller.load()

        fixture.controller.load()

        assertThat(fixture.applicationRepository.observeApplications().first()).hasSize(4)
        assertThat(fixture.paymentGateway.entitlement().totalCredits).isEqualTo(5)
        assertThat(fixture.paymentGateway.purchaseHistory()).hasSize(1)
    }

    @Test
    fun resetReturnsTheAppToAFreshInstall() = runTest {
        val fixture = createFixture()
        fixture.controller.load()

        fixture.controller.reset()

        assertThat(fixture.sessionRepository.observeAccount().first()).isNull()
        assertThat(fixture.sessionRepository.observeOnboardingComplete().first()).isFalse()
        assertThat(fixture.profileRepository.observeProfile().first()).isNull()
        assertThat(fixture.applicationRepository.observeApplications().first()).isEmpty()
        assertThat(fixture.paymentGateway.purchaseHistory()).isEmpty()
        assertThat(fixture.paymentGateway.entitlement().totalCredits).isEqualTo(1)
    }
}
