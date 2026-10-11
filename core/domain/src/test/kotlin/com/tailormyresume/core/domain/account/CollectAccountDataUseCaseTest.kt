package com.tailormyresume.core.domain.account

import com.google.common.truth.Truth.assertThat
import com.tailormyresume.core.domain.ApplicationPack
import com.tailormyresume.core.domain.FakeApplicationRepository
import com.tailormyresume.core.domain.FakeProfileRepository
import com.tailormyresume.core.model.CreditLedgerEntry
import com.tailormyresume.core.model.CreditLedgerKind
import com.tailormyresume.core.model.PageSize
import com.tailormyresume.core.model.SignInAccount
import com.tailormyresume.core.testing.account.TestAccountDataExporter
import com.tailormyresume.core.testing.data.canonicalCandidateProfile
import com.tailormyresume.core.testing.data.sampleApplication
import com.tailormyresume.core.testing.gateway.TestPaymentGateway
import com.tailormyresume.core.testing.repository.TestCreditsRepository
import com.tailormyresume.core.testing.repository.TestExportHistoryRepository
import com.tailormyresume.core.testing.repository.TestResumeSettingsRepository
import com.tailormyresume.core.testing.repository.TestSessionRepository
import com.tailormyresume.core.testing.util.TestClock
import kotlinx.coroutines.test.runTest
import org.junit.Test

class CollectAccountDataUseCaseTest {

    private val session = TestSessionRepository()
    private val profiles = FakeProfileRepository(canonicalCandidateProfile)
    private val applications = FakeApplicationRepository()
    private val exports = TestExportHistoryRepository()
    private val payments = TestPaymentGateway()
    private val clock = TestClock()
    private val credits = TestCreditsRepository()
    private val resumeSettings = TestResumeSettingsRepository()
    private val collect = CollectAccountDataUseCase(
        session,
        profiles,
        applications,
        exports,
        payments,
        credits,
        resumeSettings,
        clock,
    )

    @Test
    fun theSnapshotHoldsEverythingTheAccountOwns() = runTest {
        session.saveAccount(SignInAccount.localAccount)
        applications.upsertApplication(sampleApplication)
        payments.purchase(ApplicationPack.APPLICATION_PACK_FIVE)

        val data = collect()

        assertThat(data.generatedAt).isEqualTo(clock.instant)
        assertThat(data.account).isEqualTo(SignInAccount.localAccount)
        assertThat(data.profile).isEqualTo(canonicalCandidateProfile)
        assertThat(data.applications).containsExactly(sampleApplication)
        assertThat(data.entitlement.purchasedCredits).isEqualTo(5)
        assertThat(data.purchases).hasSize(1)
    }

    @Test
    fun collectsLedgerAndSettings() = runTest {
        val grant = CreditLedgerEntry(CreditLedgerKind.FREE_GRANT, 1, null, null, clock.instant)
        credits.record(grant)
        resumeSettings.update { it.copy(pageSize = PageSize.LETTER) }

        val data = collect()

        assertThat(data.creditLedger).containsExactly(grant)
        assertThat(data.resumeSettings.pageSize).isEqualTo(PageSize.LETTER)
    }

    @Test
    fun exportingHandsTheSnapshotToTheExporter() = runTest {
        val exporter = TestAccountDataExporter()

        val archive = ExportAccountDataUseCase(collect, exporter)()

        assertThat(exporter.exported).hasSize(1)
        assertThat(archive.file.isFile).isTrue()
    }
}
