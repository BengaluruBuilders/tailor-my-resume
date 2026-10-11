package com.tailormyresume.app.ui

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import com.tailormyresume.core.testing.data.sampleApplication
import com.tailormyresume.core.testing.data.sampleProfile
import com.tailormyresume.core.testing.repository.TestApplicationRepository
import com.tailormyresume.core.testing.repository.TestProfileRepository
import com.tailormyresume.core.testing.util.MainDispatcherRule
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test
import kotlin.time.Instant

class BackTargetViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule(StandardTestDispatcher())

    private val applications = TestApplicationRepository()
    private val profiles = TestProfileRepository()

    private fun viewModel() = BackTargetViewModel(applications, profiles)

    @Test
    fun hasHome_isFalseUntilTheUserHasAnApplicationOrAReviewedProfile() = runTest {
        profiles.sendProfile(sampleProfile.copy(reviewedAt = null))

        viewModel().hasHome.test {
            assertThat(awaitItem()).isFalse()
            expectNoEvents()
        }
    }

    @Test
    fun hasHome_isTrueWhenTheUserHasAnApplication() = runTest {
        applications.sendApplications(listOf(sampleApplication))

        viewModel().hasHome.test {
            assertThat(awaitItem()).isFalse()
            assertThat(awaitItem()).isTrue()
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun hasHome_isTrueWhenTheProfileIsReviewed() = runTest {
        profiles.sendProfile(sampleProfile.copy(reviewedAt = Instant.parse("2026-10-02T09:30:00Z")))

        viewModel().hasHome.test {
            assertThat(awaitItem()).isFalse()
            assertThat(awaitItem()).isTrue()
            cancelAndIgnoreRemainingEvents()
        }
    }
}
