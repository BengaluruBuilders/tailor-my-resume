package com.tailormyresume.app.billing

import com.google.common.truth.Truth.assertThat
import com.tailormyresume.core.domain.PurchaseFailureReason
import com.tailormyresume.core.domain.PurchaseResult
import com.tailormyresume.core.network.TailorMyResumeApiConfig
import com.tailormyresume.core.network.tailormyresumeApi
import com.tailormyresume.core.network.tailormyresumeJson
import com.tailormyresume.core.network.tailormyresumeOkHttpClient
import com.tailormyresume.core.testing.repository.TestCreditsRepository
import kotlinx.coroutines.test.runTest
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Test

class PurchaseSettleV2Test {
    private val server = MockWebServer().apply { start() }
    private val billing = FakePlayBilling().apply { products = threeProducts() }
    private val api = tailormyresumeApi(TailorMyResumeApiConfig(server.url("/").toString()), tailormyresumeOkHttpClient(FixedToken), tailormyresumeJson())
    private val gateway = RemotePaymentGateway(api, WalletSource(api), billing, FakeUid("uid-1"), idleScope(), TestCreditsRepository())

    @After
    fun tearDown() = server.shutdown()

    private fun reply(code: Int, body: String) = server.enqueue(MockResponse().setResponseCode(code).setBody(body))

    private fun playReturns(productId: String, state: PlayPurchaseState) {
        billing.purchaseResult = PlayPurchaseResult.Done(PlayPurchase(productId, "token-$productId", state))
    }

    @Test
    fun pendingAndTransientServerErrorsKeepTokenAndRepost() = runTest {
        playReturns("application_pack_15", PlayPurchaseState.PENDING)
        val playPending = gateway.purchase("application_pack_15") as PurchaseResult.Pending
        assertThat(playPending.entitlement.pendingPackIds).containsExactly("application_pack_15")
        assertThat(server.requestCount).isEqualTo(0)

        playReturns("application_pack_40", PlayPurchaseState.PURCHASED)
        reply(409, errorJson("PURCHASE_PENDING"))
        val serverPending = gateway.purchase("application_pack_40") as PurchaseResult.Pending
        assertThat(serverPending.entitlement.pendingPackIds).containsExactly("application_pack_15", "application_pack_40")
        assertThat(server.requestCount).isEqualTo(1)
        assertThat(gateway.repostCount()).isEqualTo(1)

        playReturns("application_pack_5", PlayPurchaseState.PURCHASED)
        repeat(3) { reply(502, errorJson("PLAY_UNAVAILABLE")) }
        val unavailable = gateway.purchase("application_pack_5") as PurchaseResult.Pending
        assertThat(unavailable.entitlement.pendingPackIds).contains("application_pack_5")
        assertThat(server.requestCount).isEqualTo(4)
        assertThat(gateway.repostCount()).isEqualTo(2)
        assertThat(unavailable.entitlement.totalCredits).isEqualTo(0)
    }

    @Test
    fun rejectionsFailWithoutGrantAndCancelIsSilent() = runTest {
        listOf(400 to "PURCHASE_INVALID", 403 to "FORBIDDEN").forEach { (status, code) ->
            playReturns("application_pack_5", PlayPurchaseState.PURCHASED)
            reply(status, errorJson(code))

            val result = gateway.purchase("application_pack_5") as PurchaseResult.Failed

            assertThat(result.reason).isEqualTo(PurchaseFailureReason.PaymentUnconfirmed)
            assertThat(result.entitlement.pendingPackIds).isEmpty()
            assertThat(result.entitlement.totalCredits).isEqualTo(0)
        }
        assertThat(server.requestCount).isEqualTo(2)
        assertThat(gateway.repostCount()).isEqualTo(0)

        billing.purchaseResult = PlayPurchaseResult.Failed
        val failed = gateway.purchase("application_pack_5") as PurchaseResult.Failed
        assertThat(failed.reason).isEqualTo(PurchaseFailureReason.PaymentUnavailable)

        billing.purchaseResult = PlayPurchaseResult.Cancelled
        assertThat(gateway.purchase("application_pack_5")).isEqualTo(PurchaseResult.Cancelled)
        assertThat(server.requestCount).isEqualTo(2)
    }
}
