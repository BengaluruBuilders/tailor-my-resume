package com.tailormyresume.app.billing

import com.google.common.truth.Truth.assertThat
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

class RemotePaymentGatewayPacksTest {
    private val server = MockWebServer().apply { start() }
    private val billing = FakePlayBilling()
    private val api = tailormyresumeApi(TailorMyResumeApiConfig(server.url("/").toString()), tailormyresumeOkHttpClient(FixedToken), tailormyresumeJson())
    private val gateway = RemotePaymentGateway(api, WalletSource(api), billing, FakeUid("uid-1"), idleScope(), TestCreditsRepository())

    @After
    fun tearDown() = server.shutdown()

    private fun listedByServer() = server.enqueue(MockResponse().setResponseCode(200).setBody(packsBody()))

    @Test
    fun threePacksOnlyFivePackAndNonePack() = runTest {
        billing.products = threeProducts()
        listedByServer()
        val all = gateway.packs()
        assertThat(all.map { it.id }).containsExactly("application_pack_5", "application_pack_15", "application_pack_40").inOrder()
        assertThat(all.map { it.credits }).containsExactly(5, 15, 40).inOrder()
        assertThat(all.map { it.priceInPaise }).containsExactly(5_000L, 15_000L, 40_000L).inOrder()

        billing.products = threeProducts().filterKeys { it == "application_pack_5" }
        listedByServer()
        assertThat(gateway.packs().map { it.id }).containsExactly("application_pack_5")

        billing.products = emptyMap()
        listedByServer()
        assertThat(gateway.packs()).isEmpty()
    }
}
