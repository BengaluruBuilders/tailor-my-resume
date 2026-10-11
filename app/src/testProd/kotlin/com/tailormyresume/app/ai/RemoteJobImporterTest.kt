package com.tailormyresume.app.ai

import com.google.common.truth.Truth.assertThat
import com.tailormyresume.core.domain.AiException
import com.tailormyresume.core.domain.AiFailure
import com.tailormyresume.core.network.IdTokenProvider
import com.tailormyresume.core.network.TailorMyResumeApiConfig
import com.tailormyresume.core.network.tailormyresumeApi
import com.tailormyresume.core.network.tailormyresumeJson
import com.tailormyresume.core.network.tailormyresumeOkHttpClient
import kotlinx.coroutines.runBlocking
import okhttp3.mockwebserver.MockResponse
import org.junit.After
import org.junit.Test

class RemoteJobImporterTest {
    private val backend = FakeBackend()
    private val importer = RemoteJobImporter(backend.api)

    @After
    fun tearDown() = backend.shutdown()

    private fun failureOf(url: String): AiException =
        runBlocking { runCatching { importer.import(url) }.exceptionOrNull() as AiException }

    @Test
    fun success() = runBlocking<Unit> {
        backend.reply(200, """{"jobText":"Associate Analyst ...","sourceHost":"careers.example.com"}""")

        val imported = importer.import("https://careers.example.com/jobs/associate-analyst")

        val request = backend.server.takeRequest()
        assertThat(request.method).isEqualTo("POST")
        assertThat(request.path).isEqualTo("/v1/tailormyresume/job-imports")
        assertThat(request.body.readUtf8()).isEqualTo("""{"url":"https://careers.example.com/jobs/associate-analyst"}""")
        assertThat(imported.jobText).isEqualTo("Associate Analyst ...")
        assertThat(imported.sourceHost).isEqualTo("careers.example.com")
    }

    @Test
    fun nonHttps() {
        listOf("http://careers.example.com/job", "ftp://x.example.com/job", "careers.example.com/job", "https://").forEach {
            assertThat(failureOf(it).failure).isEqualTo(AiFailure.InvalidInput)
        }
        assertThat(backend.server.requestCount).isEqualTo(0)
    }

    @Test
    fun tooLong() {
        val url = "https://careers.example.com/" + "a".repeat(2_048)

        assertThat(failureOf(url).failure).isEqualTo(AiFailure.InvalidInput)
        assertThat(backend.server.requestCount).isEqualTo(0)
    }

    @Test
    fun jobImportFailed() {
        backend.fail(422, "JOB_IMPORT_FAILED")

        assertThat(failureOf("https://careers.example.com/job").failure).isEqualTo(AiFailure.JobImportFailed)
    }

    @Test
    fun allowanceExhausted() {
        backend.fail(429, "ALLOWANCE_EXHAUSTED")

        assertThat(failureOf("https://careers.example.com/job").failure).isEqualTo(AiFailure.AllowanceExhausted)
    }

    @Test
    fun rateLimitedNoRetry() {
        backend.server.enqueue(
            MockResponse().setResponseCode(429).setHeader("Retry-After", "10")
                .setBody("""{"error":{"code":"RATE_LIMITED","message":"x"}}"""),
        )
        backend.reply(200, """{"jobText":"unused","sourceHost":"x"}""")

        val failure = failureOf("https://careers.example.com/job")

        assertThat(failure.failure).isEqualTo(AiFailure.RateLimited)
        assertThat(failure.retryAfterSeconds).isEqualTo(10)
        assertThat(backend.server.requestCount).isEqualTo(1)
    }

    @Test
    fun theBearerTokenGoesOnlyToTheBackendHost() = runBlocking<Unit> {
        val token = object : IdTokenProvider {
            override fun idToken(forceRefresh: Boolean): String = "token-123"
        }
        val api = tailormyresumeApi(
            TailorMyResumeApiConfig(backend.server.url("/").toString().trimEnd('/')),
            tailormyresumeOkHttpClient(token),
            tailormyresumeJson(),
        )
        backend.reply(200, """{"jobText":"t","sourceHost":"elsewhere.example.org"}""")

        RemoteJobImporter(api).import("https://elsewhere.example.org/job")

        val request = backend.server.takeRequest()
        assertThat(checkNotNull(request.requestUrl).host).isEqualTo(backend.server.hostName)
        assertThat(request.getHeader("Authorization")).isEqualTo("Bearer token-123")
        assertThat(request.getHeader("X-App-Id")).isEqualTo("tailormyresume")
        assertThat(backend.server.requestCount).isEqualTo(1)
    }
}
