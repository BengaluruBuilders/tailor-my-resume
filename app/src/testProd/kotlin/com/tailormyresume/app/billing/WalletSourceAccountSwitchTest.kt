package com.tailormyresume.app.billing

import com.google.common.truth.Truth.assertThat
import com.tailormyresume.core.network.TailorMyResumeApiConfig
import com.tailormyresume.core.network.tailormyresumeApi
import com.tailormyresume.core.network.tailormyresumeJson
import com.tailormyresume.core.network.tailormyresumeOkHttpClient
import com.tailormyresume.core.testing.repository.TestCreditsRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeoutOrNull
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Test
import java.util.concurrent.TimeUnit

class WalletSourceAccountSwitchTest {
    private val server = MockWebServer().apply { start() }
    private val api = tailormyresumeApi(TailorMyResumeApiConfig(server.url("/").toString()), tailormyresumeOkHttpClient(FixedToken), tailormyresumeJson())
    private val account = SwitchableUid("uid-1")
    private val source = WalletSource(api, account)
    private val gateway = RemotePaymentGateway(api, source, FakePlayBilling(), account, idleScope(), TestCreditsRepository())

    @After
    fun tearDown() {
        server.shutdown()
    }

    private fun walletAnswer(purchased: Int) = MockResponse().setResponseCode(200).setBody("""{"wallet":${walletJson(purchased = purchased)}}""")

    @Test
    fun anotherAccountDoesNotSeeTheCachedWalletWithoutAClear() = runBlocking<Unit> {
        server.enqueue(walletAnswer(purchased = 4))
        source.refresh()
        account.value = "uid-2"

        assertThat(source.cached).isNull()
        assertThat(withTimeoutOrNull(FAST_WAIT_MILLIS) { source.wallet.first() }).isNull()
    }

    @Test
    fun anotherAccountDoesNotSeeTheCachedCreditsWhileTheRefreshIsPending() = runBlocking<Unit> {
        server.enqueue(walletAnswer(purchased = 4))
        source.refresh()
        account.value = "uid-2"
        server.enqueue(walletAnswer(purchased = 9).setHeadersDelay(SLOW_ANSWER_MILLIS, TimeUnit.MILLISECONDS))

        val first = withTimeoutOrNull(FAST_WAIT_MILLIS) { gateway.observeEntitlement().first() }

        assertThat(first).isEqualTo(NO_CREDITS)
    }

    @Test
    fun anAnswerFetchedForAnotherAccountIsNotStored() = runBlocking<Unit> {
        val held = HeldAnswer(walletAnswer(purchased = 4)).also { server.dispatcher = it }
        val refresh = async(Dispatchers.Default) { runCatching { source.refresh() } }
        held.awaitRequest()

        account.value = "uid-2"
        held.release()

        assertThat(refresh.await().isFailure).isTrue()
        assertThat(source.cached).isNull()
    }

    private companion object {
        const val FAST_WAIT_MILLIS = 700L
        const val SLOW_ANSWER_MILLIS = 3_000L
    }
}
