package com.tailormyresume.core.data.mock

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.currentTime
import kotlinx.coroutines.test.runTest
import org.junit.Test

class MockLatencyTest {

    @Test
    fun noLatencyTakesNoTime() = runTest {
        NoMockLatency.await(MockOperation.SIGN_IN)

        assertThat(currentTime).isEqualTo(0)
    }

    @Test
    fun theDefaultLatencyWaitsForEveryOperation() = runTest {
        val latency = DelayMockLatency()

        MockOperation.entries.forEach { operation ->
            val before = currentTime
            latency.await(operation)
            assertThat(currentTime - before).isEqualTo(latency.delayMillis(operation))
            assertThat(latency.delayMillis(operation)).isGreaterThan(0)
        }
    }
}
