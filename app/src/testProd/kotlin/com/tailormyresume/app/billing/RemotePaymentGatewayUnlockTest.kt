package com.tailormyresume.app.billing

import com.google.common.truth.Truth.assertThat
import com.tailormyresume.core.domain.CreditSpend
import com.tailormyresume.core.network.TailorMyResumeApiConfig
import com.tailormyresume.core.network.tailormyresumeApi
import com.tailormyresume.core.network.tailormyresumeJson
import com.tailormyresume.core.network.tailormyresumeOkHttpClient
import com.tailormyresume.core.testing.repository.TestCreditsRepository
import kotlinx.coroutines.test.runTest
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Test

class RemotePaymentGatewayUnlockTest {
    private val server = MockWebServer().apply { start() }
    private val api = tailormyresumeApi(TailorMyResumeApiConfig(server.url("/").toString()), tailormyresumeOkHttpClient(FixedToken), tailormyresumeJson())
    private val gateway = RemotePaymentGateway(api, WalletSource(api), FakePlayBilling(), FakeUid("uid-1"), idleScope(), TestCreditsRepository())

    @After
    fun tearDown() = server.shutdown()

    @Test
    fun unlockMakesNoRequestInProd() = runTest {
        val spend = gateway.unlock("application-1")

        assertThat(spend).isInstanceOf(CreditSpend.Spent::class.java)
        assertThat(server.requestCount).isEqualTo(0)
    }
}
