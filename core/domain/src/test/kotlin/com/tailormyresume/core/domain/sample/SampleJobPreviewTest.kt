package com.tailormyresume.core.domain.sample

import com.google.common.truth.Truth.assertThat
import com.tailormyresume.core.data.mock.NoMockLatency
import com.tailormyresume.core.domain.AnalyzeJobUseCase
import com.tailormyresume.core.domain.FirebaseUidProvider
import com.tailormyresume.core.domain.TailorResumeUseCase
import com.tailormyresume.core.domain.offline.OfflineFabricationGuard
import com.tailormyresume.core.domain.offline.OfflineGapMatcher
import com.tailormyresume.core.domain.offline.OfflineJobAnalysisSource
import com.tailormyresume.core.domain.offline.OfflineJobDescriptionAnalyzer
import com.tailormyresume.core.domain.offline.OfflinePaymentGateway
import com.tailormyresume.core.domain.offline.OfflineResumeTailor
import com.tailormyresume.core.domain.onboarding.NextOnboardingStepUseCase
import com.tailormyresume.core.domain.onboarding.OnboardingStep
import com.tailormyresume.core.testing.mock.TestMockStateStore
import com.tailormyresume.core.testing.repository.TestApplicationRepository
import com.tailormyresume.core.testing.repository.TestExportHistoryRepository
import com.tailormyresume.core.testing.repository.TestProfileRepository
import com.tailormyresume.core.testing.repository.TestSessionRepository
import com.tailormyresume.core.testing.util.TestClock
import com.tailormyresume.core.testing.util.TestIdGenerator
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Test

class SampleJobPreviewTest {

    private val store = TestMockStateStore()
    private val session = TestSessionRepository()
    private val profiles = TestProfileRepository()
    private val applications = TestApplicationRepository()
    private val clock = TestClock()
    private val payments = OfflinePaymentGateway(store, NoMockLatency, clock, TestIdGenerator("order"))
    private var firebaseUid: String? = null
    private val controller = OfflineSampleDataController(
        store = store,
        sessionRepository = session,
        profileRepository = profiles,
        applicationRepository = applications,
        exportHistoryRepository = TestExportHistoryRepository(),
        paymentGateway = payments,
        analyzeJob = AnalyzeJobUseCase(OfflineJobAnalysisSource(OfflineJobDescriptionAnalyzer(), OfflineGapMatcher())),
        tailorResume = TailorResumeUseCase(OfflineResumeTailor(), OfflineFabricationGuard()),
        clock = clock,
        firebaseUid = FirebaseUidProvider { firebaseUid },
    )
    private val nextStep = NextOnboardingStepUseCase(session, profiles)

    @Test
    fun keepingTheSampleJobOnAnEmptyAppMakesGapAnalysisTheNextStep() = runTest {
        controller.keepSampleJobDescription()

        assertThat(nextStep()).isInstanceOf(OnboardingStep.GapAnalysis::class.java)
    }

    @Test
    fun keepingTheSampleJobSpendsNoCreditAndAddsNoApplication() = runTest {
        val creditsBefore = payments.entitlement().totalCredits

        controller.keepSampleJobDescription()

        assertThat(payments.entitlement().totalCredits).isEqualTo(creditsBefore)
        assertThat(payments.purchaseHistory()).isEmpty()
        assertThat(applications.observeApplications().first()).isEmpty()
    }

    @Test
    fun keepingTheSampleJobAfterLoadingSampleDataKeepsTheLoadedProfile() = runTest {
        controller.load()
        val profileBefore = profiles.observeProfile().first()

        controller.keepSampleJobDescription()

        assertThat(profiles.observeProfile().first()).isEqualTo(profileBefore)
        assertThat(applications.observeApplications().first()).hasSize(4)
    }

    @Test
    fun keepingTheSampleJobWithARealFirebaseUserWritesNoSampleAccountOrProfile() = runTest {
        firebaseUid = "real-uid"

        controller.keepSampleJobDescription()

        assertThat(session.observeAccount().first()).isNull()
        assertThat(profiles.observeProfile().first()).isNull()
    }
}
