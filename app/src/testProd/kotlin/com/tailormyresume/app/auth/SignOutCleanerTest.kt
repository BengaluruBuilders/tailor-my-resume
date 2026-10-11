package com.tailormyresume.app.auth

import com.google.common.truth.Truth.assertThat
import com.tailormyresume.app.ai.ANALYSIS_RESPONSE
import com.tailormyresume.app.ai.FakeBackend
import com.tailormyresume.app.ai.FixedIds
import com.tailormyresume.app.ai.PendingTailoringIds
import com.tailormyresume.app.ai.RemoteJobAnalysisSource
import com.tailormyresume.app.ai.candidate
import com.tailormyresume.app.billing.FakePlayBilling
import com.tailormyresume.app.billing.FakeUid
import com.tailormyresume.app.billing.RemotePaymentGateway
import com.tailormyresume.app.billing.WalletSource
import com.tailormyresume.app.billing.idleScope
import com.tailormyresume.app.billing.walletJson
import com.tailormyresume.core.data.repository.PendingReportQueue
import com.tailormyresume.core.model.ContentReport
import com.tailormyresume.core.model.ReportedItemKind
import com.tailormyresume.core.testing.mock.TestMockStateStore
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Test
import kotlin.time.Instant

class SignOutCleanerTest {
    private val backend = FakeBackend()
    private val store = TestMockStateStore()
    private val wallet = WalletSource(backend.api)
    private val payments = RemotePaymentGateway(backend.api, wallet, FakePlayBilling(), FakeUid("uid-1"), idleScope())
    private val analysis = RemoteJobAnalysisSource(backend.api, NoMatcher)
    private val reports = PendingReportQueue(store)
    private val cleaner = SignOutCleaner(payments, analysis, reports, store)

    @After
    fun tearDown() = backend.shutdown()

    @Test
    fun theNextUserInheritsNothingFromTheLastOne() = runBlocking<Unit> {
        backend.reply(200, """{"wallet":${walletJson(free = 0, purchased = 4)}}""")
        payments.entitlement()
        backend.reply(200, ANALYSIS_RESPONSE)
        analysis.analyse(candidate, "the raw job text")
        reports.add(ContentReport("app-1", ReportedItemKind.REQUIREMENT, "req-1", "text", Instant.fromEpochMilliseconds(1), null))
        val tailoringIds = PendingTailoringIds(store, FixedIds)
        val firstId = tailoringIds.idFor("app-1", null)

        cleaner.clear()

        assertThat(wallet.cached).isNull()
        assertThat(reports.pending()).isEmpty()
        assertThat(tailoringIds.idFor("app-1", null)).isNotEqualTo(firstId)
        backend.reply(200, ANALYSIS_RESPONSE)
        analysis.analyse(candidate, "the raw job text")
        assertThat(backend.server.requestCount).isEqualTo(3)
    }
}
