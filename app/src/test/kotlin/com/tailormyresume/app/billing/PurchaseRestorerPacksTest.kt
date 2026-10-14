package com.tailormyresume.app.billing

import com.google.common.truth.Truth.assertThat
import com.tailormyresume.core.network.TailorMyResumeApiConfig
import com.tailormyresume.core.network.tailormyresumeApi
import com.tailormyresume.core.network.tailormyresumeJson
import com.tailormyresume.core.network.tailormyresumeOkHttpClient
import com.tailormyresume.core.testing.repository.TestCreditsRepository
import kotlinx.coroutines.test.runTest
import okhttp3.mockwebserver.Dispatcher
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okhttp3.mockwebserver.RecordedRequest
import org.junit.After
import org.junit.Test
import java.util.concurrent.CopyOnWriteArrayList

class PurchaseRestorerPacksTest {
    private val posted = CopyOnWriteArrayList<String>()
    private val server = MockWebServer().apply {
        dispatcher = object : Dispatcher() {
            override fun dispatch(request: RecordedRequest): MockResponse {
                if (request.method == "GET") return MockResponse().setBody("""{"wallet":${walletV2(1, 20)}}""")
                val body = request.body.readUtf8()
                posted += body
                val (id, credits) = THREE_PACK_IDS.first { (id, _) -> "\"productId\":\"$id\"" in body }
                return if ("token-foreign" in body) {
                    MockResponse().setResponseCode(403).setBody(errorJson("FORBIDDEN"))
                } else {
                    MockResponse().setResponseCode(200).setBody(purchaseV2(id, credits))
                }
            }
        }
        start()
    }
    private val billing = FakePlayBilling().apply {
        owned = THREE_PACK_IDS.map { (id, _) -> PlayPurchase(id, "token-$id", PlayPurchaseState.PURCHASED) } +
            PlayPurchase("application_pack_5", "token-foreign", PlayPurchaseState.PURCHASED)
    }
    private val api = tailormyresumeApi(TailorMyResumeApiConfig(server.url("/").toString()), tailormyresumeOkHttpClient(FixedToken), tailormyresumeJson())
    private val gateway = RemotePaymentGateway(api, WalletSource(api), billing, FakeUid("uid-1"), idleScope(), TestCreditsRepository())

    @After
    fun tearDown() = server.shutdown()

    @Test
    fun restoresThreeTokensAndDropsForeignToken() = runTest {
        val entitlement = gateway.restorePurchases()

        assertThat(posted).hasSize(4)
        THREE_PACK_IDS.forEach { (id, _) -> assertThat(posted.count { "\"purchaseToken\":\"token-$id\"" in it }).isEqualTo(1) }
        assertThat(entitlement.totalCredits).isEqualTo(21)
        assertThat(entitlement.pendingPackIds).isEmpty()
        assertThat(gateway.repostCount()).isEqualTo(0)
    }
}
