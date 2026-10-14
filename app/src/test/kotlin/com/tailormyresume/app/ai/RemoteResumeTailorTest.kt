package com.tailormyresume.app.ai

import com.google.common.truth.Truth.assertThat
import com.tailormyresume.core.domain.AiException
import com.tailormyresume.core.domain.AiFailure
import com.tailormyresume.core.domain.FabricationGuard
import com.tailormyresume.core.domain.TailorResumeUseCase
import com.tailormyresume.core.model.EvidenceBullet
import com.tailormyresume.core.model.GapAnalysis
import com.tailormyresume.core.model.GuardrailViolation
import com.tailormyresume.core.model.KeywordCoverage
import com.tailormyresume.core.testing.mock.TestMockStateStore
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.currentTime
import kotlinx.coroutines.test.runTest
import okhttp3.mockwebserver.MockResponse
import org.junit.After
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class RemoteResumeTailorTest {
    private val backend = FakeBackend()
    private val store = TestMockStateStore()
    private val gap = GapAnalysis(listOf(matchOf(evidence = arrayOf(FACT_ID))), KeywordCoverage(1, 1))
    private val tailor = RemoteResumeTailor(backend.api, PendingTailoringIds(store, FixedIds))

    @After
    fun tearDown() = backend.shutdown()

    private fun requestIdOf(body: String) = Regex(""""requestId":"([^"]+)"""").find(body)?.groupValues.orEmpty().last()

    @Test
    fun startsAJobThenPollsWithABackoffAndMapsTheResult() = runTest {
        backend.reply(202, tailoringBody("RUNNING"))
        backend.reply(200, tailoringBody("RUNNING"))
        backend.reply(200, tailoringBody("SUCCEEDED", tailoringResult("Cleaned and checked weekly sales data in Excel.")))

        val resume = tailor.tailor(candidate, job, gap, "app-1", null, "run-1")

        val start = backend.server.takeRequest()
        assertThat(start.method).isEqualTo("POST")
        assertThat(start.path).isEqualTo("/v1/tailormyresume/tailorings")
        assertThat(start.body.readUtf8()).contains(""""applicationId":"app-1"""")
        assertThat(backend.server.takeRequest().path).isEqualTo("/v1/tailormyresume/tailorings/tl_1")
        assertThat(currentTime).isEqualTo(5_000L)
        val bullet = resume.bullets.single()
        assertThat(bullet.originalText).isEqualTo(FACT_TEXT)
        assertThat(bullet.proposedText).isEqualTo("Cleaned and checked weekly sales data in Excel.")
        assertThat(bullet.generationId).isEqualTo("g-tailor")
        assertThat(bullet.violations).isEmpty()
    }

    @Test
    fun aStartThatAnswers200WithTheRunningJobKeepsPolling() = runTest {
        backend.reply(200, tailoringBody("RUNNING"))
        backend.reply(200, tailoringBody("SUCCEEDED", tailoringResult("Cleaned and checked weekly sales data in Excel.")))

        val resume = tailor.tailor(candidate, job, gap, "app-1", null, "run-1")

        assertThat(backend.server.takeRequest().method).isEqualTo("POST")
        assertThat(backend.server.takeRequest().path).isEqualTo("/v1/tailormyresume/tailorings/tl_1")
        assertThat(resume.bullets.single().proposedText).isEqualTo("Cleaned and checked weekly sales data in Excel.")
    }

    @Test
    fun aTailoringRequestNeverSendsASection() = runTest {
        backend.reply(202, tailoringBody("SUCCEEDED", tailoringResult(FACT_TEXT)))

        tailor.tailor(candidate, job, gap, "app-1", null, "run-1")

        assertThat(backend.server.takeRequest().body.readUtf8()).doesNotContain("section")
    }

    @Test
    fun aRetryAfterATransientFailureReusesTheRequestId() = runTest {
        backend.reply(202, tailoringBody("RUNNING"))
        backend.fail(502, "AI_PROVIDER_ERROR")
        val first = runCatching { tailor.tailor(candidate, job, gap, "app-1", null, "run-1") }.exceptionOrNull()
        backend.reply(200, tailoringBody("SUCCEEDED", tailoringResult(FACT_TEXT)))

        tailor.tailor(candidate, job, gap, "app-1", null, "run-1")

        assertThat((first as AiException).failure).isEqualTo(AiFailure.Unavailable)
        val firstId = requestIdOf(backend.server.takeRequest().body.readUtf8())
        backend.server.takeRequest()
        assertThat(requestIdOf(backend.server.takeRequest().body.readUtf8())).isEqualTo(firstId)
    }

    @Test
    fun aRateLimitedStartKeepsItsRequestIdForTheNextTry() = runTest {
        repeat(3) {
            backend.server.enqueue(
                MockResponse().setResponseCode(429).setHeader("Retry-After", "0")
                    .setBody("""{"error":{"code":"RATE_LIMITED","message":"x"}}"""),
            )
        }
        val first = runCatching { tailor.tailor(candidate, job, gap, "app-1", null, "run-1") }.exceptionOrNull()
        backend.reply(202, tailoringBody("SUCCEEDED", tailoringResult(FACT_TEXT)))

        tailor.tailor(candidate, job, gap, "app-1", null, "run-1")

        assertThat((first as AiException).failure).isEqualTo(AiFailure.RateLimited)
        val ids = List(4) { requestIdOf(backend.server.takeRequest().body.readUtf8()) }
        assertThat(ids.distinct()).hasSize(1)
    }

    @Test
    fun aSucceededJobKeepsItsRequestIdForTheSameRun() = runTest {
        backend.reply(202, tailoringBody("SUCCEEDED", tailoringResult(FACT_TEXT)))
        backend.reply(202, tailoringBody("SUCCEEDED", tailoringResult(FACT_TEXT)))

        tailor.tailor(candidate, job, gap, "app-1", null, "run-1")
        tailor.tailor(candidate, job, gap, "app-1", null, "run-1")

        val ids = List(2) { requestIdOf(backend.server.takeRequest().body.readUtf8()) }
        assertThat(ids.distinct()).hasSize(1)
    }

    @Test
    fun aFailedJobIsUnavailableAndForgetsItsRequestId() = runTest {
        backend.reply(202, tailoringBody("RUNNING"))
        backend.reply(200, tailoringBody("FAILED", ""","failureCode":"INTERRUPTED""""))
        backend.reply(202, tailoringBody("SUCCEEDED", tailoringResult(FACT_TEXT)))

        val failure = runCatching { tailor.tailor(candidate, job, gap, "app-1", null, "run-1") }.exceptionOrNull()
        tailor.tailor(candidate, job, gap, "app-1", null, "run-1")

        assertThat((failure as AiException).failure).isEqualTo(AiFailure.Unavailable)
        val ids = listOf(
            requestIdOf(backend.server.takeRequest().body.readUtf8()),
            backend.server.takeRequest().path,
            requestIdOf(backend.server.takeRequest().body.readUtf8()),
        )
        assertThat(ids[0]).isNotEqualTo(ids[2])
    }

    @Test
    fun aJobThatEndsOnTheCoreQuotaIsQuotaExceededAndForgetsItsRequestId() = runTest {
        backend.reply(200, tailoringBody("FAILED", ""","failureCode":"QUOTA_EXCEEDED""""))
        backend.reply(202, tailoringBody("SUCCEEDED", tailoringResult(FACT_TEXT)))

        val failure = runCatching { tailor.tailor(candidate, job, gap, "app-1", null, "run-1") }.exceptionOrNull()
        tailor.tailor(candidate, job, gap, "app-1", null, "run-1")

        assertThat((failure as AiException).failure).isEqualTo(AiFailure.QuotaExceeded)
        val ids = List(2) { requestIdOf(backend.server.takeRequest().body.readUtf8()) }
        assertThat(ids.distinct()).hasSize(2)
    }

    @Test
    fun aJobThatEndsOnTheBudgetIsQuotaExceeded() = runTest {
        backend.reply(200, tailoringBody("FAILED", ""","failureCode":"BUDGET_EXCEEDED""""))

        val failure = runCatching { tailor.tailor(candidate, job, gap, "app-1", null, "run-1") }.exceptionOrNull()

        assertThat((failure as AiException).failure).isEqualTo(AiFailure.QuotaExceeded)
    }

    @Test
    fun noCreditEndsTheAttemptAndForgetsItsRequestId() = runTest {
        backend.fail(402, "NO_CREDIT")

        val failure = runCatching { tailor.tailor(candidate, job, gap, "app-1", null, "run-1") }.exceptionOrNull()

        assertThat((failure as AiException).failure).isEqualTo(AiFailure.NoCredit)
        assertThat(store.read("tailoring.request.run-1")).isNull()
    }

    @Test
    fun aBusyServerIsRetriedAfterItsRetryAfter() = runTest {
        backend.server.enqueue(
            MockResponse().setResponseCode(429).setHeader("Retry-After", "10")
                .setBody("""{"error":{"code":"RATE_LIMITED","message":"x"}}"""),
        )
        backend.reply(202, tailoringBody("SUCCEEDED", tailoringResult(FACT_TEXT)))

        tailor.tailor(candidate, job, gap, "app-1", null, "run-1")

        assertThat(currentTime).isEqualTo(10_000L)
        assertThat(backend.server.requestCount).isEqualTo(2)
    }

    @Test
    fun aJobThatNeverFinishesTimesOutButKeepsItsRequestId() = runTest {
        backend.reply(202, tailoringBody("RUNNING"))
        repeat(100) { backend.reply(200, tailoringBody("RUNNING")) }

        val failure = runCatching { tailor.tailor(candidate, job, gap, "app-1", null, "run-1") }.exceptionOrNull()

        assertThat((failure as AiException).failure).isEqualTo(AiFailure.Timeout)
        assertThat(currentTime).isAtLeast(330_000L)
        assertThat(store.read("tailoring.request.run-1")).isNotNull()
    }

    @Test
    fun aBulletTheDeviceGuardRejectsFallsBackToTheOriginalLine() = runTest {
        backend.reply(202, tailoringBody("SUCCEEDED", tailoringResult("Cleaned sales data for 4000 stores.")))
        val guard = object : FabricationGuard {
            override fun check(proposedText: String, sources: List<EvidenceBullet>, profile: com.tailormyresume.core.model.CandidateProfile) =
                if ("4000" in proposedText) listOf(GuardrailViolation.UnsupportedNumber("4000")) else emptyList()
        }

        val resume = TailorResumeUseCase(tailor, guard)(candidate, job, gap, "app-1", "run-1")

        val bullet = resume.bullets.single()
        assertThat(bullet.proposedText).isEqualTo(FACT_TEXT)
        assertThat(bullet.editTypes).isEmpty()
        assertThat(bullet.violations).containsExactly(GuardrailViolation.UnsupportedNumber("4000"))
        assertThat(bullet.generationId).isEqualTo("g-tailor")
    }
}
