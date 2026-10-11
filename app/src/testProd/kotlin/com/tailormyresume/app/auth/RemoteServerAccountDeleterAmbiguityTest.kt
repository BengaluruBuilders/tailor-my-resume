package com.tailormyresume.app.auth

import com.google.common.truth.Truth.assertThat
import com.tailormyresume.core.network.ApiError
import com.tailormyresume.core.network.ApiException
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Test

class RemoteServerAccountDeleterAmbiguityTest {
    private val server = MockWebServer().apply { start() }
    private val deleter = RemoteServerAccountDeleter(server.api())

    @After
    fun tearDown() = runCatching { server.shutdown() }.let { }

    private fun ambiguous(error: ApiError) = deleter.mayHaveReachedServer(ApiException(error))

    @Test
    fun classifiesTimeoutOfflineAnd5xxAsAmbiguous() {
        listOf(
            ApiError.Timeout,
            ApiError.Offline,
            ApiError.InternalError,
            ApiError.HttpError,
            ApiError.AiProviderError,
            ApiError.Unknown(500),
            ApiError.Unknown(503),
            ApiError.Unknown(0),
        ).forEach { error -> assertThat(ambiguous(error)).isTrue() }
    }

    @Test
    fun classifiesClientAndAuthFailuresAsUnambiguous() {
        listOf(
            ApiError.Unauthenticated,
            ApiError.InvalidToken,
            ApiError.Forbidden,
            ApiError.Unknown(404),
        ).forEach { error -> assertThat(ambiguous(error)).isFalse() }
        assertThat(deleter.mayHaveReachedServer(IllegalStateException("x"))).isFalse()
    }
}
