package com.tailormyresume.app.ai

import com.google.common.truth.Truth.assertThat
import com.tailormyresume.core.model.GapAnalysis
import com.tailormyresume.core.model.KeywordCoverage
import com.tailormyresume.core.testing.mock.TestMockStateStore
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Test

class RemoteResumeTailorRequestIdTest {
    private val backend = FakeBackend()
    private val store = TestMockStateStore()
    private val gap = GapAnalysis(listOf(matchOf(evidence = arrayOf(FACT_ID))), KeywordCoverage(1, 1))
    private val tailor = RemoteResumeTailor(backend.api, PendingTailoringIds(store, FixedIds))

    @After
    fun tearDown() = backend.shutdown()

    private fun requestIdOf(body: String) = Regex(""""requestId":"([^"]+)"""").find(body)?.groupValues.orEmpty().last()

    @Test
    fun retrySendsSameRequestIdWithoutSection() = runTest {
        backend.reply(202, tailoringBody("RUNNING"))
        repeat(100) { backend.reply(200, tailoringBody("RUNNING")) }
        runCatching { tailor.tailor(candidate, job, gap, "app-1", null) }
        val keyAfterGiveUp = store.read("tailoring.request.app-1")
        val firstBody = backend.server.takeRequest().body.readUtf8()

        assertThat(keyAfterGiveUp).isEqualTo(requestIdOf(firstBody))
        assertThat(firstBody).doesNotContain("section")
    }

    @Test
    fun aRetryAfterANetworkDropSendsTheSameRequestId() = runTest {
        backend.reply(502, """{"error":{"code":"AI_PROVIDER_ERROR","message":"x"}}""")
        runCatching { tailor.tailor(candidate, job, gap, "app-1", null) }
        backend.reply(202, tailoringBody("SUCCEEDED", tailoringResult(FACT_TEXT)))

        tailor.tailor(candidate, job, gap, "app-1", null)

        val first = backend.server.takeRequest().body.readUtf8()
        val second = backend.server.takeRequest().body.readUtf8()
        assertThat(requestIdOf(second)).isEqualTo(requestIdOf(first))
        assertThat(second).doesNotContain("section")
    }
}
