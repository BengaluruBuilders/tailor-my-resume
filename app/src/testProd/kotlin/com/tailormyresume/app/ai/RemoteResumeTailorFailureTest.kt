package com.tailormyresume.app.ai

import com.google.common.truth.Truth.assertThat
import com.tailormyresume.core.domain.AiException
import com.tailormyresume.core.domain.AiFailure
import com.tailormyresume.core.model.GapAnalysis
import com.tailormyresume.core.model.KeywordCoverage
import com.tailormyresume.core.testing.mock.TestMockStateStore
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Test

class RemoteResumeTailorFailureTest {
    private val backend = FakeBackend()
    private val store = TestMockStateStore()
    private val gap = GapAnalysis(listOf(matchOf(evidence = arrayOf(FACT_ID))), KeywordCoverage(1, 1))
    private val tailor = RemoteResumeTailor(backend.api, PendingTailoringIds(store, FixedIds))

    @After
    fun tearDown() = backend.shutdown()

    private suspend fun failureOfNextAttempt() =
        (runCatching { tailor.tailor(candidate, job, gap, "app-1", null, "run-1") }.exceptionOrNull() as AiException).failure

    @Test
    fun noCreditClearsRequestId() = runTest {
        backend.fail(402, "NO_CREDIT")

        assertThat(failureOfNextAttempt()).isEqualTo(AiFailure.NoCredit)
        assertThat(store.read("tailoring.request.run-1")).isNull()
        assertThat(backend.server.requestCount).isEqualTo(1)
    }

    @Test
    fun failedAnyCodeClearsRequestId() = runTest {
        listOf("INTERRUPTED", "AI_PROVIDER_ERROR", "A_CODE_FROM_THE_FUTURE").forEach { code ->
            backend.reply(202, tailoringBody("RUNNING"))
            backend.reply(200, tailoringBody("FAILED", ""","failureCode":"$code""""))

            assertThat(failureOfNextAttempt()).isEqualTo(AiFailure.Unavailable)
            assertThat(store.read("tailoring.request.run-1")).isNull()
        }
    }

    @Test
    fun failedJobNeverShowsOldBullets() = runTest {
        backend.reply(
            200,
            tailoringBody("FAILED", tailoringResult("Stale bullet.") + ""","failureCode":"INTERRUPTED""""),
        )

        assertThat(failureOfNextAttempt()).isEqualTo(AiFailure.Unavailable)
    }

    @Test
    fun unknownStatusIsTerminalNotPolledAndClearsRequestId() = runTest {
        backend.reply(202, tailoringBody("PAUSED"))

        assertThat(failureOfNextAttempt()).isEqualTo(AiFailure.Unavailable)
        assertThat(store.read("tailoring.request.run-1")).isNull()
        assertThat(backend.server.requestCount).isEqualTo(1)
    }

    @Test
    fun unknownStatusAfterPollingIsTerminalToo() = runTest {
        backend.reply(202, tailoringBody("RUNNING"))
        backend.reply(200, tailoringBody("PAUSED"))

        assertThat(failureOfNextAttempt()).isEqualTo(AiFailure.Unavailable)
        assertThat(store.read("tailoring.request.run-1")).isNull()
        assertThat(backend.server.requestCount).isEqualTo(2)
    }
}
