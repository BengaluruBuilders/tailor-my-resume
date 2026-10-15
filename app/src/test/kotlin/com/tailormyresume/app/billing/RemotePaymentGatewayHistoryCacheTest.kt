package com.tailormyresume.app.billing

import com.google.common.truth.Truth.assertThat
import com.tailormyresume.core.domain.PurchaseRecord
import com.tailormyresume.core.domain.PurchaseState
import com.tailormyresume.core.network.TailorMyResumeApiConfig
import com.tailormyresume.core.network.tailormyresumeApi
import com.tailormyresume.core.network.tailormyresumeJson
import com.tailormyresume.core.network.tailormyresumeOkHttpClient
import com.tailormyresume.core.testing.repository.TestCreditsRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import kotlinx.coroutines.withTimeoutOrNull
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Test
import java.util.concurrent.TimeUnit
import kotlin.time.Instant

class RemotePaymentGatewayHistoryCacheTest {
    private val server = MockWebServer().apply { start() }
    private val api = tailormyresumeApi(TailorMyResumeApiConfig(server.url("/").toString()), tailormyresumeOkHttpClient(FixedToken), tailormyresumeJson())
    private val account = SwitchableUid("uid-1")
    private val gateway = RemotePaymentGateway(api, WalletSource(api, account), FakePlayBilling(), account, idleScope(), TestCreditsRepository())

    @After
    fun tearDown() {
        server.shutdown()
    }

    private fun purchasesBody(vararg orders: String) = """{"purchases":[${orders.joinToString(",") { order ->
        """{"orderId":"$order","productId":"application_pack_5","creditsGranted":5,"purchasedAt":"2026-10-0${order.last()}T09:12:00Z","state":"COMPLETED"}"""
    }}]}"""

    private fun answer(body: String, delayMillis: Long = 0) =
        server.enqueue(MockResponse().setResponseCode(200).setBody(body).setHeadersDelay(delayMillis, TimeUnit.MILLISECONDS))

    private fun fail() = server.enqueue(MockResponse().setResponseCode(500).setBody("""{"error":{"code":"internal","message":"m"}}"""))

    private suspend fun warmCache() {
        answer(purchasesBody("GPA.7"))
        gateway.purchaseHistory()
    }

    private val warm = PurchaseRecord("application_pack_5", "GPA.7", Instant.parse("2026-10-07T09:12:00Z"), PurchaseState.COMPLETED)

    @Test
    fun cachedHistoryIsEmittedBeforeTheRefreshAnswers() = runBlocking<Unit> {
        warmCache()
        answer(purchasesBody("GPA.7", "GPA.8"), delayMillis = SLOW_ANSWER_MILLIS)

        val emitted = withTimeout(FAST_WAIT_MILLIS) { gateway.observePurchaseHistory().first() }

        assertThat(emitted).containsExactly(warm)
    }

    @Test
    fun theRefreshedHistoryFollowsTheCachedOne() = runBlocking<Unit> {
        warmCache()
        answer(purchasesBody("GPA.7", "GPA.8"))

        val emitted = withTimeout(SLOW_ANSWER_MILLIS) { gateway.observePurchaseHistory().take(2).toList() }

        assertThat(emitted.map { list -> list.map(PurchaseRecord::orderId) }).containsExactly(listOf("GPA.7"), listOf("GPA.7", "GPA.8")).inOrder()
    }

    @Test
    fun withoutACacheNothingIsEmittedBeforeTheAnswer() = runBlocking<Unit> {
        answer(purchasesBody("GPA.7"), delayMillis = SLOW_ANSWER_MILLIS)

        val early = withTimeoutOrNull(FAST_WAIT_MILLIS) { gateway.observePurchaseHistory().first() }

        assertThat(early).isNull()
    }

    @Test
    fun aFailedRefreshKeepsTheCachedList() = runBlocking<Unit> {
        warmCache()
        fail()

        val emitted = withTimeout(SLOW_ANSWER_MILLIS) { gateway.observePurchaseHistory().toList() }

        assertThat(emitted).containsExactly(listOf(warm))
    }

    @Test
    fun clearingCreditsEmptiesTheCache() = runBlocking<Unit> {
        warmCache()
        gateway.clearCredits()
        answer(purchasesBody("GPA.9"), delayMillis = SLOW_ANSWER_MILLIS)

        val early = withTimeoutOrNull(FAST_WAIT_MILLIS) { gateway.observePurchaseHistory().first() }

        assertThat(early).isNull()
    }

    @Test
    fun anAnswerFromBeforeTheClearIsNotStored() = runBlocking<Unit> {
        answer(purchasesBody("GPA.7"), delayMillis = SLOW_ANSWER_MILLIS / 2)
        val inFlight = async(Dispatchers.IO) { gateway.purchaseHistory() }
        server.takeRequest(SLOW_ANSWER_MILLIS, TimeUnit.MILLISECONDS)
        gateway.clearCredits()
        inFlight.await()
        answer(purchasesBody("GPA.9"), delayMillis = SLOW_ANSWER_MILLIS)

        val early = withTimeoutOrNull(FAST_WAIT_MILLIS) { gateway.observePurchaseHistory().first() }

        assertThat(early).isNull()
    }

    @Test
    fun anotherAccountDoesNotSeeTheCachedHistoryWithoutAClear() = runBlocking<Unit> {
        warmCache()
        account.value = "uid-2"
        answer(purchasesBody("GPA.9"), delayMillis = SLOW_ANSWER_MILLIS)

        val early = withTimeoutOrNull(FAST_WAIT_MILLIS) { gateway.observePurchaseHistory().first() }

        assertThat(early).isNull()
    }

    @Test
    fun aFailedRefreshDoesNotRevealTheOtherAccountsHistory() = runBlocking<Unit> {
        warmCache()
        account.value = "uid-2"
        fail()

        val outcome = runCatching { withTimeout(SLOW_ANSWER_MILLIS) { gateway.observePurchaseHistory().toList() } }

        assertThat(outcome.isFailure).isTrue()
    }

    @Test
    fun anAnswerFetchedForAnotherAccountIsNotStored() = runBlocking<Unit> {
        answer(purchasesBody("GPA.7"), delayMillis = SLOW_ANSWER_MILLIS / 2)
        val inFlight = async(Dispatchers.IO) { gateway.purchaseHistory() }
        server.takeRequest(SLOW_ANSWER_MILLIS, TimeUnit.MILLISECONDS)
        account.value = "uid-2"
        inFlight.await()
        answer(purchasesBody("GPA.9"), delayMillis = SLOW_ANSWER_MILLIS)

        val early = withTimeoutOrNull(FAST_WAIT_MILLIS) { gateway.observePurchaseHistory().first() }

        assertThat(early).isNull()
    }

    private companion object {
        const val FAST_WAIT_MILLIS = 700L
        const val SLOW_ANSWER_MILLIS = 3_000L
    }
}
