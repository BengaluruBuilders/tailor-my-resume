package com.tailormyresume.app.billing

import com.google.common.truth.Truth.assertThat
import com.tailormyresume.app.auth.AccountOperationGuard
import com.tailormyresume.app.auth.AuthFailure
import com.tailormyresume.app.auth.AuthOutcome
import com.tailormyresume.app.auth.FirebaseSessionClient
import com.tailormyresume.app.auth.FirebaseUser
import com.tailormyresume.app.auth.api
import com.tailormyresume.core.data.account.LocalWorkFence
import com.tailormyresume.core.model.AccountMode
import com.tailormyresume.core.model.SignInAccount
import com.tailormyresume.core.testing.repository.TestCreditsRepository
import com.tailormyresume.core.testing.repository.TestSessionRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withContext
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Test
import java.util.concurrent.TimeUnit

class HistoricalPurchaseSettlementTest {
    private val server = MockWebServer().apply { start() }
    private val api = server.api()
    private val uids = SwitchableUid("uid-1")
    private val sessions = TestSessionRepository()
    private val fence = LocalWorkFence()
    private val wallet = WalletSource(api, uids)
    private val billing = FakePlayBilling()
    private val credits = TestCreditsRepository()
    private var verified: FirebaseUser? = FirebaseUser("uid-1", "", "", googleSubject = "google-1")
    private val firebase = object : FirebaseSessionClient {
        override suspend fun currentUser() = verified
        override suspend fun signIn(googleIdToken: String): AuthOutcome<FirebaseUser> = AuthOutcome.Failure(AuthFailure.Failed)
        override fun signOut() = Unit
    }

    @After fun tearDown() {
        server.shutdown()
    }
    private suspend fun google() {
        sessions.saveAccount(SignInAccount("uid-1", "", ""))
    }
    private fun subject(scope: kotlinx.coroutines.CoroutineScope) = HistoricalPurchaseSettlement(api, wallet, billing, uids, sessions, firebase, AccountOperationGuard(uids, fence), scope, credits)
    private fun answer(code: Int = 201, body: String = purchaseJson(walletJson(free = 0, purchased = 5))) {
        server.enqueue(MockResponse().setResponseCode(code).setBody(body))
    }

    @Test fun startupRepostsPaidUnpostedTokenWithoutLoadingProducts() = runTest {
        google()
        billing.owned = listOf(paid)
        answer()
        subject(backgroundScope).reconcile()
        assertThat(billing.queries).isEqualTo(1)
        val request = server.takeRequest()
        assertThat(request.method).isEqualTo("POST")
        assertThat(request.path).endsWith("/purchases")
        assertThat(request.body.readUtf8()).contains("historical-token")
        assertThat(wallet.cached?.purchasedCredits).isEqualTo(5)
    }

    @Test fun pendingGrantsNothingUntilAnUnsolicitedCompletion() = runTest {
        google()
        billing.owned = listOf(paid.copy(state = PlayPurchaseState.PENDING))
        val subject = subject(backgroundScope)
        subject.reconcile()
        assertThat(server.requestCount).isEqualTo(0)
        answer()
        subject.reconcile(paid)
        assertThat(server.requestCount).isEqualTo(1)
        assertThat(wallet.cached?.purchasedCredits).isEqualTo(5)
    }

    @Test fun transientFailureRepostsTheSameTokenAndRefreshesTheWallet() = runTest {
        google()
        billing.owned = listOf(paid)
        answer(502, errorJson("PLAY_UNAVAILABLE"))
        val subject = subject(backgroundScope)
        subject.reconcile()
        assertThat(subject.repostCount()).isEqualTo(1)
        val first = server.takeRequest().body.readUtf8()
        answer()
        advanceTimeBy(2_000)
        runCurrent()
        val second = withContext(Dispatchers.IO) { server.takeRequest(5, TimeUnit.SECONDS) }
        assertThat(second?.body?.readUtf8()).isEqualTo(first)
        withContext(Dispatchers.IO) { kotlinx.coroutines.withTimeout(5_000) { wallet.wallet.first { it != null } } }
        assertThat(wallet.cached?.purchasedCredits).isEqualTo(5)
        subject.invalidate()
    }

    @Test fun signOutCancelsRetryBeforeAnotherPost() = runTest {
        google()
        billing.owned = listOf(paid)
        answer(502, errorJson("PLAY_UNAVAILABLE"))
        val subject = subject(backgroundScope)
        subject.reconcile()
        subject.invalidate()
        advanceTimeBy(64_000)
        runCurrent()
        assertThat(subject.repostCount()).isEqualTo(0)
        assertThat(server.requestCount).isEqualTo(1)
    }

    @Test fun anonymousOtherAndMissingGoogleIdentityNeverQueryOrPost() = runTest {
        val subject = subject(backgroundScope)
        for (mode in listOf(AccountMode.ANONYMOUS, AccountMode.OTHER)) {
            sessions.saveAccount(SignInAccount("uid-1", "", "", mode))
            assertThat(runCatching { subject.reconcile() }.isFailure).isTrue()
        }
        google()
        verified = verified?.copy(googleSubject = null)
        assertThat(runCatching { subject.reconcile() }.isFailure).isTrue()
        assertThat(billing.queries).isEqualTo(0)
        assertThat(server.requestCount).isEqualTo(0)
    }

    @Test fun lateReplyCannotApplyAfterSwitchingAwayAndBackToTheSameUid() = runBlocking {
        google()
        billing.owned = listOf(paid)
        val held = HeldAnswer(MockResponse().setResponseCode(201).setBody(purchaseJson(walletJson(purchased = 5))))
        server.dispatcher = held
        val subject = subject(this)
        val work = async(Dispatchers.Default) { runCatching { subject.reconcile() } }
        held.awaitRequest()
        uids.value = "uid-2"
        fence.invalidate()
        uids.value = "uid-1"
        held.release()
        assertThat(work.await().isFailure).isTrue()
        assertThat(wallet.cached).isNull()
        assertThat(subject.repostCount()).isEqualTo(0)
    }

    @Test fun cancelledCallerDoesNotCreateBackgroundReposts() = runBlocking {
        google()
        billing.owned = listOf(paid)
        val held = HeldAnswer(MockResponse().setResponseCode(502).setBody(errorJson("PLAY_UNAVAILABLE")))
        server.dispatcher = held
        val subject = subject(this)
        val work = launch(Dispatchers.Default) { subject.reconcile() }
        held.awaitRequest()
        work.cancel()
        held.release()
        work.join()
        assertThat(subject.repostCount()).isEqualTo(0)
        assertThat(wallet.cached).isNull()
    }

    private companion object {
        val paid = PlayPurchase("application_pack_5", "historical-token", PlayPurchaseState.PURCHASED)
    }
}
