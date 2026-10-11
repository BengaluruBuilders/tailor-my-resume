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
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.cancel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withContext
import okhttp3.mockwebserver.Dispatcher
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okhttp3.mockwebserver.RecordedRequest
import org.junit.After
import org.junit.Test
import java.util.concurrent.BrokenBarrierException
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.CountDownLatch
import java.util.concurrent.CyclicBarrier
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException
import java.util.concurrent.atomic.AtomicInteger

class RemotePaymentGatewayTest {
    private val server = MockWebServer().apply { start() }
    private val billing = FakePlayBilling()
    private val api = tailormyresumeApi(TailorMyResumeApiConfig(server.url("/").toString()), tailormyresumeOkHttpClient(FixedToken), tailormyresumeJson())
    private val source = WalletSource(api)

    @After
    fun tearDown() {
        clock.cancel()
        server.shutdown()
    }

    private fun gateway(uid: String? = "uid-1") = RemotePaymentGateway(api, source, billing, FakeUid(uid), idleScope(), TestCreditsRepository())

    private fun reply(code: Int, body: String) = server.enqueue(MockResponse().setResponseCode(code).setBody(body))

    private fun donePurchase(state: PlayPurchaseState = PlayPurchaseState.PURCHASED) {
        billing.purchaseResult = PlayPurchaseResult.Done(PlayPurchase("application_pack_5", "token-1", state))
    }

    @Test
    fun observedEntitlementEmitsNoCreditsWhenTheWalletCannotBeFetched() = runTest {
        server.shutdown()

        val entitlement = gateway().observeEntitlement().first()

        assertThat(entitlement.totalCredits).isEqualTo(0)
    }

    @Test
    fun entitlementComesFromTheWallet() = runTest {
        reply(200, """{"wallet":${walletJson(free = 0, purchased = 4, unlocked = "\"a1\"")}}""")

        val entitlement = gateway().entitlement()

        assertThat(entitlement.purchasedCredits).isEqualTo(4)
        assertThat(server.takeRequest().path).isEqualTo("/v1/tailormyresume/wallet")
    }

    @Test
    fun packsUsePlayNameAndPriceAndDropPacksPlayDoesNotKnow() = runTest {
        reply(
            200,
            """{"packs":[{"productId":"application_pack_5","credits":5,"creditsExpire":false},
{"productId":"unknown","credits":9,"creditsExpire":false}]}""",
        )

        val packs = gateway().packs()

        assertThat(packs.map { it.id }).containsExactly("application_pack_5")
        assertThat(packs.single().priceInPaise).isEqualTo(14_900)
        assertThat(packs.single().credits).isEqualTo(5)
    }

    @Test
    fun aPurchasedTokenIsPostedWithTheHashedUidAndCompletes() = runTest {
        donePurchase()
        reply(201, purchaseJson(walletJson(free = 1, purchased = 5)))

        val result = gateway("uid-1").purchase("application_pack_5") as PurchaseResult.Completed

        assertThat(result.entitlement.purchasedCredits).isEqualTo(5)
        assertThat(billing.launchedWith).isEqualTo("application_pack_5" to obfuscatedAccountId("uid-1"))
        val request = server.takeRequest()
        assertThat(request.path).isEqualTo("/v1/tailormyresume/purchases")
        assertThat(request.body.readUtf8()).contains("\"purchaseToken\":\"token-1\"")
    }

    @Test
    fun aRepeatedTokenAnswers200AndStillCompletes() = runTest {
        donePurchase()
        reply(200, purchaseJson(walletJson(purchased = 5)))

        assertThat(gateway().purchase("application_pack_5")).isInstanceOf(PurchaseResult.Completed::class.java)
    }

    @Test
    fun serverAnswersAfterPurchasedAreHeldPendingOrUnconfirmed() = runTest {
        donePurchase()
        repeat(3) { reply(502, errorJson("PLAY_UNAVAILABLE")) }
        val held = gateway().purchase("application_pack_5") as PurchaseResult.Pending
        assertThat(held.entitlement.pendingPackIds).containsExactly("application_pack_5")

        listOf(400 to "PURCHASE_INVALID", 403 to "FORBIDDEN").forEach { (status, code) ->
            donePurchase()
            reply(status, errorJson(code))

            val result = gateway().purchase("application_pack_5") as PurchaseResult.Failed

            assertThat(result.reason).isEqualTo(PurchaseFailureReason.PaymentUnconfirmed)
        }
    }

    @Test
    fun aPendingAnswerFromTheServerKeepsThePackPending() = runTest {
        donePurchase()
        reply(409, errorJson("PURCHASE_PENDING"))

        val result = gateway().purchase("application_pack_5") as PurchaseResult.Pending

        assertThat(result.entitlement.pendingPackIds).containsExactly("application_pack_5")
    }

    @Test
    fun aPendingPlayPurchaseIsNeverPostedToTheServer() = runTest {
        donePurchase(PlayPurchaseState.PENDING)

        val result = gateway().purchase("application_pack_5")

        assertThat(result).isInstanceOf(PurchaseResult.Pending::class.java)
        assertThat(server.requestCount).isEqualTo(0)
    }

    @Test
    fun withoutAUidThePurchaseIsUnavailableAndPlayIsNotOpened() = runTest {
        val result = gateway(uid = null).purchase("application_pack_5") as PurchaseResult.Failed

        assertThat(result.reason).isEqualTo(PurchaseFailureReason.PurchaseUnavailable)
        assertThat(billing.launchedWith).isNull()
    }

    @Test
    fun cancellingInPlayCancels() = runTest {
        billing.purchaseResult = PlayPurchaseResult.Cancelled

        assertThat(gateway().purchase("application_pack_5")).isEqualTo(PurchaseResult.Cancelled)
    }

    @Test
    fun restorePostsEveryPurchasedTokenAndSkipsPendingOnes() = runTest {
        billing.owned = listOf(
            PlayPurchase("application_pack_5", "t1", PlayPurchaseState.PURCHASED),
            PlayPurchase("application_pack_5", "t2", PlayPurchaseState.PENDING),
        )
        reply(200, purchaseJson(walletJson(purchased = 5)))
        reply(200, """{"wallet":${walletJson(purchased = 5)}}""")

        val entitlement = gateway().restorePurchases()

        assertThat(entitlement.purchasedCredits).isEqualTo(5)
        assertThat(server.requestCount).isEqualTo(2)
        assertThat(server.takeRequest().body.readUtf8()).contains("\"purchaseToken\":\"t1\"")
    }

    private class ScriptedPurchases : Dispatcher() {
        class Step(val response: MockResponse, val gate: CountDownLatch? = null)

        val steps = ConcurrentLinkedQueue<Step>()
        val bodies = CopyOnWriteArrayList<String>()

        override fun dispatch(request: RecordedRequest): MockResponse {
            if (request.path.orEmpty().endsWith("/purchases").not()) {
                return MockResponse().setResponseCode(200).setBody("""{"wallet":${walletJson()}}""")
            }
            bodies += request.body.readUtf8()
            val step = steps.poll()
            step?.gate?.await(GATE_SECONDS, TimeUnit.SECONDS)
            return step?.response ?: MockResponse().setResponseCode(502).setBody(errorJson("PLAY_UNAVAILABLE"))
        }
    }

    private val scripted = ScriptedPurchases()
    private val wallet = WalletSource(api)
    private val clock = TestScope()

    private fun scriptedGateway(): RemotePaymentGateway {
        server.dispatcher = scripted
        return RemotePaymentGateway(api, wallet, billing, FakeUid("uid-1"), clock, TestCreditsRepository())
    }

    private fun recordedStep(gate: CountDownLatch? = null) =
        ScriptedPurchases.Step(MockResponse().setResponseCode(201).setBody(purchaseJson(walletJson(purchased = 5))), gate)

    private suspend fun settleIo() = repeat(IO_ROUNDS) {
        clock.testScheduler.runCurrent()
        withContext(Dispatchers.Default) { delay(IO_PAUSE_MILLIS) }
    }

    private suspend fun elapseBackoff() {
        val start = clock.testScheduler.currentTime
        BACKOFF_DEADLINES_MILLIS.forEach { deadline ->
            clock.testScheduler.advanceTimeBy(start + deadline - clock.testScheduler.currentTime)
            settleIo()
        }
    }

    private suspend fun awaitCompletion(job: Job) {
        while (!job.isCompleted) {
            clock.testScheduler.runCurrent()
            withContext(Dispatchers.Default) { delay(IO_PAUSE_MILLIS) }
        }
    }

    private suspend fun awaitPosts(count: Int) {
        repeat(IO_ROUNDS * 10) {
            clock.testScheduler.runCurrent()
            if (scripted.bodies.size >= count) return
            withContext(Dispatchers.Default) { delay(IO_PAUSE_MILLIS) }
        }
    }

    private suspend fun RemotePaymentGateway.restoreOwned(vararg tokens: String) {
        billing.owned = tokens.map { PlayPurchase(PACK, it, PlayPurchaseState.PURCHASED) }
        awaitCompletion(clock.launch { restorePurchases() })
    }

    @Test
    fun clearCreditsCancelsAnInFlightRepostSoTheWalletAndPendingStayEmpty() = runTest {
        val gateway = scriptedGateway()
        val gate = CountDownLatch(1)
        gateway.restoreOwned("token-A")
        scripted.steps += recordedStep(gate)
        clock.testScheduler.advanceTimeBy(2_000)
        awaitPosts(count = 2)

        gateway.clearCredits()
        gate.countDown()
        settleIo()
        elapseBackoff()

        assertThat(wallet.cached).isNull()
        assertThat(gateway.observeEntitlement().first().pendingPackIds).isEmpty()
        assertThat(scripted.bodies).hasSize(2)
    }

    @Test
    fun aSignOutThenASamePackPurchaseByAnotherAccountStartsItsOwnRepostAndNeverPostsTheOldToken() = runTest {
        val gateway = scriptedGateway()
        gateway.restoreOwned("token-A")
        gateway.clearCredits()
        gateway.restoreOwned("token-B")
        scripted.steps += recordedStep()

        elapseBackoff()

        val afterSignOut = scripted.bodies.drop(1)
        assertThat(afterSignOut.none { "token-A" in it }).isTrue()
        assertThat(afterSignOut.last()).contains("\"purchaseToken\":\"token-B\"")
        assertThat(gateway.observeEntitlement().first().pendingPackIds).isEmpty()
        assertThat(wallet.cached?.purchasedCredits).isEqualTo(5)
    }

    @Test
    fun twoTokensOfOnePackRepostSeparatelyAndARepeatedTokenDoesNot() = runTest {
        val gateway = scriptedGateway()
        gateway.restoreOwned("t1", "t2")
        gateway.restoreOwned("t1", "t2")
        scripted.steps += listOf(recordedStep(), recordedStep())

        elapseBackoff()

        assertThat(scripted.bodies).hasSize(6)
        assertThat(scripted.bodies.count { "\"purchaseToken\":\"t1\"" in it }).isEqualTo(3)
        assertThat(scripted.bodies.count { "\"purchaseToken\":\"t2\"" in it }).isEqualTo(3)
    }

    @Test
    fun concurrentRestoresHoldingManyPacksLoseNoPending() = runTest {
        val gateway = scriptedGateway()
        val packs = List(HELD_PACKS) { "pack_$it" }
        billing.owned = packs.map { PlayPurchase(it, "t-$it", PlayPurchaseState.PENDING) }

        withContext(Dispatchers.Default) {
            coroutineScope { repeat(WRITERS) { launch { gateway.restorePurchases() } } }
        }

        assertThat(gateway.observeEntitlement().first().pendingPackIds).containsExactlyElementsIn(packs)
    }

    @Test
    fun aRestoreInFlightAtSignOutNeverRepostsTheOldTokenOrHoldsItsPack() = runTest {
        val gateway = scriptedGateway()
        val gate = CountDownLatch(1)
        billing.owned = listOf(PlayPurchase(PACK, "token-A", PlayPurchaseState.PURCHASED))
        scripted.steps += ScriptedPurchases.Step(MockResponse().setResponseCode(502).setBody(errorJson("PLAY_UNAVAILABLE")), gate)
        val restore = clock.launch { gateway.restorePurchases() }
        awaitPosts(count = 1)

        gateway.clearCredits()
        gate.countDown()
        awaitCompletion(restore)
        elapseBackoff()

        assertThat(scripted.bodies).hasSize(1)
        assertThat(gateway.observeEntitlement().first().pendingPackIds).isEmpty()
        assertThat(gateway.repostCount()).isEqualTo(0)
    }

    @Test
    fun aPurchaseWhoseSessionExpiresMidSettleNeverRepostsTheOldTokenAfterTheSignOut() = runTest {
        val gateway = scriptedGateway()
        val gate = CountDownLatch(1)
        donePurchase()
        scripted.steps += ScriptedPurchases.Step(MockResponse().setResponseCode(401).setBody(errorJson("UNAUTHENTICATED")), gate)
        val purchase = clock.launch { gateway.purchase(PACK) }
        awaitPosts(count = 1)

        gateway.clearCredits()
        gate.countDown()
        elapseBackoff()
        awaitCompletion(purchase)

        assertThat(scripted.bodies).hasSize(REQUESTS_SENT_BEFORE_THE_CLEAR)
        assertThat(gateway.observeEntitlement().first().pendingPackIds).isEmpty()
        assertThat(gateway.repostCount()).isEqualTo(0)
    }

    @Test
    fun aFinishedRepostLeavesNoJobInTheRegistry() = runTest {
        val gateway = scriptedGateway()
        gateway.restoreOwned("t1")
        assertThat(gateway.repostCount()).isEqualTo(1)
        scripted.steps += recordedStep()

        elapseBackoff()

        assertThat(scripted.bodies).hasSize(2)
        assertThat(gateway.repostCount()).isEqualTo(0)
    }

    private class StripedBilling(private val delegate: PlayBilling, private val stripes: Int) : PlayBilling by delegate {
        private val calls = AtomicInteger()
        var striping = false

        override suspend fun ownedPurchases(): List<PlayPurchase> {
            val owned = delegate.ownedPurchases()
            if (!striping) return owned
            val stripe = calls.getAndIncrement() % stripes
            return owned.chunked(1 + EXTRAS_PER_PACK).filterIndexed { index, _ -> index % stripes == stripe }.flatten()
        }
    }

    private class AnswerByToken(private val sameInstant: CyclicBarrier) : Dispatcher() {
        override fun dispatch(request: RecordedRequest): MockResponse {
            if (request.path.orEmpty().endsWith("/purchases").not()) {
                return MockResponse().setResponseCode(200).setBody("""{"wallet":${walletJson()}}""")
            }
            try {
                sameInstant.await(BARRIER_MILLIS, TimeUnit.MILLISECONDS)
            } catch (_: BrokenBarrierException) {
            } catch (_: TimeoutException) {
            }
            val token = request.body.readUtf8().substringAfter("\"purchaseToken\":\"").substringBefore('"')
            return if (token.removePrefix("t-").toInt() % 2 == 0) {
                MockResponse().setResponseCode(201).setBody(purchaseJson(walletJson(purchased = 5)))
            } else {
                MockResponse().setResponseCode(400).setBody(errorJson("PURCHASE_INVALID"))
            }
        }
    }

    @Test
    fun concurrentRecordedAndUnconfirmedRemovalsLoseNoConcurrentHold() = runTest {
        server.dispatcher = AnswerByToken(CyclicBarrier(CONCURRENT_REQUESTS))
        val stripedBilling = StripedBilling(billing, RACING_WRITERS)
        val gateway = RemotePaymentGateway(api, wallet, stripedBilling, FakeUid("uid-1"), clock, TestCreditsRepository())
        val settled = List(SETTLED_PACKS) { "settled_$it" }
        val extras = settled.indices.flatMap { index -> List(EXTRAS_PER_PACK) { "extra_${index}_$it" } }
        val filler = List(FILLER_PACKS) { "filler_$it" }
        billing.owned = filler.map { PlayPurchase(it, "t-$it", PlayPurchaseState.PENDING) } +
            settled.mapIndexed { index, pack -> PlayPurchase(pack, "t-$index", PlayPurchaseState.PENDING) }
        gateway.restorePurchases()
        billing.owned = settled.indices.flatMap { index ->
            listOf(PlayPurchase(settled[index], "t-$index", PlayPurchaseState.PURCHASED)) +
                List(EXTRAS_PER_PACK) { PlayPurchase("extra_${index}_$it", "t-x$index-$it", PlayPurchaseState.PENDING) }
        }

        stripedBilling.striping = true
        withContext(Dispatchers.Default) {
            coroutineScope { repeat(RACING_WRITERS) { launch { gateway.restorePurchases() } } }
        }

        assertThat(gateway.observeEntitlement().first().pendingPackIds).containsExactlyElementsIn(filler + extras)
    }

    @Test
    fun aWalletRefreshInFlightAtSignOutDoesNotRestoreTheOldWallet() = runTest {
        val held = HeldAnswer(MockResponse().setResponseCode(200).setBody("""{"wallet":${walletJson(free = 0, purchased = 4)}}""")).also { server.dispatcher = it }
        val gateway = gateway()
        val refresh = async(Dispatchers.Default) { runCatching { gateway.entitlement() } }
        held.awaitRequest()

        gateway.clearCredits()
        held.release()
        refresh.await()

        assertThat(source.cached).isNull()
    }

    @Test
    fun clearCreditsWithNoRepostJobReturnsNoCredits() = runTest {
        val gateway = scriptedGateway()
        billing.owned = listOf(PlayPurchase(PACK, "t1", PlayPurchaseState.PENDING))
        gateway.restorePurchases()

        val cleared = gateway.clearCredits()

        assertThat(cleared.totalCredits).isEqualTo(0)
        assertThat(cleared.pendingPackIds).isEmpty()
        assertThat(wallet.cached).isNull()
        assertThat(gateway.observeEntitlement().first().pendingPackIds).isEmpty()
    }

    private companion object {
        const val PACK = "application_pack_5"
        const val GATE_SECONDS = 5L
        const val IO_ROUNDS = 20
        const val IO_PAUSE_MILLIS = 15L
        const val HELD_PACKS = 1_500
        const val WRITERS = 8
        const val REQUESTS_SENT_BEFORE_THE_CLEAR = 2
        const val SETTLED_PACKS = 400
        const val EXTRAS_PER_PACK = 1
        const val RACING_WRITERS = 8
        const val CONCURRENT_REQUESTS = 5
        const val BARRIER_MILLIS = 250L
        const val FILLER_PACKS = 8_000
        val BACKOFF_DEADLINES_MILLIS = listOf(2_000L, 6_000L, 14_000L, 30_000L, 62_000L)
    }
}
