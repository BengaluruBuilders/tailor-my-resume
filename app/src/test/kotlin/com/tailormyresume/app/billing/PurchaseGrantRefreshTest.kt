package com.tailormyresume.app.billing

import com.google.common.truth.Truth.assertThat
import com.tailormyresume.core.data.repository.CreditsRepository
import com.tailormyresume.core.domain.PurchaseResult
import com.tailormyresume.core.network.TailorMyResumeApiConfig
import com.tailormyresume.core.network.tailormyresumeApi
import com.tailormyresume.core.network.tailormyresumeJson
import com.tailormyresume.core.network.tailormyresumeOkHttpClient
import com.tailormyresume.core.testing.repository.TestCreditsRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Test

private class RefreshCountingCredits : CreditsRepository by TestCreditsRepository() {
    var refreshes = 0

    override suspend fun refresh() {
        refreshes++
    }
}

@OptIn(ExperimentalCoroutinesApi::class)
class PurchaseGrantRefreshTest {
    private val server = MockWebServer().apply { start() }
    private val billing = FakePlayBilling().apply { products = threeProducts() }
    private val api = tailormyresumeApi(TailorMyResumeApiConfig(server.url("/").toString()), tailormyresumeOkHttpClient(FixedToken), tailormyresumeJson())
    private val credits = RefreshCountingCredits()
    private val gateway = RemotePaymentGateway(api, WalletSource(api), billing, FakeUid("uid-1"), CoroutineScope(UnconfinedTestDispatcher()), credits)

    @After
    fun tearDown() = server.shutdown()

    @Test
    fun aGrantedPurchaseRefreshesTheLedgerAndAFailedOneDoesNot() = runTest {
        billing.purchaseResult = PlayPurchaseResult.Done(PlayPurchase("application_pack_5", "token-1", PlayPurchaseState.PURCHASED))
        server.enqueue(MockResponse().setResponseCode(400).setBody(errorJson("PURCHASE_INVALID")))
        gateway.purchase("application_pack_5")
        assertThat(credits.refreshes).isEqualTo(0)

        server.enqueue(MockResponse().setResponseCode(200).setBody(purchaseJson(walletJson(free = 1, purchased = 5))))
        assertThat(gateway.purchase("application_pack_5")).isInstanceOf(PurchaseResult.Completed::class.java)

        assertThat(credits.refreshes).isEqualTo(1)
    }
}
