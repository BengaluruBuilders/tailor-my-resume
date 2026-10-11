package com.tailormyresume.core.domain.account

import com.google.common.truth.Truth.assertThat
import com.tailormyresume.core.data.mock.MockLatency
import com.tailormyresume.core.data.mock.MockOperation
import com.tailormyresume.core.data.mock.NoMockLatency
import com.tailormyresume.core.data.repository.ApplicationRepository
import com.tailormyresume.core.data.repository.ProfileRepository
import com.tailormyresume.core.domain.ApplicationPack
import com.tailormyresume.core.domain.CreditSpend
import com.tailormyresume.core.domain.PaymentGateway
import com.tailormyresume.core.domain.PurchaseEntitlement
import com.tailormyresume.core.domain.PurchaseFailureReason
import com.tailormyresume.core.domain.PurchaseResult
import com.tailormyresume.core.domain.offline.OfflineServerAccountDeleter
import com.tailormyresume.core.model.ApplicationStatus
import com.tailormyresume.core.model.CandidateProfile
import com.tailormyresume.core.model.CreditKind
import com.tailormyresume.core.model.ExportFormat
import com.tailormyresume.core.model.ExportRecord
import com.tailormyresume.core.model.JobApplication
import com.tailormyresume.core.model.SignInAccount
import com.tailormyresume.core.testing.data.canonicalCandidateProfile
import com.tailormyresume.core.testing.data.sampleApplication
import com.tailormyresume.core.testing.gateway.TestPaymentGateway
import com.tailormyresume.core.testing.gateway.TestSignInGateway
import com.tailormyresume.core.testing.mock.TestMockStateStore
import com.tailormyresume.core.testing.repository.TestExportHistoryRepository
import com.tailormyresume.core.testing.repository.TestSessionRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.test.runTest
import org.junit.Test

class DeleteAccountUseCaseTest {

    private val applications = MutableStateFlow<List<JobApplication>>(emptyList())
    private val profile = MutableStateFlow<CandidateProfile?>(null)
    private val calls = mutableListOf<String>()
    private var failingApplicationIds: Set<String> = emptySet()
    private var failingProfileClear = false
    private val session = TestSessionRepository()
    private val exportHistory = TestExportHistoryRepository()
    private var serverAccountDeleter: ServerAccountDeleter = OfflineServerAccountDeleter()

    @Test
    fun previewCountsTheRealProfileApplicationsAndCredits() = runTest {
        applications.value = fourApplications()
        profile.value = canonicalCandidateProfile
        val useCase = useCase(gateway = gatewayWith(credits = 4))

        val counts = useCase.preview()

        assertThat(counts.profileFacts).isEqualTo(27)
        assertThat(counts.applications).isEqualTo(4)
        assertThat(counts.unusedCredits).isEqualTo(4)
    }

    @Test
    fun aFailedServerDeleteKeepsEveryLocalRecord() = runTest {
        applications.value = fourApplications()
        profile.value = canonicalCandidateProfile
        serverAccountDeleter = ServerAccountDeleter { Result.failure(IllegalStateException("offline")) }
        val useCase = useCase(gateway = gatewayWith(credits = 4))

        val result = useCase()

        assertThat(result).isEqualTo(AccountDeletionResult.Failed(dataIntact = true))
        assertThat(applications.value).hasSize(4)
        assertThat(profile.value).isEqualTo(canonicalCandidateProfile)
        assertThat(calls).isEmpty()
    }

    @Test
    fun previewWithAnEmptyAccountCountsZero() = runTest {
        val useCase = useCase(gateway = gatewayWith(credits = 0))

        val counts = useCase.preview()

        assertThat(counts.profileFacts).isEqualTo(0)
        assertThat(counts.applications).isEqualTo(0)
        assertThat(counts.unusedCredits).isEqualTo(0)
    }

    @Test
    fun deletingRemovesEveryApplicationTheProfileAndTheCredits() = runTest {
        applications.value = fourApplications()
        profile.value = canonicalCandidateProfile
        val gateway = gatewayWith(credits = 4)
        val useCase = useCase(gateway = gateway)

        val result = useCase()

        assertThat(result).isEqualTo(
            AccountDeletionResult.Deleted(
                AccountDeletionCounts(
                    profileFacts = 27,
                    applications = 4,
                    unusedCredits = 4,
                ),
            ),
        )
        assertThat(applications.value).isEmpty()
        assertThat(profile.value).isNull()
        assertThat(gateway.entitlement().totalCredits).isEqualTo(0)
    }

    @Test
    fun theNextSignInAfterDeletionStartsWithTheFreeCredit() = runTest {
        val store = TestMockStateStore()
        val payment = TestPaymentGateway(store = store)
        val signIn = TestSignInGateway(session, store)
        payment.purchase(ApplicationPack.APPLICATION_PACK_FIVE)
        val useCase = DeleteAccountUseCase(
            exportHistoryRepository = exportHistory,
            sessionRepository = session,
            signInGateway = signIn,
            serverAccountDeleter = OfflineServerAccountDeleter(),
            applicationRepository = RecordingApplicationRepository(applications, calls, { failingApplicationIds }),
            profileRepository = RecordingProfileRepository(profile, calls, { failingProfileClear }),
            creditBalance = AccountCreditBalance(paymentGateway = payment),
            latency = NoMockLatency,
        )

        useCase()
        signIn.signIn()

        assertThat(payment.entitlement().freeCredits).isEqualTo(1)
        assertThat(payment.entitlement().purchasedCredits).isEqualTo(0)
    }

    @Test
    fun deletingReportsTheNamedStepsInOrderBehindTheCounts() = runTest {
        applications.value = fourApplications()
        profile.value = canonicalCandidateProfile
        val seen = mutableListOf<AccountDeletionStep>()
        val useCase = useCase(gateway = gatewayWith(credits = 2))

        useCase(onStep = { step -> seen += step })

        assertThat(seen).containsExactly(
            AccountDeletionStep.DELETING_PROFILE_FACTS,
            AccountDeletionStep.DELETING_APPLICATIONS,
            AccountDeletionStep.CLOSING_ACCOUNT,
        ).inOrder()
    }

    @Test
    fun eachStepWaitsForTheLatencyPolicyBeforeItsWork() = runTest {
        applications.value = fourApplications()
        profile.value = canonicalCandidateProfile
        val awaited = mutableListOf<MockOperation>()
        val latency = object : MockLatency {
            override suspend fun await(operation: MockOperation) {
                awaited += operation
                assertThat(session.observeAccount().first()).isEqualTo(SignInAccount.localAccount)
            }
        }
        session.saveAccount(SignInAccount.localAccount)

        useCase(gateway = gatewayWith(credits = 1), latency = latency)()

        assertThat(awaited).containsExactly(
            MockOperation.DELETE_ACCOUNT_STEP,
            MockOperation.DELETE_ACCOUNT_STEP,
            MockOperation.DELETE_ACCOUNT_STEP,
        )
        assertThat(session.observeAccount().first()).isNull()
    }

    @Test
    fun theRepositoriesAreCalledInTheOrderTheStepsPromise() = runTest {
        applications.value = fourApplications()
        profile.value = canonicalCandidateProfile
        val useCase = useCase(gateway = gatewayWith(credits = 1))

        useCase()

        assertThat(calls).containsExactly(
            "clearProfile",
            "delete:application-1",
            "delete:application-2",
            "delete:application-3",
            "delete:application-4",
        ).inOrder()
    }

    @Test
    fun aFailureWhileDeletingAnApplicationRestoresTheClearedProfileAndEveryApplication() = runTest {
        applications.value = fourApplications()
        profile.value = canonicalCandidateProfile
        failingApplicationIds = setOf("application-3")
        val useCase = useCase(gateway = gatewayWith(credits = 4))

        val result = useCase()

        assertThat(result).isEqualTo(AccountDeletionResult.Failed(dataIntact = true))
        assertThat(applications.value.map { it.id }).containsExactly(
            "application-1",
            "application-2",
            "application-3",
            "application-4",
        )
        assertThat(profile.value).isEqualTo(canonicalCandidateProfile)
        assertThat(calls.first()).isEqualTo("clearProfile")
        assertThat(calls).contains("saveProfile")
    }

    @Test
    fun aFailureWhileClearingTheProfileLeavesEveryApplicationInPlace() = runTest {
        applications.value = fourApplications()
        profile.value = canonicalCandidateProfile
        failingProfileClear = true
        val useCase = useCase(gateway = gatewayWith(credits = 4))

        val result = useCase()

        assertThat(result).isEqualTo(AccountDeletionResult.Failed(dataIntact = true))
        assertThat(calls.filter { it.startsWith("delete:") }).isEmpty()
        assertThat(applications.value.map { it.id }).containsExactly(
            "application-1",
            "application-2",
            "application-3",
            "application-4",
        )
        assertThat(profile.value).isEqualTo(canonicalCandidateProfile)
    }

    @Test
    fun aFailureWhileClosingTheAccountNeverClaimsTheDataIsIntact() = runTest {
        applications.value = fourApplications()
        profile.value = canonicalCandidateProfile
        val useCase = useCase(gateway = ThrowingPaymentGateway())

        val result = useCase()

        assertThat(result).isEqualTo(AccountDeletionResult.Failed(dataIntact = false))
    }

    @Test
    fun aLocalFailureAfterTheServerDeletionNeverClaimsTheDataIsIntact() = runTest {
        applications.value = fourApplications()
        profile.value = canonicalCandidateProfile
        failingProfileClear = true
        serverAccountDeleter = object : ServerAccountDeleter {
            override val deletesRemoteData = true

            override suspend fun delete() = Result.success(Unit)
        }

        val result = useCase(gateway = gatewayWith(credits = 4))()

        assertThat(result).isEqualTo(AccountDeletionResult.Failed(dataIntact = false))
    }

    @Test
    fun anUnreachableWalletFallsBackToTheCachedCreditsInsteadOfCrashing() = runTest {
        val gateway = object : PaymentGateway by gatewayWith(credits = 4) {
            override suspend fun entitlement(): PurchaseEntitlement = throw java.io.IOException("offline")

            override fun observeEntitlement(): Flow<PurchaseEntitlement> = flowOf(PurchaseEntitlement(0, 3, emptyList()))
        }

        val counts = useCase(gateway = gateway).preview()

        assertThat(counts.unusedCredits).isEqualTo(3)
    }

    @Test
    fun cancellationIsNotSwallowedAsAFailedDeletion() = runTest {
        applications.value = fourApplications()
        profile.value = canonicalCandidateProfile
        val useCase = useCase(gateway = gatewayWith(credits = 4))

        val thrown = runCatching {
            useCase(onStep = { step ->
                if (step == AccountDeletionStep.CLOSING_ACCOUNT) throw CancellationException("stop")
            })
        }.exceptionOrNull()

        assertThat(thrown).isInstanceOf(CancellationException::class.java)
    }

    @Test
    fun deletingSignsOutAndClearsSessionAndExportHistory() = runTest {
        applications.value = fourApplications()
        profile.value = canonicalCandidateProfile
        session.saveAccount(SignInAccount.localAccount)
        session.markOnboardingComplete()
        exportHistory.record(exportRecord())
        val useCase = useCase(gateway = gatewayWith(credits = 2))

        useCase()

        assertThat(session.observeAccount().first()).isNull()
        assertThat(session.observeOnboardingComplete().first()).isFalse()
        assertThat(exportHistory.observeExports().first()).isEmpty()
    }

    @Test
    fun aFailureBeforeTheAccountClosesKeepsTheSessionAndRestoresTheExportHistory() = runTest {
        applications.value = fourApplications()
        profile.value = canonicalCandidateProfile
        failingProfileClear = true
        session.saveAccount(SignInAccount.localAccount)
        exportHistory.record(exportRecord())
        val useCase = useCase(gateway = gatewayWith(credits = 2))

        val result = useCase()

        assertThat(result).isEqualTo(AccountDeletionResult.Failed(dataIntact = true))
        assertThat(session.observeAccount().first()).isEqualTo(SignInAccount.localAccount)
        assertThat(exportHistory.observeExports().first()).containsExactly(exportRecord())
    }

    @Test
    fun aFailureWhileDeletingAnApplicationRestoresTheExportHistoryExactly() = runTest {
        applications.value = fourApplications()
        profile.value = canonicalCandidateProfile
        failingApplicationIds = setOf("application-3")
        exportHistory.record(exportRecord())
        val useCase = useCase(gateway = gatewayWith(credits = 4))

        val result = useCase()

        assertThat(result).isEqualTo(AccountDeletionResult.Failed(dataIntact = true))
        assertThat(exportHistory.observeExports().first()).containsExactly(exportRecord())
    }

    private fun exportRecord() = ExportRecord(
        applicationId = "application-1",
        format = ExportFormat.PDF,
        fileName = "resume.pdf",
        exportedAt = kotlin.time.Instant.fromEpochSeconds(1_700_000_100),
        creditKind = CreditKind.FREE,
    )

    private fun useCase(gateway: PaymentGateway, latency: MockLatency = NoMockLatency): DeleteAccountUseCase = DeleteAccountUseCase(
        exportHistoryRepository = exportHistory,
        sessionRepository = session,
        signInGateway = TestSignInGateway(session),
        serverAccountDeleter = serverAccountDeleter,
        applicationRepository = RecordingApplicationRepository(
            applications = applications,
            calls = calls,
            failingIds = { failingApplicationIds },
        ),
        profileRepository = RecordingProfileRepository(
            profile = profile,
            calls = calls,
            shouldFailOnClear = { failingProfileClear },
        ),
        creditBalance = AccountCreditBalance(paymentGateway = gateway),
        latency = latency,
    )

    private fun fourApplications(): List<JobApplication> = listOf("1", "2", "3", "4").map { index ->
        sampleApplication.copy(id = "application-$index", status = ApplicationStatus.SAVED)
    }

    private fun gatewayWith(credits: Int): PaymentGateway = BalancePaymentGateway(
        PurchaseEntitlement(
            freeCredits = credits,
            purchasedCredits = 0,
            pendingPackIds = emptyList(),
        ),
    )
}

private class RecordingApplicationRepository(
    private val applications: MutableStateFlow<List<JobApplication>>,
    private val calls: MutableList<String>,
    private val failingIds: () -> Set<String>,
) : ApplicationRepository {

    override fun observeApplications(): Flow<List<JobApplication>> = applications

    override fun observeApplication(id: String): Flow<JobApplication?> = MutableStateFlow(null)

    override suspend fun upsertApplication(application: JobApplication) {
        calls += "upsert:${application.id}"
        applications.update { current ->
            if (current.any { it.id == application.id }) {
                current.map { if (it.id == application.id) application else it }
            } else {
                current + application
            }
        }
    }

    override suspend fun updateStatus(id: String, status: ApplicationStatus) = Unit

    override suspend fun updateNotes(id: String, notes: String) = Unit

    override suspend fun deleteApplication(id: String) {
        if (id in failingIds()) throw IllegalStateException("delete failed for $id")
        calls += "delete:$id"
        applications.update { current -> current.filterNot { it.id == id } }
    }
}

private class RecordingProfileRepository(
    private val profile: MutableStateFlow<CandidateProfile?>,
    private val calls: MutableList<String>,
    private val shouldFailOnClear: () -> Boolean,
) : ProfileRepository {

    override fun observeProfile(): Flow<CandidateProfile?> = profile

    override suspend fun saveProfile(profile: CandidateProfile) {
        calls += "saveProfile"
        this.profile.value = profile
    }

    override suspend fun clearProfile() {
        if (shouldFailOnClear()) throw IllegalStateException("clear failed")
        calls += "clearProfile"
        profile.value = null
    }
}

private class BalancePaymentGateway(
    entitlement: PurchaseEntitlement,
) : PaymentGateway {

    private var current: PurchaseEntitlement = entitlement

    override suspend fun packs(): List<ApplicationPack> = emptyList()

    override suspend fun purchase(packId: String): PurchaseResult = PurchaseResult.Failed(
        reason = PurchaseFailureReason.PurchaseUnavailable,
        entitlement = current,
    )

    override suspend fun entitlement(): PurchaseEntitlement = current

    override suspend fun restorePurchases(): PurchaseEntitlement = current

    override suspend fun unlock(applicationId: String): CreditSpend = if (current.totalCredits > 0) {
        current = current.copy(freeCredits = current.freeCredits - 1)
        CreditSpend.Spent(current)
    } else {
        CreditSpend.NoCreditLeft
    }

    override suspend fun clearCredits(): PurchaseEntitlement {
        current = PurchaseEntitlement(freeCredits = 0, purchasedCredits = 0, pendingPackIds = emptyList())
        return current
    }
}

private class ThrowingPaymentGateway : PaymentGateway {

    override suspend fun packs(): List<ApplicationPack> = throw IllegalStateException("no packs")

    override suspend fun purchase(packId: String): PurchaseResult = throw IllegalStateException("no purchase")

    override suspend fun entitlement(): PurchaseEntitlement = PurchaseEntitlement(
        freeCredits = 4,
        purchasedCredits = 0,
        pendingPackIds = emptyList(),
    )

    override suspend fun restorePurchases(): PurchaseEntitlement = entitlement()

    override suspend fun unlock(applicationId: String): CreditSpend = throw IllegalStateException("no credit service")

    override suspend fun clearCredits(): PurchaseEntitlement = throw IllegalStateException("no credit service")
}
