package com.tailormyresume.app.billing

import com.google.common.truth.Truth.assertThat
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

class RemotePaymentGatewayPurchaseTest {
    private val server = MockWebServer().apply { start() }
    private val billing = FakePlayBilling().apply { products = threeProducts() }
    private val api = tailormyresumeApi(TailorMyResumeApiConfig(server.url("/").toString()), tailormyresumeOkHttpClient(FixedToken), tailormyresumeJson())
    private val gateway = RemotePaymentGateway(api, WalletSource(api), billing, FakeUid("uid-1"), idleScope(), TestCreditsRepository())

    @After
    fun tearDown() = server.shutdown()

    @Test
    fun purchasesEachPackAndRefusesUnlistedPack() = runTest {
        server.enqueue(MockResponse().setResponseCode(200).setBody(packsBody()))
        gateway.packs()
        assertThat(server.takeRequest().path).isEqualTo("/v1/tailormyresume/packs")

        THREE_PACK_IDS.forEach { (id, credits) ->
            billing.purchaseResult = PlayPurchaseResult.Done(PlayPurchase(id, "token-$id", PlayPurchaseState.PURCHASED))
            server.enqueue(MockResponse().setResponseCode(201).setBody(purchaseV2(id, credits)))

            val result = gateway.purchase(id) as PurchaseResult.Completed

            assertThat(result.entitlement.purchasedCredits).isEqualTo(credits)
            assertThat(result.entitlement.totalCredits).isEqualTo(credits + 1)
            assertThat(billing.launchedWith).isEqualTo(id to obfuscatedAccountId("uid-1"))
            assertThat(obfuscatedAccountId("uid-1")).matches("[0-9a-f]{64}")
            val request = server.takeRequest()
            assertThat(request.path).isEqualTo("/v1/tailormyresume/purchases")
            val body = request.body.readUtf8()
            assertThat(body).contains("\"productId\":\"$id\"")
            assertThat(body).contains("\"purchaseToken\":\"token-$id\"")
        }

        billing.launchedWith = null
        val refused = gateway.purchase("application_pack_99")

        assertThat(refused).isInstanceOf(PurchaseResult.Failed::class.java)
        assertThat(billing.launchedWith).isNull()
        assertThat(server.requestCount).isEqualTo(4)
    }
}
