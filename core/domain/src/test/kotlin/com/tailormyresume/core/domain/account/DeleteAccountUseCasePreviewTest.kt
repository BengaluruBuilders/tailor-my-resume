package com.tailormyresume.core.domain.account

import com.google.common.truth.Truth.assertThat
import com.tailormyresume.core.data.mock.NoMockLatency
import com.tailormyresume.core.data.repository.ApplicationRepository
import com.tailormyresume.core.data.repository.ProfileRepository
import com.tailormyresume.core.domain.PaymentGateway
import com.tailormyresume.core.domain.PurchaseEntitlement
import com.tailormyresume.core.domain.offline.OfflineServerAccountDeleter
import com.tailormyresume.core.model.ApplicationStatus
import com.tailormyresume.core.model.CandidateProfile
import com.tailormyresume.core.model.JobApplication
import com.tailormyresume.core.testing.gateway.TestPaymentGateway
import com.tailormyresume.core.testing.gateway.TestSignInGateway
import com.tailormyresume.core.testing.repository.TestCreditsRepository
import com.tailormyresume.core.testing.repository.TestExportHistoryRepository
import com.tailormyresume.core.testing.repository.TestResumeSettingsRepository
import com.tailormyresume.core.testing.repository.TestSessionRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Test

class DeleteAccountUseCasePreviewTest {

    private val applications = MutableStateFlow<List<JobApplication>>(emptyList())
    private val profile = MutableStateFlow<CandidateProfile?>(null)
    private val session = TestSessionRepository()
    private val exportHistory = TestExportHistoryRepository()

    @Test
    fun previewUsesCachedCredits() = runTest {
        var entitlementCalls = 0
        val delegate = TestPaymentGateway().withFreeCredits(4)
        val gateway = object : PaymentGateway by delegate {
            override suspend fun entitlement(): PurchaseEntitlement {
                entitlementCalls++
                return delegate.entitlement()
            }
        }

        val counts = useCase(gateway).preview()

        assertThat(counts.unusedCredits).isEqualTo(4)
        assertThat(entitlementCalls).isEqualTo(0)
    }

    @Test
    fun refreshedPreviewUsesTheFreshEntitlement() = runTest {
        val gateway = TestPaymentGateway().withFreeCredits(4)

        val counts = useCase(gateway).refreshedPreview()

        assertThat(counts.unusedCredits).isEqualTo(4)
    }

    @Test
    fun refreshedPreviewFallsBackToTheCachedCreditsWhenTheWalletFails() = runTest {
        val gateway = object : PaymentGateway by TestPaymentGateway().withFreeCredits(4) {
            override suspend fun entitlement(): PurchaseEntitlement = throw java.io.IOException("offline")

            override fun observeEntitlement(): Flow<PurchaseEntitlement> = flowOf(PurchaseEntitlement(0, 3, emptyList()))
        }

        val counts = useCase(gateway).refreshedPreview()

        assertThat(counts.unusedCredits).isEqualTo(3)
    }

    private fun useCase(gateway: PaymentGateway): DeleteAccountUseCase = DeleteAccountUseCase(
        exportHistoryRepository = exportHistory,
        sessionRepository = session,
        signInGateway = TestSignInGateway(session),
        serverAccountDeleter = OfflineServerAccountDeleter(),
        applicationRepository = EmptyApplicationRepository(applications),
        profileRepository = EmptyProfileRepository(profile),
        creditBalance = AccountCreditBalance(paymentGateway = gateway),
        latency = NoMockLatency,
        creditsRepository = TestCreditsRepository(),
        resumeSettingsRepository = TestResumeSettingsRepository(),
    )
}

private class EmptyApplicationRepository(
    private val applications: MutableStateFlow<List<JobApplication>>,
) : ApplicationRepository {

    override fun observeApplications(): Flow<List<JobApplication>> = applications

    override fun observeApplication(id: String): Flow<JobApplication?> = MutableStateFlow(null)

    override suspend fun upsertApplication(application: JobApplication) = Unit

    override suspend fun updateStatus(id: String, status: ApplicationStatus) = Unit

    override suspend fun deleteApplication(id: String) = Unit
}

private class EmptyProfileRepository(
    private val profile: MutableStateFlow<CandidateProfile?>,
) : ProfileRepository {

    override fun observeProfile(): Flow<CandidateProfile?> = profile

    override suspend fun saveProfile(profile: CandidateProfile) = Unit

    override suspend fun clearProfile() = Unit
}
