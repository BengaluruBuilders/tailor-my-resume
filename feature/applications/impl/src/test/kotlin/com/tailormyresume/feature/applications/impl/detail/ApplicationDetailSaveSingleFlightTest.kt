package com.tailormyresume.feature.applications.impl.detail

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import com.tailormyresume.core.data.repository.ApplicationRepository
import com.tailormyresume.core.domain.SetApplicationStatusUseCase
import com.tailormyresume.core.domain.account.AccountCreditBalance
import com.tailormyresume.core.model.ApplicationStatus
import com.tailormyresume.core.model.JobApplication
import com.tailormyresume.core.testing.gateway.TestPaymentGateway
import com.tailormyresume.core.testing.repository.TestApplicationRepository
import com.tailormyresume.core.testing.util.MainDispatcherRule
import com.tailormyresume.core.testing.util.TestClock
import com.tailormyresume.feature.applications.impl.prototypeApplication
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test

class ApplicationDetailSaveSingleFlightTest {

    @get:Rule
    val dispatcherRule = MainDispatcherRule(StandardTestDispatcher())

    private val backing = TestApplicationRepository()
    private var writes = 0
    private val applicationRepository = object : ApplicationRepository by backing {
        override suspend fun upsertApplication(application: JobApplication) {
            writes += 1
            delay(WRITE_LATENCY_MS)
            backing.upsertApplication(application)
        }
    }

    @Test
    fun doubleTapOnSaveStatusWritesOnceAndEmitsOneEvent() = runTest {
        backing.sendApplications(listOf(prototypeApplication("kb")))
        val viewModel = ApplicationDetailViewModel(
            applicationRepository = applicationRepository,
            creditBalance = AccountCreditBalance(TestPaymentGateway().withFreeCredits(1)),
            setApplicationStatus = SetApplicationStatusUseCase(applicationRepository, TestClock()),
            exportedPdfIntents = FakeExportedPdfIntents(),
            applicationId = "kb",
        )
        viewModel.state.first { it is ApplicationDetailUiState.Ready }
        viewModel.onChangeStatus()
        viewModel.onPickStatus(ApplicationStatus.OFFER)

        viewModel.events.test {
            viewModel.onSaveStatus()
            viewModel.onSaveStatus()
            advanceUntilIdle()

            assertThat(awaitItem()).isEqualTo(ApplicationDetailEvent.StatusSet(ApplicationStatus.OFFER))
            expectNoEvents()
            assertThat(writes).isEqualTo(1)
        }
    }
}

private const val WRITE_LATENCY_MS = 50L
