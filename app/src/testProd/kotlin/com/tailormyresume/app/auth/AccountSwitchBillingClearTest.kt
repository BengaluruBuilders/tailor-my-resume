package com.tailormyresume.app.auth

import com.google.common.truth.Truth.assertThat
import com.tailormyresume.app.ai.RemoteJobAnalysisSource
import com.tailormyresume.app.billing.FakePlayBilling
import com.tailormyresume.app.billing.PlayPurchase
import com.tailormyresume.app.billing.PlayPurchaseResult
import com.tailormyresume.app.billing.PlayPurchaseState
import com.tailormyresume.app.billing.RemotePaymentGateway
import com.tailormyresume.app.billing.SwitchableUid
import com.tailormyresume.app.billing.WalletSource
import com.tailormyresume.app.billing.idleScope
import com.tailormyresume.core.data.repository.PendingReportQueue
import com.tailormyresume.core.testing.mock.TestMockStateStore
import com.tailormyresume.core.testing.repository.TestSessionRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeoutOrNull
import okhttp3.mockwebserver.Dispatcher
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okhttp3.mockwebserver.RecordedRequest
import org.junit.After
import org.junit.Test
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger

class AccountSwitchBillingClearTest {
    private val server = MockWebServer().apply { start() }
    private val api = server.api()
    private val account = SwitchableUid("uid-0")
    private val billing = FakePlayBilling()
    private val payments = RemotePaymentGateway(api, WalletSource(api, account), billing, account, idleScope())
    private val session = TestSessionRepository()
    private val store = TestMockStateStore()
    private val cleaner = SignOutCleaner(payments, RemoteJobAnalysisSource(api, NoMatcher), PendingReportQueue(store), store)

    private val purchasesRequests = AtomicInteger()

    private val routes = object : Dispatcher() {
        override fun dispatch(request: RecordedRequest): MockResponse = when {
            request.path.orEmpty().endsWith("/me") -> jsonResponse(200, ME_BODY)
            request.path.orEmpty().endsWith("/purchases") && purchasesRequests.incrementAndGet() > 1 ->
                jsonResponse(200, PURCHASES_BODY).setHeadersDelay(SLOW_MILLIS, TimeUnit.MILLISECONDS)
            request.path.orEmpty().endsWith("/purchases") -> jsonResponse(200, PURCHASES_BODY)
            else -> jsonResponse(200, WALLET_BODY).setHeadersDelay(SLOW_MILLIS, TimeUnit.MILLISECONDS)
        }
    }

    @After
    fun tearDown() = runCatching { server.shutdown() }.let { }

    private fun gateway() =
        RemoteSignInGateway(completeConfig, ScriptedCredentials(), ScriptedFirebase(), api, session, cleaner, LocalDataWiper.None)

    private suspend fun holdAPackAndCacheHistoryForUid0() {
        server.dispatcher = routes
        billing.purchaseResult = PlayPurchaseResult.Done(PlayPurchase(PACK, "token-1", PlayPurchaseState.PENDING))
        payments.purchase(PACK)
        payments.purchaseHistory()
    }

    private suspend fun pendingPacks() = payments.observeEntitlement().first().pendingPackIds

    private suspend fun cachedHistory() = withTimeoutOrNull(FAST_WAIT_MILLIS) { payments.observePurchaseHistory().first() }

    @Test
    fun anAccountSwitchSignInClearsThePreviousAccountsPendingPacksAndHistory() = runBlocking<Unit> {
        holdAPackAndCacheHistoryForUid0()
        session.saveLastAccountId("uid-0")
        account.value = "uid-1"

        gateway().signIn()
        account.value = "uid-0"

        assertThat(pendingPacks()).isEmpty()
        assertThat(cachedHistory()).isNull()
    }

    @Test
    fun aSameAccountSignInKeepsThePendingPacksAndHistory() = runBlocking<Unit> {
        account.value = "uid-1"
        holdAPackAndCacheHistoryForUid0()
        session.saveLastAccountId("uid-1")

        gateway().signIn()

        assertThat(pendingPacks()).containsExactly(PACK)
        assertThat(cachedHistory()).hasSize(1)
    }

    private companion object {
        const val PACK = "application_pack_5"
        const val FAST_WAIT_MILLIS = 700L
        const val SLOW_MILLIS = 3_000L
        const val PURCHASES_BODY = """{"purchases":[{"orderId":"GPA.7","productId":"application_pack_5","creditsGranted":5,"purchasedAt":"2026-10-07T09:12:00Z","state":"COMPLETED"}]}"""
        const val WALLET_BODY = """{"wallet":{"freeCredits":1,"purchasedCredits":0,"analysesLeftToday":3,"freeTailoringsLeftToday":1,"day":"2026-10-07","resetsAt":"2026-10-07T18:30:00Z","unlockedApplicationIds":[]}}"""
    }
}
