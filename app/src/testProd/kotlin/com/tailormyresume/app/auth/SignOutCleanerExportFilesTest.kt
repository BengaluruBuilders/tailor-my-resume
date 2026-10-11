package com.tailormyresume.app.auth

import com.google.common.truth.Truth.assertThat
import com.tailormyresume.app.ai.FakeBackend
import com.tailormyresume.app.ai.RemoteJobAnalysisSource
import com.tailormyresume.app.billing.FakePlayBilling
import com.tailormyresume.app.billing.FakeUid
import com.tailormyresume.app.billing.RemotePaymentGateway
import com.tailormyresume.app.billing.WalletSource
import com.tailormyresume.app.billing.idleScope
import com.tailormyresume.core.data.repository.PendingReportQueue
import com.tailormyresume.core.domain.account.ExportedFiles
import com.tailormyresume.core.testing.mock.TestMockStateStore
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Test

class SignOutCleanerExportFilesTest {
    private val backend = FakeBackend()
    private val store = TestMockStateStore()
    private var deleteAllCalls = 0

    private val cleaner = SignOutCleaner(
        RemotePaymentGateway(backend.api, WalletSource(backend.api), FakePlayBilling(), FakeUid("uid-1"), idleScope()),
        RemoteJobAnalysisSource(backend.api, NoMatcher),
        PendingReportQueue(store),
        store,
        ExportedFiles { deleteAllCalls++ },
    )

    @After
    fun tearDown() = backend.shutdown()

    @Test
    fun deletesExportFilesButKeepsProfileAndApplications() = runBlocking<Unit> {
        store.write("coverletter.app-1", "letter")
        store.write("prep.plan.app-1", "plan")

        cleaner.clear()

        assertThat(deleteAllCalls).isEqualTo(1)
        assertThat(store.read("coverletter.app-1")).isEqualTo("letter")
        assertThat(store.read("prep.plan.app-1")).isEqualTo("plan")
    }
}
