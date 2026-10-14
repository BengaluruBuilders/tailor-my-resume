package com.tailormyresume.app.billing

import com.google.common.truth.Truth.assertThat
import com.tailormyresume.core.network.TailorMyResumeApiConfig
import com.tailormyresume.core.network.tailormyresumeApi
import com.tailormyresume.core.network.tailormyresumeJson
import com.tailormyresume.core.network.tailormyresumeOkHttpClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withContext
import okhttp3.mockwebserver.Dispatcher
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okhttp3.mockwebserver.RecordedRequest
import org.junit.After
import org.junit.Test
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

internal class HeldAnswer(private val response: MockResponse) : Dispatcher() {
    private val arrived = CountDownLatch(1)
    private val released = CountDownLatch(1)

    override fun dispatch(request: RecordedRequest): MockResponse {
        arrived.countDown()
        released.await(HOLD_SECONDS, TimeUnit.SECONDS)
        return response
    }

    suspend fun awaitRequest() = withContext(Dispatchers.Default) { arrived.await(HOLD_SECONDS, TimeUnit.SECONDS) }

    fun release() = released.countDown()

    private companion object {
        const val HOLD_SECONDS = 5L
    }
}

class WalletSourceTest {
    private val server = MockWebServer().apply { start() }
    private val api = tailormyresumeApi(TailorMyResumeApiConfig(server.url("/").toString()), tailormyresumeOkHttpClient(FixedToken), tailormyresumeJson())
    private val source = WalletSource(api)

    @After
    fun tearDown() {
        server.shutdown()
    }

    private fun walletAnswer(purchased: Int) = MockResponse().setResponseCode(200).setBody("""{"wallet":${walletJson(purchased = purchased)}}""")

    private fun holdAnswer(purchased: Int) = HeldAnswer(walletAnswer(purchased)).also { server.dispatcher = it }

    @Test
    fun aRefreshAnsweredAfterClearIsNotStored() = runTest {
        val held = holdAnswer(purchased = 4)
        val refresh = async(Dispatchers.Default) { runCatching { source.refresh() } }
        held.awaitRequest()

        source.clear()
        held.release()
        val outcome = refresh.await()

        assertThat(outcome.isFailure).isTrue()
        assertThat(source.cached).isNull()
    }

    @Test
    fun aStaleRefreshOrCachedReturnsTheClearedCache() = runTest {
        val held = holdAnswer(purchased = 4)
        val refresh = async(Dispatchers.Default) { source.refreshOrCached() }
        held.awaitRequest()

        source.clear()
        held.release()

        assertThat(refresh.await()).isNull()
        assertThat(source.cached).isNull()
    }

    @Test
    fun aRefreshStartedAfterClearIsStored() = runTest {
        source.clear()
        server.enqueue(walletAnswer(purchased = 2))

        val wallet = source.refresh()

        assertThat(wallet.purchasedCredits).isEqualTo(2)
        assertThat(source.cached).isEqualTo(wallet)
    }

    @Test
    fun anUpdateStartedBeforeClearIsDroppedAndOneStartedAfterIsStored() = runTest {
        server.enqueue(walletAnswer(purchased = 3))
        val wallet = source.refresh()
        val before = source.generation()
        source.clear()

        source.update(wallet, before)
        assertThat(source.cached).isNull()

        source.update(wallet, source.generation())
        assertThat(source.cached).isEqualTo(wallet)
    }
}
