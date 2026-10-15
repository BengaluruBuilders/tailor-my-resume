package com.tailormyresume.app.auth

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Test

class RemoteAccountClosedProbeTest {
    private val server = MockWebServer().apply { start() }

    @After
    fun tearDown() = runCatching { server.shutdown() }.let { }

    @Test
    fun isClosedMapsAccountDeletedToTrue() = runTest {
        server.enqueue(errorResponse(409, "ACCOUNT_DELETED"))

        val result = RemoteServerAccountDeleter(server.api()).isClosed()

        assertThat(result.getOrNull()).isTrue()
        val request = server.takeRequest()
        assertThat(request.method).isEqualTo("GET")
        assertThat(request.path).isEqualTo("/v1/me")
    }

    @Test
    fun isClosedFalseWhenAccountOpen() = runTest {
        server.enqueue(jsonResponse(200, ME_BODY))

        assertThat(RemoteServerAccountDeleter(server.api()).isClosed().getOrNull()).isFalse()
    }

    @Test
    fun isClosedFailsOnAServerError() = runTest {
        server.enqueue(errorResponse(500, "INTERNAL_ERROR"))

        assertThat(RemoteServerAccountDeleter(server.api()).isClosed().isFailure).isTrue()
    }

    @Test
    fun isClosedFailsWhenOffline() = runTest {
        val deleter = RemoteServerAccountDeleter(server.api())
        server.shutdown()

        assertThat(deleter.isClosed().isFailure).isTrue()
    }
}
