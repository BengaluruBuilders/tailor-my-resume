package com.tailormyresume.app.billing

import com.google.common.truth.Truth.assertThat
import com.tailormyresume.core.domain.PurchaseFailureReason
import com.tailormyresume.core.domain.PurchaseResult
import com.tailormyresume.core.network.TailorMyResumeApiConfig
import com.tailormyresume.core.network.tailormyresumeApi
import com.tailormyresume.core.network.tailormyresumeJson
import com.tailormyresume.core.network.tailormyresumeOkHttpClient
import com.tailormyresume.core.testing.repository.TestCreditsRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withContext
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Test

class PurchaseSettleTest {
    private val server = MockWebServer().apply { start() }
    private val billing = FakePlayBilling()
    private val api = tailormyresumeApi(TailorMyResumeApiConfig(server.url("/").toString()), tailormyresumeOkHttpClient(FixedToken), tailormyresumeJson())
    private val gateway = RemotePaymentGateway(api, WalletSource(api), billing, FakeUid("uid-1"), idleScope(), TestCreditsRepository())
    private val ownedToken = PlayPurchase("application_pack_5", "token-1", PlayPurchaseState.PURCHASED)

    @After
    fun tearDown() = server.shutdown()

    private fun reply(code: Int, body: String) = server.enqueue(MockResponse().setResponseCode(code).setBody(body))

    private fun playUnavailable(times: Int) = repeat(times) { reply(502, errorJson("PLAY_UNAVAILABLE")) }

    private suspend fun TestScope.realIoPause(millis: Long = REAL_IO_PAUSE_MILLIS) {
        runCurrent()
        withContext(Dispatchers.Default) { delay(millis) }
        runCurrent()
    }

    private suspend fun TestScope.advanceVirtualSeconds(seconds: Int) = repeat(seconds) {
        advanceTimeBy(1_000)
        realIoPause()
    }

    private fun played() {
        billing.purchaseResult = PlayPurchaseResult.Done(ownedToken)
        billing.owned = listOf(ownedToken)
    }

    @Test
    fun playUnavailableAfterPurchasedKeepsThePackPendingAfterBoundedRetries() = runTest {
        played()
        playUnavailable(times = 3)

        val result = gateway.purchase("application_pack_5") as PurchaseResult.Pending

        assertThat(result.entitlement.pendingPackIds).containsExactly("application_pack_5")
        assertThat(server.requestCount).isEqualTo(3)
    }

    @Test
    fun aServerErrorWithNoEnvelopeKeepsThePackPending() = runTest {
        played()
        repeat(3) { reply(500, "") }

        val result = gateway.purchase("application_pack_5")

        assertThat(result).isInstanceOf(PurchaseResult.Pending::class.java)
    }

    @Test
    fun anOfflineDeviceAfterPurchasedKeepsThePackPending() = runTest {
        played()
        server.shutdown()

        val result = gateway.purchase("application_pack_5") as PurchaseResult.Pending

        assertThat(result.entitlement.pendingPackIds).containsExactly("application_pack_5")
    }

    @Test
    fun aRetryThatTheServerRecordsCompletesTheWaitingPurchase() = runTest {
        played()
        playUnavailable(times = 1)
        reply(201, purchaseJson(walletJson(purchased = 5)))

        val result = gateway.purchase("application_pack_5") as PurchaseResult.Completed

        assertThat(result.entitlement.purchasedCredits).isEqualTo(5)
        assertThat(result.entitlement.pendingPackIds).isEmpty()
        assertThat(server.requestCount).isEqualTo(2)
    }

    @Test
    fun restoreRepostsTheSameTokenOfAHeldPurchaseAndClearsIt() = runTest {
        played()
        playUnavailable(times = 3)
        gateway.purchase("application_pack_5")
        repeat(server.requestCount) { server.takeRequest() }
        reply(201, purchaseJson(walletJson(purchased = 5)))
        reply(200, """{"wallet":${walletJson(purchased = 5)}}""")

        val entitlement = gateway.restorePurchases()

        assertThat(server.takeRequest().body.readUtf8()).contains("\"purchaseToken\":\"token-1\"")
        assertThat(entitlement.purchasedCredits).isEqualTo(5)
        assertThat(entitlement.pendingPackIds).isEmpty()
    }

    @Test
    fun anAlreadyOwnedPackIsPostedAgainAndCompletes() = runTest {
        billing.purchaseResult = PlayPurchaseResult.AlreadyOwned
        billing.owned = listOf(ownedToken)
        reply(201, purchaseJson(walletJson(purchased = 5)))

        val result = gateway.purchase("application_pack_5") as PurchaseResult.Completed

        assertThat(result.entitlement.purchasedCredits).isEqualTo(5)
        assertThat(server.takeRequest().body.readUtf8()).contains("\"purchaseToken\":\"token-1\"")
    }

    @Test
    fun anAlreadyOwnedPackTheServerCannotRecordStaysPending() = runTest {
        billing.purchaseResult = PlayPurchaseResult.AlreadyOwned
        billing.owned = listOf(ownedToken)
        playUnavailable(times = 3)

        val result = gateway.purchase("application_pack_5") as PurchaseResult.Pending

        assertThat(result.entitlement.pendingPackIds).containsExactly("application_pack_5")
    }

    @Test
    fun invalidAndForbiddenAnswersAfterPurchasedAreUnconfirmedNotNoCharge() = runTest {
        listOf(400 to "PURCHASE_INVALID", 403 to "FORBIDDEN").forEach { (status, code) ->
            played()
            reply(status, errorJson(code))

            val result = gateway.purchase("application_pack_5") as PurchaseResult.Failed

            assertThat(result.reason).isEqualTo(PurchaseFailureReason.PaymentUnconfirmed)
        }
    }

    @Test
    fun aHeldPurchaseIsPostedAgainByTheAppScopedJobAndCompletes() = runTest {
        val gateway = RemotePaymentGateway(api, WalletSource(api), billing, FakeUid("uid-1"), backgroundScope, TestCreditsRepository())
        played()
        playUnavailable(times = 3)
        reply(201, purchaseJson(walletJson(purchased = 5)))

        val result = gateway.purchase("application_pack_5") as PurchaseResult.Pending
        advanceVirtualSeconds(60)

        assertThat(result.entitlement.pendingPackIds).containsExactly("application_pack_5")
        assertThat(server.requestCount).isEqualTo(4)
        assertThat(gateway.observeEntitlement().first().pendingPackIds).isEmpty()
    }

    @Test
    fun theAppScopedJobGivesUpAfterABoundedNumberOfPostsAndKeepsThePackPending() = runTest {
        val gateway = RemotePaymentGateway(api, WalletSource(api), billing, FakeUid("uid-1"), backgroundScope, TestCreditsRepository())
        played()
        playUnavailable(times = 20)

        gateway.purchase("application_pack_5")
        advanceVirtualSeconds(120)

        assertThat(server.requestCount).isEqualTo(8)
        assertThat(gateway.observeEntitlement().first().pendingPackIds).containsExactly("application_pack_5")
    }

    @Test
    fun theAppScopedJobStopsOnARejectionAndTheResultIsNotPending() = runTest {
        val gateway = RemotePaymentGateway(api, WalletSource(api), billing, FakeUid("uid-1"), backgroundScope, TestCreditsRepository())
        played()
        playUnavailable(times = 3)
        reply(400, errorJson("PURCHASE_INVALID"))

        gateway.purchase("application_pack_5")
        advanceVirtualSeconds(120)

        assertThat(server.requestCount).isEqualTo(4)
        assertThat(gateway.observeEntitlement().first().pendingPackIds).isEmpty()
    }

    @Test
    fun cancellingTheCallerMidPurchaseStillHoldsThePackAndKeepsPosting() = runTest {
        val gateway = RemotePaymentGateway(api, WalletSource(api), billing, FakeUid("uid-1"), backgroundScope, TestCreditsRepository())
        played()
        playUnavailable(times = 1)
        reply(201, purchaseJson(walletJson(purchased = 5)))

        val caller = launch { gateway.purchase("application_pack_5") }
        runCurrent()
        caller.cancel()
        runCurrent()
        assertThat(gateway.observeEntitlement().first().pendingPackIds).containsExactly("application_pack_5")
        advanceVirtualSeconds(60)

        assertThat(server.requestCount).isEqualTo(2)
        assertThat(gateway.observeEntitlement().first().pendingPackIds).isEmpty()
    }

    @Test
    fun anAlreadyOwnedAnswerWithNoOwnedTokenIsUnconfirmedNotUnavailable() = runTest {
        billing.purchaseResult = PlayPurchaseResult.AlreadyOwned
        billing.owned = emptyList()

        val result = gateway.purchase("application_pack_5") as PurchaseResult.Failed

        assertThat(result.reason).isEqualTo(PurchaseFailureReason.PaymentUnconfirmed)
    }

    @Test
    fun aPlayFailureBeforePurchasedStaysFailedAndNothingIsPosted() = runTest {
        billing.purchaseResult = PlayPurchaseResult.Failed

        val result = gateway.purchase("application_pack_5") as PurchaseResult.Failed

        assertThat(result.reason).isEqualTo(PurchaseFailureReason.PaymentUnavailable)
        assertThat(server.requestCount).isEqualTo(0)
    }

    private companion object {
        const val REAL_IO_PAUSE_MILLIS = 15L
    }
}
