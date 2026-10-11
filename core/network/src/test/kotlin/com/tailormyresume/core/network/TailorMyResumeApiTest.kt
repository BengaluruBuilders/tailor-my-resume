package com.tailormyresume.core.network

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.runBlocking
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okhttp3.mockwebserver.SocketPolicy
import org.junit.After
import org.junit.Test
import java.io.IOException
import java.io.InterruptedIOException
import java.net.SocketTimeoutException
import java.util.concurrent.TimeUnit

class TailorMyResumeApiTest {
    private val server = MockWebServer().apply { start() }
    private val tokens = RecordingTokens()
    private val json = tailormyresumeJson()

    @After
    fun tearDown() = runCatching { server.shutdown() }.let { }

    private var expirySignals = 0

    private fun api(readTimeoutMillis: Long? = null): TailorMyResumeApi {
        val base = tailormyresumeOkHttpClient(tokens, sessionListener = { expirySignals++ })
        val client = readTimeoutMillis
            ?.let { base.newBuilder().readTimeout(it, TimeUnit.MILLISECONDS).build() }
            ?: base
        return tailormyresumeApi(TailorMyResumeApiConfig(server.url("/").toString().trimEnd('/')), client, json)
    }

    private fun jsonResponse(code: Int, body: String) =
        MockResponse().setResponseCode(code).setHeader("Content-Type", "application/json").setBody(body)

    private fun error(code: Int, errorCode: String) =
        jsonResponse(code, """{"error":{"code":"$errorCode","message":"ignored"}}""")

    private fun failureOf(block: suspend () -> Any): ApiError {
        val exception = runBlocking { apiResult(block) }.exceptionOrNull()
        return (exception as ApiException).error
    }

    @Test
    fun everyRequestCarriesAppIdAndBearerToken() {
        server.enqueue(jsonResponse(200, """{"user":{"id":"u","createdAt":"t"}}"""))
        runBlocking { api().me() }
        val request = server.takeRequest()
        assertThat(request.path).isEqualTo("/v1/me")
        assertThat(request.getHeader("X-App-Id")).isEqualTo("tailormyresume")
        assertThat(request.getHeader("Authorization")).isEqualTo("Bearer token-0")
        assertThat(tokens.forceRefreshCalls).containsExactly(false)
    }

    @Test
    fun unauthorisedIsRetriedOnceWithAFreshToken() {
        server.enqueue(error(401, "INVALID_TOKEN"))
        server.enqueue(jsonResponse(200, """{"user":{"id":"u","createdAt":"t"}}"""))
        runBlocking { api().me() }
        assertThat(server.requestCount).isEqualTo(2)
        server.takeRequest()
        assertThat(server.takeRequest().getHeader("Authorization")).isEqualTo("Bearer token-1")
        assertThat(tokens.forceRefreshCalls).containsExactly(false, true).inOrder()
    }

    @Test
    fun secondUnauthorisedIsNotRetriedAgain() {
        server.enqueue(error(401, "INVALID_TOKEN"))
        server.enqueue(error(401, "INVALID_TOKEN"))
        assertThat(failureOf { api().me() }).isEqualTo(ApiError.InvalidToken)
        assertThat(server.requestCount).isEqualTo(2)
    }

    @Test
    fun secondUnauthorisedSignalsTheSessionExpiredListenerOnce() {
        server.enqueue(error(401, "INVALID_TOKEN"))
        server.enqueue(error(401, "INVALID_TOKEN"))
        assertThat(failureOf { api().me() }).isEqualTo(ApiError.InvalidToken)
        assertThat(expirySignals).isEqualTo(1)
    }

    @Test
    fun aSessionExpiredTokenSignalsTheListener() {
        tokens.expired = true
        assertThat(failureOf { api().me() }).isEqualTo(ApiError.Unauthenticated)
        assertThat(expirySignals).isEqualTo(1)
        assertThat(server.requestCount).isEqualTo(0)
    }

    @Test
    fun anExpiredSessionSendsNoRequestAndStillSignalsOnce() {
        tokens.expired = true
        assertThat(failureOf { api().me() }).isEqualTo(ApiError.Unauthenticated)
        assertThat(expirySignals).isEqualTo(1)
        assertThat(server.requestCount).isEqualTo(0)
    }

    @Test
    fun aTokenTimeoutIsATimeoutAndSendsNoRequest() {
        tokens.failure = SocketTimeoutException("token")
        assertThat(failureOf { api().me() }).isEqualTo(ApiError.Timeout)
        assertThat(server.requestCount).isEqualTo(0)
        assertThat(expirySignals).isEqualTo(0)
    }

    @Test
    fun aTransientTokenFailureIsOffline() {
        tokens.failure = IOException("token")
        assertThat(failureOf { api().me() }).isEqualTo(ApiError.Offline)
        assertThat(server.requestCount).isEqualTo(0)
        assertThat(expirySignals).isEqualTo(0)
    }

    @Test
    fun aTimeoutDuringTheForcedRefreshIsATimeoutWithoutASecondRequest() {
        server.enqueue(error(401, "INVALID_TOKEN"))
        tokens.failureOnRefresh = SocketTimeoutException("token")
        assertThat(failureOf { api().me() }).isEqualTo(ApiError.Timeout)
        assertThat(server.requestCount).isEqualTo(1)
        assertThat(expirySignals).isEqualTo(0)
    }

    @Test
    fun aMissingTokenDoesNotSignalTheListener() {
        tokens.available = false
        server.enqueue(error(401, "UNAUTHENTICATED"))
        failureOf { api().me() }
        assertThat(expirySignals).isEqualTo(0)
    }

    @Test
    fun aRefreshedTokenThatSucceedsDoesNotSignalTheListener() {
        server.enqueue(error(401, "INVALID_TOKEN"))
        server.enqueue(jsonResponse(200, """{"user":{"id":"u","createdAt":"t"}}"""))
        runBlocking { api().me() }
        assertThat(expirySignals).isEqualTo(0)
    }

    @Test
    fun noTokenMeansNoAuthorizationHeader() {
        tokens.available = false
        server.enqueue(error(401, "UNAUTHENTICATED"))
        assertThat(failureOf { api().me() }).isEqualTo(ApiError.Unauthenticated)
        assertThat(server.takeRequest().getHeader("Authorization")).isNull()
        assertThat(server.requestCount).isEqualTo(1)
    }

    @Test
    fun everyErrorCodeDecodes() {
        val expected = mapOf(
            400 to listOf(
                "INVALID_INPUT" to ApiError.InvalidInput,
                "APP_ID_INVALID" to ApiError.AppIdInvalid,
                "APP_NOT_FOUND" to ApiError.AppNotFound,
                "PURCHASE_INVALID" to ApiError.PurchaseInvalid,
            ),
            401 to listOf("UNAUTHENTICATED" to ApiError.Unauthenticated, "INVALID_TOKEN" to ApiError.InvalidToken),
            402 to listOf("NO_CREDIT" to ApiError.NoCredit),
            403 to listOf(
                "CROSS_APP_TOKEN" to ApiError.CrossAppToken,
                "FORBIDDEN" to ApiError.Forbidden,
            ),
            404 to listOf("NOT_FOUND" to ApiError.NotFound),
            405 to listOf("METHOD_NOT_ALLOWED" to ApiError.MethodNotAllowed),
            409 to listOf(
                "ACCOUNT_DELETED" to ApiError.AccountDeleted,
                "PURCHASE_PENDING" to ApiError.PurchasePending,
                "ANALYSIS_IN_PROGRESS" to ApiError.AnalysisInProgress,
            ),
            413 to listOf("PAYLOAD_TOO_LARGE" to ApiError.PayloadTooLarge),
            429 to listOf(
                "QUOTA_EXCEEDED" to ApiError.QuotaExceeded,
                "BUDGET_EXCEEDED" to ApiError.BudgetExceeded,
                "ALLOWANCE_EXHAUSTED" to ApiError.AllowanceExhausted,
            ),
            500 to listOf("HTTP_ERROR" to ApiError.HttpError, "INTERNAL_ERROR" to ApiError.InternalError),
            502 to listOf("AI_PROVIDER_ERROR" to ApiError.AiProviderError, "PLAY_UNAVAILABLE" to ApiError.PlayUnavailable),
        )
        val api = api()
        tokens.available = false
        expected.forEach { (status, codes) ->
            codes.forEach { (code, error) ->
                server.enqueue(error(status, code))
                assertThat(failureOf { api.wallet() }).isEqualTo(error)
            }
        }
    }

    @Test
    fun statusFallbackMapsPaymentRequiredAndRateLimitedWithoutAnEnvelope() {
        server.enqueue(jsonResponse(402, "<html>pay</html>"))
        server.enqueue(jsonResponse(429, "<html>slow</html>").setHeader("Retry-After", "7"))
        server.enqueue(jsonResponse(429, "").setHeader("Retry-After", "7"))
        server.enqueue(error(429, "TEAPOT").setHeader("Retry-After", "9"))
        server.enqueue(jsonResponse(401, ""))
        val api = api()
        tokens.available = false

        assertThat(failureOf { api.wallet() }).isEqualTo(ApiError.NoCredit)
        assertThat(failureOf { api.wallet() }).isEqualTo(ApiError.RateLimited(7))
        assertThat(failureOf { api.wallet() }).isEqualTo(ApiError.RateLimited(7))
        assertThat(failureOf { api.wallet() }).isEqualTo(ApiError.RateLimited(9))
        assertThat(failureOf { api.wallet() }).isEqualTo(ApiError.Unauthenticated)
    }

    @Test
    fun aKnownCodeWinsOverTheStatusFallback() {
        server.enqueue(error(429, "ALLOWANCE_EXHAUSTED"))
        server.enqueue(error(402, "FORBIDDEN"))
        server.enqueue(error(401, "INVALID_TOKEN"))
        val api = api()
        tokens.available = false

        assertThat(failureOf { api.wallet() }).isEqualTo(ApiError.AllowanceExhausted)
        assertThat(failureOf { api.wallet() }).isEqualTo(ApiError.Forbidden)
        assertThat(failureOf { api.wallet() }).isEqualTo(ApiError.InvalidToken)
    }

    @Test
    fun serverFailuresWithoutAnEnvelopeStayUnknown() {
        server.enqueue(jsonResponse(500, "<html>boom</html>"))
        server.enqueue(jsonResponse(502, ""))
        val api = api()

        assertThat(failureOf { api.wallet() }).isEqualTo(ApiError.Unknown(500))
        assertThat(failureOf { api.wallet() }).isEqualTo(ApiError.Unknown(502))
    }

    @Test
    fun rateLimitedKeepsRetryAfter() {
        server.enqueue(error(429, "RATE_LIMITED").setHeader("Retry-After", "10"))
        assertThat(failureOf { api().wallet() }).isEqualTo(ApiError.RateLimited(10))
    }

    @Test
    fun unknownCodeAndEmptyBodyKeepTheStatus() {
        server.enqueue(error(418, "TEAPOT"))
        server.enqueue(jsonResponse(503, ""))
        val api = api()
        assertThat(failureOf { api.wallet() }).isEqualTo(ApiError.Unknown(418))
        assertThat(failureOf { api.wallet() }).isEqualTo(ApiError.Unknown(503))
    }

    @Test
    fun slowServerIsATimeout() {
        server.enqueue(MockResponse().setSocketPolicy(SocketPolicy.NO_RESPONSE))
        assertThat(failureOf { api(readTimeoutMillis = 200).wallet() }).isEqualTo(ApiError.Timeout)
    }

    @Test
    fun sessionExpiryDuringTheForcedRefreshSignalsOnceAndKeepsTheFirstAnswer() {
        server.enqueue(error(401, "INVALID_TOKEN"))
        tokens.expiredOnRefresh = true

        assertThat(failureOf { api().me() }).isEqualTo(ApiError.InvalidToken)
        assertThat(expirySignals).isEqualTo(1)
        assertThat(server.requestCount).isEqualTo(1)
    }

    @Test
    fun aCallTimeoutIsATimeout() {
        server.enqueue(MockResponse().setSocketPolicy(SocketPolicy.NO_RESPONSE))
        val client = tailormyresumeOkHttpClient(tokens).newBuilder().callTimeout(200, TimeUnit.MILLISECONDS).build()
        val api = tailormyresumeApi(TailorMyResumeApiConfig(server.url("/").toString().trimEnd('/')), client, json)

        assertThat(failureOf { api.wallet() }).isEqualTo(ApiError.Timeout)
    }

    @Test
    fun anInterruptedIoExceptionIsATimeout() {
        assertThat(failureOf { throw InterruptedIOException("timeout") }).isEqualTo(ApiError.Timeout)
    }

    @Test
    fun anUnexpectedExceptionBecomesAnApiErrorInsteadOfEscaping() {
        assertThat(failureOf { throw IllegalStateException("boom") }).isEqualTo(ApiError.Unknown(0))
        assertThat(failureOf { throw IllegalArgumentException("bad") }).isEqualTo(ApiError.Unknown(0))
    }

    @Test
    fun aCancellationStillPropagates() {
        val outcome = runCatching { runBlocking { apiResult { throw CancellationException("stop") } } }

        assertThat(outcome.exceptionOrNull()).isInstanceOf(CancellationException::class.java)
    }

    @Test
    fun unreachableServerIsOffline() {
        val api = api()
        server.shutdown()
        assertThat(failureOf { api.wallet() }).isEqualTo(ApiError.Offline)
    }

    @Test
    fun clientUsesTheContractTimeouts() {
        val client = tailormyresumeOkHttpClient(tokens)
        assertThat(client.readTimeoutMillis).isEqualTo(60_000)
        assertThat(client.connectTimeoutMillis).isEqualTo(15_000)
    }

    private class RecordingTokens : IdTokenProvider {
        val forceRefreshCalls = mutableListOf<Boolean>()
        var available = true
        var expired = false
        var expiredOnRefresh = false
        var failure: IOException? = null
        var failureOnRefresh: IOException? = null
        private var issued = 0

        override fun idToken(forceRefresh: Boolean): String? {
            forceRefreshCalls += forceRefresh
            if (expired || (forceRefresh && expiredOnRefresh)) throw SessionExpiredException()
            failure?.let { throw it }
            if (forceRefresh) failureOnRefresh?.let { throw it }
            return if (available) "token-${issued++}" else null
        }
    }
}
