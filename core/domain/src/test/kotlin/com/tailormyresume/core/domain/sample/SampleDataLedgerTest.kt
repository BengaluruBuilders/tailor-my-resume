package com.tailormyresume.core.domain.sample

import com.google.common.truth.Truth.assertThat
import com.tailormyresume.core.data.mock.NoMockLatency
import com.tailormyresume.core.domain.AnalyzeJobUseCase
import com.tailormyresume.core.domain.TailorResumeUseCase
import com.tailormyresume.core.domain.offline.OfflineFabricationGuard
import com.tailormyresume.core.domain.offline.OfflineGapMatcher
import com.tailormyresume.core.domain.offline.OfflineJobAnalysisSource
import com.tailormyresume.core.domain.offline.OfflineJobDescriptionAnalyzer
import com.tailormyresume.core.domain.offline.OfflinePaymentGateway
import com.tailormyresume.core.domain.offline.OfflineResumeTailor
import com.tailormyresume.core.testing.mock.TestMockStateStore
import com.tailormyresume.core.testing.repository.TestApplicationRepository
import com.tailormyresume.core.testing.repository.TestCreditsRepository
import com.tailormyresume.core.testing.repository.TestExportHistoryRepository
import com.tailormyresume.core.testing.repository.TestProfileRepository
import com.tailormyresume.core.testing.repository.TestSessionRepository
import com.tailormyresume.core.testing.util.TestClock
import com.tailormyresume.core.testing.util.TestIdGenerator
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Test

class SampleDataLedgerTest {

    private val store = TestMockStateStore()
    private val clock = TestClock()
    private val payments = OfflinePaymentGateway(store, NoMockLatency, clock, TestIdGenerator("order"))
    private val credits = TestCreditsRepository()
    private val controller = OfflineSampleDataController(
        store = store,
        sessionRepository = TestSessionRepository(),
        profileRepository = TestProfileRepository(),
        applicationRepository = TestApplicationRepository(),
        exportHistoryRepository = TestExportHistoryRepository(),
        paymentGateway = payments,
        analyzeJob = AnalyzeJobUseCase(OfflineJobAnalysisSource(OfflineJobDescriptionAnalyzer(), OfflineGapMatcher())),
        tailorResume = TailorResumeUseCase(OfflineResumeTailor(), OfflineFabricationGuard()),
        clock = clock,
        credits = credits,
    )

    @Test
    fun sampleDataLedgerBalanceEqualsTheGatewayBalance() = runTest {
        controller.load()

        assertThat(credits.observeLedger().first().sumOf { it.amount }).isEqualTo(payments.entitlement().totalCredits)
    }

    @Test
    fun loadingTwiceDoesNotStackGrantsOrPurchases() = runTest {
        controller.load()
        controller.load()

        assertThat(credits.observeLedger().first().sumOf { it.amount }).isEqualTo(payments.entitlement().totalCredits)
    }
}
