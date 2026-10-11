package com.tailormyresume.app.billing

import com.google.common.truth.Truth.assertThat
import com.tailormyresume.core.data.repository.CreditsRepository
import com.tailormyresume.core.model.SignInAccount
import com.tailormyresume.core.network.TailorMyResumeApiConfig
import com.tailormyresume.core.network.tailormyresumeApi
import com.tailormyresume.core.network.tailormyresumeJson
import com.tailormyresume.core.network.tailormyresumeOkHttpClient
import com.tailormyresume.core.testing.repository.TestCreditsRepository
import com.tailormyresume.core.testing.repository.TestSessionRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okhttp3.mockwebserver.RecordedRequest
import okhttp3.mockwebserver.SocketPolicy
import org.junit.After
import org.junit.Test
import java.util.concurrent.TimeUnit

private class RestartCreditsCounter : CreditsRepository by TestCreditsRepository() {
    @Volatile
    var refreshes = 0

    override suspend fun refresh() {
        refreshes++
    }
}

class PurchaseProcessDeathRecoveryTest {
    private val server = MockWebServer().apply { start() }
    private val session = TestSessionRepository()
    private val api = tailormyresumeApi(
        TailorMyResumeApiConfig(server.url("/").toString()),
        tailormyresumeOkHttpClient(FixedToken),
        tailormyresumeJson(),
    )
    private val token = PlayPurchase("application_pack_5", "token-1", PlayPurchaseState.PURCHASED)
    private val billing = FakePlayBilling().apply {
        purchaseResult = PlayPurchaseResult.Done(token)
        owned = listOf(token)
    }
    private val firstProcess = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val secondProcess = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val creditsAfterRestart = RestartCreditsCounter()

    @After
    fun tearDown() {
        firstProcess.cancel()
        secondProcess.cancel()
        server.shutdown()
    }

    private fun gateway(scope: CoroutineScope, credits: CreditsRepository) =
        RemotePaymentGateway(api, WalletSource(api), billing, FakeUid("uid-1"), scope, credits)

    private fun nextRequest(): RecordedRequest = checkNotNull(server.takeRequest(5, TimeUnit.SECONDS)) { "no request arrived" }

    private suspend fun awaitUntil(condition: () -> Boolean) = withTimeout(5_000) { while (!condition()) delay(20) }

    private fun RecordedRequest.isPurchasePost() = method == "POST" && path == PURCHASES_PATH

    @Test
    fun aPurchaseInterruptedByProcessDeathIsPostedOnceMoreAndCreditedAfterRestart() = runBlocking {
        server.enqueue(MockResponse().setSocketPolicy(SocketPolicy.NO_RESPONSE))
        val firstGateway = gateway(firstProcess, TestCreditsRepository())
        val purchasing = launch(Dispatchers.IO) { firstGateway.purchase("application_pack_5") }
        val interrupted = nextRequest()
        assertThat(interrupted.isPurchasePost()).isTrue()
        assertThat(interrupted.body.readUtf8()).contains("\"purchaseToken\":\"token-1\"")

        firstProcess.cancel()
        purchasing.cancel()
        purchasing.join()
        server.enqueue(MockResponse().setResponseCode(201).setBody(purchaseV2("application_pack_5", 5)))
        repeat(2) {
            server.enqueue(MockResponse().setResponseCode(200).setBody("""{"wallet":${walletV2(free = 1, purchased = 5)}}"""))
        }
        val secondGateway = gateway(secondProcess, creditsAfterRestart)
        session.saveAccount(account)
        PurchaseRestorer({ secondGateway }, { billing }, session, secondProcess).start()

        val repost = nextRequest()
        assertThat(repost.isPurchasePost()).isTrue()
        assertThat(repost.body.readUtf8()).contains("\"purchaseToken\":\"token-1\"")
        awaitUntil { creditsAfterRestart.refreshes == 1 }
        assertThat(secondGateway.entitlement().purchasedCredits).isEqualTo(5)
    }

    @Test
    fun aTokenThatPlayNoLongerListsIsNotPostedAgainAtTheNextStart() = runBlocking {
        server.enqueue(MockResponse().setResponseCode(201).setBody(purchaseV2("application_pack_5", 5)))
        server.enqueue(MockResponse().setResponseCode(200).setBody("""{"wallet":${walletV2(free = 1, purchased = 5)}}"""))
        session.saveAccount(account)
        val firstStart = gateway(secondProcess, creditsAfterRestart)
        PurchaseRestorer({ firstStart }, { billing }, session, secondProcess).start()
        assertThat(nextRequest().isPurchasePost()).isTrue()
        nextRequest()
        awaitUntil { creditsAfterRestart.refreshes == 1 }

        billing.owned = emptyList()
        server.enqueue(MockResponse().setResponseCode(200).setBody("""{"wallet":${walletV2(free = 1, purchased = 5)}}"""))
        val nextProcess = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        val laterStart = gateway(nextProcess, RestartCreditsCounter())
        PurchaseRestorer({ laterStart }, { billing }, session, nextProcess).start()
        val later = nextRequest()
        delay(SETTLE_MILLIS)
        nextProcess.cancel()

        assertThat(later.isPurchasePost()).isFalse()
        assertThat(server.requestCount).isEqualTo(3)
    }

    private companion object {
        const val PURCHASES_PATH = "/v1/tailormyresume/purchases"
        const val SETTLE_MILLIS = 300L
        val account = SignInAccount("uid-1", "Priya", "p@example.com")
    }
}
