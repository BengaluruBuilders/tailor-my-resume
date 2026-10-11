package com.tailormyresume.app.ai

import com.google.common.truth.Truth.assertThat
import com.google.common.truth.Truth.assertWithMessage
import com.tailormyresume.core.domain.AiException
import com.tailormyresume.core.domain.AiFailure
import com.tailormyresume.core.domain.GapMatcher
import com.tailormyresume.core.domain.JobAnalysisResult
import com.tailormyresume.core.domain.fact.FactIdAllocator
import com.tailormyresume.core.model.GapAnalysis
import com.tailormyresume.core.model.KeywordCoverage
import com.tailormyresume.core.network.ApiError
import com.tailormyresume.core.network.ApiException
import com.tailormyresume.core.testing.mock.TestMockStateStore
import kotlinx.coroutines.runBlocking
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.SocketPolicy
import org.junit.After
import org.junit.Test

class AiFailureMappingTest {
    private val backend = FakeBackend()
    private val analysis = JobAnalysisResult(job, GapAnalysis(listOf(matchOf(evidence = arrayOf(FACT_ID))), KeywordCoverage(1, 1)))

    @After
    fun tearDown() = backend.shutdown()

    private fun routes(): Map<String, suspend () -> Any> {
        val matcher = object : GapMatcher {
            override fun match(profile: com.tailormyresume.core.model.CandidateProfile, job: com.tailormyresume.core.model.JobDescription) =
                analysis.gap
        }
        val tailor = RemoteResumeTailor(backend.api, PendingTailoringIds(TestMockStateStore(), FixedIds))
        return mapOf(
            "parse" to { RemoteResumeTextParser(backend.api, FactIdAllocator()).parse("x".repeat(60)) },
            "analyse" to { RemoteJobAnalysisSource(backend.api, matcher).analyse(candidate, "jd text") },
            "tailor" to { tailor.tailor(candidate, job, analysis.gap, "app-1", null, "run-1") },
        )
    }

    private fun failureOf(route: suspend () -> Any): AiFailure? =
        runBlocking { runCatching { route() }.exceptionOrNull() }.let { (it as? AiException)?.failure }

    @Test
    fun everyRouteMapsEveryErrorCode() {
        val expected = mapOf(
            (402 to "NO_CREDIT") to AiFailure.NoCredit,
            (429 to "ALLOWANCE_EXHAUSTED") to AiFailure.AllowanceExhausted,
            (400 to "INVALID_INPUT") to AiFailure.InvalidInput,
            (413 to "PAYLOAD_TOO_LARGE") to AiFailure.InvalidInput,
            (409 to "ACCOUNT_DELETED") to AiFailure.AccountDeleted,
            (409 to "ANALYSIS_IN_PROGRESS") to AiFailure.AnalysisInProgress,
            (502 to "AI_PROVIDER_ERROR") to AiFailure.Unavailable,
            (500 to "INTERNAL_ERROR") to AiFailure.Unavailable,
            (429 to "QUOTA_EXCEEDED") to AiFailure.QuotaExceeded,
            (404 to "NOT_FOUND") to AiFailure.Unavailable,
        )
        routes().forEach { (name, route) ->
            expected.forEach { (response, failure) ->
                backend.fail(response.first, response.second)
                assertWithMessage("$name ${response.second}").that(failureOf(route)).isEqualTo(failure)
            }
        }
    }

    @Test
    fun everyContractErrorCodeMapsEndToEnd() {
        val contract = listOf(
            Triple(400, "INVALID_INPUT", AiFailure.InvalidInput),
            Triple(401, "UNAUTHENTICATED", AiFailure.SignInRequired),
            Triple(401, "INVALID_TOKEN", AiFailure.SignInRequired),
            Triple(403, "CROSS_APP_TOKEN", AiFailure.Unavailable),
            Triple(403, "FORBIDDEN", AiFailure.Unavailable),
            Triple(404, "NOT_FOUND", AiFailure.Unavailable),
            Triple(409, "ACCOUNT_DELETED", AiFailure.AccountDeleted),
            Triple(409, "ANALYSIS_IN_PROGRESS", AiFailure.AnalysisInProgress),
            Triple(413, "PAYLOAD_TOO_LARGE", AiFailure.InvalidInput),
            Triple(429, "RATE_LIMITED", AiFailure.RateLimited),
            Triple(429, "QUOTA_EXCEEDED", AiFailure.QuotaExceeded),
            Triple(429, "BUDGET_EXCEEDED", AiFailure.QuotaExceeded),
            Triple(502, "AI_PROVIDER_ERROR", AiFailure.Unavailable),
            Triple(405, "METHOD_NOT_ALLOWED", AiFailure.Unavailable),
            Triple(500, "HTTP_ERROR", AiFailure.Unavailable),
            Triple(500, "INTERNAL_ERROR", AiFailure.Unavailable),
            Triple(402, "NO_CREDIT", AiFailure.NoCredit),
            Triple(409, "PURCHASE_PENDING", AiFailure.Unavailable),
            Triple(400, "PURCHASE_INVALID", AiFailure.Unavailable),
            Triple(429, "ALLOWANCE_EXHAUSTED", AiFailure.AllowanceExhausted),
            Triple(502, "PLAY_UNAVAILABLE", AiFailure.Unavailable),
        )
        contract.forEach { (status, code, failure) ->
            routes().filterKeys { code != "RATE_LIMITED" || it != "tailor" }.forEach { (name, route) ->
                backend.fail(status, code)
                assertWithMessage("$name $status $code").that(failureOf(route)).isEqualTo(failure)
            }
        }
    }

    @Test
    fun rateLimitedKeepsRetryAfterAndIsNotUnavailable() {
        assertThat(ApiError.RateLimited(12).toAiFailure()).isEqualTo(AiFailure.RateLimited)
        val thrown = runCatching { Result.failure<Unit>(ApiException(ApiError.RateLimited(12))).orAiFailure() }
            .exceptionOrNull() as AiException
        assertThat(thrown.failure).isEqualTo(AiFailure.RateLimited)
        assertThat(thrown.retryAfterSeconds).isEqualTo(12)
    }

    @Test
    fun analysisInProgressIsItsOwnFailure() {
        assertThat(ApiError.AnalysisInProgress.toAiFailure()).isEqualTo(AiFailure.AnalysisInProgress)
    }

    @Test
    fun quotaAndBudgetMapToQuotaExceeded() {
        assertThat(ApiError.QuotaExceeded.toAiFailure()).isEqualTo(AiFailure.QuotaExceeded)
        assertThat(ApiError.BudgetExceeded.toAiFailure()).isEqualTo(AiFailure.QuotaExceeded)
    }

    @Test
    fun unauthenticatedAndInvalidTokenMapToSignInRequired() {
        assertThat(ApiError.Unauthenticated.toAiFailure()).isEqualTo(AiFailure.SignInRequired)
        assertThat(ApiError.InvalidToken.toAiFailure()).isEqualTo(AiFailure.SignInRequired)
    }

    @Test
    fun serverFaultsStayUnavailable() {
        listOf(ApiError.Unknown(503), ApiError.HttpError, ApiError.InternalError, ApiError.AiProviderError)
            .forEach { assertThat(it.toAiFailure()).isEqualTo(AiFailure.Unavailable) }
    }

    @Test
    fun unreachableServerIsNetwork() {
        val routes = routes()
        backend.shutdown()
        routes.forEach { (name, route) -> assertWithMessage(name).that(failureOf(route)).isEqualTo(AiFailure.Network) }
    }

    @Test
    fun silentServerIsTimeout() {
        routes().filterKeys { it != "tailor" }.forEach { (name, route) ->
            backend.server.enqueue(MockResponse().setSocketPolicy(SocketPolicy.NO_RESPONSE))
            assertWithMessage(name).that(failureOf(route)).isEqualTo(AiFailure.Timeout)
        }
    }

    @Test
    fun malformedBodyIsUnavailable() {
        routes().forEach { (name, route) ->
            backend.reply(200, "{not json")
            assertWithMessage(name).that(failureOf(route)).isEqualTo(AiFailure.Unavailable)
        }
    }
}
