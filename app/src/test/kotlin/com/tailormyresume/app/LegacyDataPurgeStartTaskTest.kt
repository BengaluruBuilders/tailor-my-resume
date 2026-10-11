package com.tailormyresume.app

import com.google.common.truth.Truth.assertThat
import com.tailormyresume.core.data.mock.LegacyDataPurge
import com.tailormyresume.core.data.mock.MockStateStore
import com.tailormyresume.core.testing.mock.TestMockStateStore
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Test

class LegacyDataPurgeStartTaskTest {

    private class FailingStore : MockStateStore by TestMockStateStore() {
        override suspend fun removeWithPrefix(prefix: String) = throw IllegalStateException("disk full")
    }

    @Test
    fun startRemovesLegacyKeys() = runTest(UnconfinedTestDispatcher()) {
        val store = TestMockStateStore()
        store.write("coverletter.x", "letter")
        store.write("profile.facts", "kept")

        LegacyDataPurgeStartTask(LegacyDataPurge(store), TestScope(testScheduler)).start()
        testScheduler.advanceUntilIdle()

        assertThat(store.read("coverletter.x")).isNull()
        assertThat(store.read("profile.facts")).isEqualTo("kept")
    }

    @Test
    fun startDoesNotPropagateStoreFailure() = runTest(UnconfinedTestDispatcher()) {
        val scope = TestScope(testScheduler)

        LegacyDataPurgeStartTask(LegacyDataPurge(FailingStore()), scope).start()
        scope.testScheduler.advanceUntilIdle()

        assertThat(scope.coroutineContext[kotlinx.coroutines.Job]?.isActive).isTrue()
    }
}
