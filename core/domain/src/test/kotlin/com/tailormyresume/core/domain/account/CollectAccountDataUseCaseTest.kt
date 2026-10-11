package com.tailormyresume.core.domain.account

import com.google.common.truth.Truth.assertThat
import com.tailormyresume.core.domain.ApplicationPack
import com.tailormyresume.core.domain.FakeApplicationRepository
import com.tailormyresume.core.domain.FakeProfileRepository
import com.tailormyresume.core.model.SignInAccount
import com.tailormyresume.core.testing.account.TestAccountDataExporter
import com.tailormyresume.core.testing.data.canonicalCandidateProfile
import com.tailormyresume.core.testing.data.sampleApplication
import com.tailormyresume.core.testing.gateway.TestPaymentGateway
import com.tailormyresume.core.testing.repository.TestExportHistoryRepository
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
    private val collect = CollectAccountDataUseCase(session, profiles, applications, exports, payments, clock)

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
    fun exportingHandsTheSnapshotToTheExporter() = runTest {
        val exporter = TestAccountDataExporter()

        val archive = ExportAccountDataUseCase(collect, exporter)()

        assertThat(exporter.exported).hasSize(1)
        assertThat(archive.file.isFile).isTrue()
    }
}
