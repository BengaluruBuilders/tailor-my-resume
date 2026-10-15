package com.tailormyresume.app.billing

import com.google.common.truth.Truth.assertThat
import com.tailormyresume.core.network.TailorMyResumeApiConfig
import com.tailormyresume.core.network.tailormyresumeApi
import com.tailormyresume.core.network.tailormyresumeJson
import com.tailormyresume.core.network.tailormyresumeOkHttpClient
import com.tailormyresume.core.testing.repository.TestCreditsRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import okhttp3.mockwebserver.Dispatcher
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okhttp3.mockwebserver.RecordedRequest
import org.junit.After
import org.junit.Test
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger

class RemotePaymentGatewayAccountSwitchTest {
    private val server = MockWebServer().apply { start() }
    private val api = tailormyresumeApi(TailorMyResumeApiConfig(server.url("/").toString()), tailormyresumeOkHttpClient(FixedToken), tailormyresumeJson())
    private val account = SwitchableUid("uid-1")
    private val source = WalletSource(api, account)
    private val billing = FakePlayBilling()
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val gateway = RemotePaymentGateway(api, source, billing, account, scope, TestCreditsRepository())

    @After
    fun tearDown() {
        scope.cancel()
        server.shutdown()
    }

    private fun recorded() = MockResponse().setResponseCode(201).setBody(purchaseJson(walletJson(purchased = 5)))

    private class PendingThenHeld(private val later: MockResponse) : Dispatcher() {
        private val calls = AtomicInteger()
        private val secondArrived = CountDownLatch(1)
        private val released = CountDownLatch(1)

        override fun dispatch(request: RecordedRequest): MockResponse {
            if (calls.incrementAndGet() == 1) return MockResponse().setResponseCode(409).setBody(errorJson("PURCHASE_PENDING"))
            secondArrived.countDown()
            released.await(HOLD_SECONDS, TimeUnit.SECONDS)
            return later
        }

        fun awaitSecondRequest() = secondArrived.await(HOLD_SECONDS, TimeUnit.SECONDS)

        fun release() = released.countDown()
    }

    @Test
    fun aPurchaseAnswerHeldAcrossAnAccountSwitchIsNotStoredAsTheNewAccountsWallet() = runBlocking<Unit> {
        billing.purchaseResult = PlayPurchaseResult.Done(PlayPurchase(PACK, "token-1", PlayPurchaseState.PURCHASED))
        val held = HeldAnswer(recorded()).also { server.dispatcher = it }
        val purchase = async(Dispatchers.Default) { gateway.purchase(PACK) }
        held.awaitRequest()

        account.value = "uid-2"
        held.release()
        purchase.await()

        assertThat(source.cached).isNull()
    }

    @Test
    fun aRepostAnswerHeldAcrossAnAccountSwitchIsNotStored() = runBlocking<Unit> {
        billing.purchaseResult = PlayPurchaseResult.Done(PlayPurchase(PACK, "token-1", PlayPurchaseState.PURCHASED))
        val scripted = PendingThenHeld(recorded()).also { server.dispatcher = it }
        gateway.purchase(PACK)
        assertThat(scripted.awaitSecondRequest()).isTrue()

        account.value = "uid-2"
        scripted.release()
        withTimeout(HOLD_SECONDS * 1_000) { while (gateway.repostCount() > 0) delay(POLL_MILLIS) }

        assertThat(source.cached).isNull()
    }

    private companion object {
        const val PACK = "application_pack_5"
        const val HOLD_SECONDS = 5L
        const val POLL_MILLIS = 50L
    }
}
