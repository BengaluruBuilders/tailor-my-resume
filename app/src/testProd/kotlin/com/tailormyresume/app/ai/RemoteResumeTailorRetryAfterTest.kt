package com.tailormyresume.app.ai

import com.google.common.truth.Truth.assertThat
import com.tailormyresume.core.domain.AiException
import com.tailormyresume.core.domain.AiFailure
import com.tailormyresume.core.model.GapAnalysis
import com.tailormyresume.core.model.KeywordCoverage
import com.tailormyresume.core.testing.mock.TestMockStateStore
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.currentTime
import kotlinx.coroutines.test.runTest
import okhttp3.mockwebserver.MockResponse
import org.junit.After
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class RemoteResumeTailorRetryAfterTest {
    private val backend = FakeBackend()
    private val gap = GapAnalysis(listOf(matchOf(evidence = arrayOf(FACT_ID))), KeywordCoverage(1, 1))
    private val tailor = RemoteResumeTailor(backend.api, PendingTailoringIds(TestMockStateStore(), FixedIds))

    @After
    fun tearDown() = backend.shutdown()

    private fun plainRateLimit(retryAfter: String) {
        backend.server.enqueue(MockResponse().setResponseCode(429).setHeader("Retry-After", retryAfter))
        backend.reply(202, tailoringBody("SUCCEEDED", tailoringResult(FACT_TEXT)))
    }

    @Test
    fun aRetryAfterOfADaySurfacesRateLimitedAtOnce() = runTest {
        backend.server.enqueue(MockResponse().setResponseCode(429).setHeader("Retry-After", "86400"))

        val failure = runCatching { tailor.tailor(candidate, job, gap, "app-1", null, "run-1") }.exceptionOrNull()

        assertThat((failure as AiException).failure).isEqualTo(AiFailure.RateLimited)
        assertThat(currentTime).isEqualTo(0L)
        assertThat(backend.server.requestCount).isEqualTo(1)
    }

    @Test
    fun aRetryAfterOfExactlyAMinuteIsHonoured() = runTest {
        plainRateLimit("60")

        tailor.tailor(candidate, job, gap, "app-1", null, "run-1")

        assertThat(currentTime).isEqualTo(60_000L)
        assertThat(backend.server.requestCount).isEqualTo(2)
    }

    @Test
    fun aRetryAfterOfSixtyOneSecondsSurfacesRateLimitedWithoutSleeping() = runTest {
        backend.server.enqueue(MockResponse().setResponseCode(429).setHeader("Retry-After", "61"))

        val failure = runCatching { tailor.tailor(candidate, job, gap, "app-1", null, "run-1") }.exceptionOrNull()

        assertThat((failure as AiException).failure).isEqualTo(AiFailure.RateLimited)
        assertThat(currentTime).isEqualTo(0L)
        assertThat(backend.server.requestCount).isEqualTo(1)
    }

    @Test
    fun aRetryAfterOfZeroFallsBackToTheTenSecondDefault() = runTest {
        plainRateLimit("0")

        tailor.tailor(candidate, job, gap, "app-1", null, "run-1")

        assertThat(currentTime).isEqualTo(10_000L)
    }
}
