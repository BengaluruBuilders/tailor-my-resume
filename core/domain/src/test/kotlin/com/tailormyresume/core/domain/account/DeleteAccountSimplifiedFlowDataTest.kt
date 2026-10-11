package com.tailormyresume.core.domain.account

import com.google.common.truth.Truth.assertThat
import com.tailormyresume.core.data.mock.NoMockLatency
import com.tailormyresume.core.domain.offline.OfflineServerAccountDeleter
import com.tailormyresume.core.model.CreditLedgerEntry
import com.tailormyresume.core.model.CreditLedgerKind
import com.tailormyresume.core.model.PageSize
import com.tailormyresume.core.model.ResumeSettings
import com.tailormyresume.core.testing.gateway.TestPaymentGateway
import com.tailormyresume.core.testing.gateway.TestSignInGateway
import com.tailormyresume.core.testing.repository.TestApplicationRepository
import com.tailormyresume.core.testing.repository.TestCreditsRepository
import com.tailormyresume.core.testing.repository.TestExportHistoryRepository
import com.tailormyresume.core.testing.repository.TestPendingAccountWipe
import com.tailormyresume.core.testing.repository.TestProfileRepository
import com.tailormyresume.core.testing.repository.TestResumeSettingsRepository
import com.tailormyresume.core.testing.repository.TestSessionRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Test
import kotlin.time.Instant

class DeleteAccountSimplifiedFlowDataTest {

    private val session = TestSessionRepository()
    private val credits = TestCreditsRepository()
    private val settings = TestResumeSettingsRepository()

    private val remoteDeleter = object : ServerAccountDeleter {
        override val deletesRemoteData = true
        override suspend fun delete(): Result<Unit> = Result.success(Unit)
        override suspend fun isClosed(): Result<Boolean> = Result.success(true)
    }

    private suspend fun seedLocalData() {
        credits.record(CreditLedgerEntry(CreditLedgerKind.PURCHASE, 5, "app-1", "application_pack_5", Instant.fromEpochSeconds(1)))
        settings.update { it.copy(pageSize = PageSize.LETTER, productUpdates = true) }
    }

    private fun useCase(deleter: ServerAccountDeleter) = DeleteAccountUseCase(
        applicationRepository = TestApplicationRepository(),
        profileRepository = TestProfileRepository(),
        exportHistoryRepository = TestExportHistoryRepository(),
        sessionRepository = session,
        signInGateway = TestSignInGateway(session),
        serverAccountDeleter = deleter,
        creditBalance = AccountCreditBalance(TestPaymentGateway()),
        latency = NoMockLatency,
        creditsRepository = credits,
        resumeSettingsRepository = settings,
        pendingWipe = TestPendingAccountWipe(),
    )

    @Test
    fun offlineDeleteClearsTheLedgerAndTheResumeSettings() = runTest {
        seedLocalData()

        val result = useCase(OfflineServerAccountDeleter())()

        assertThat(result).isInstanceOf(AccountDeletionResult.Deleted::class.java)
        assertThat(credits.observeLedger().first()).isEmpty()
        assertThat(settings.observeSettings().first()).isEqualTo(ResumeSettings())
    }

    @Test
    fun backendDeleteClearsTheLedgerAndTheResumeSettings() = runTest {
        seedLocalData()

        val result = useCase(remoteDeleter)()

        assertThat(result).isInstanceOf(AccountDeletionResult.Deleted::class.java)
        assertThat(credits.observeLedger().first()).isEmpty()
        assertThat(settings.observeSettings().first()).isEqualTo(ResumeSettings())
    }
}
