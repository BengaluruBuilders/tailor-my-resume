package com.tailormyresume.app.auth

import com.google.common.truth.Truth.assertThat
import com.tailormyresume.core.data.repository.PendingWipeState
import com.tailormyresume.core.domain.account.AccountWipeFinisher
import com.tailormyresume.core.domain.account.FinishPendingAccountWipeUseCase
import com.tailormyresume.core.domain.account.ServerAccountDeleter
import com.tailormyresume.core.testing.repository.TestPendingAccountWipe
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Test

class PendingWipeStartTaskTest {

    private val marker = TestPendingAccountWipe()
    private var wipes = 0

    private val deleter = object : ServerAccountDeleter {
        override val deletesRemoteData = true

        override suspend fun delete() = Result.success(Unit)

        override suspend fun isClosed() = Result.success(true)
    }

    private fun TestScope.task() = PendingWipeStartTask(
        FinishPendingAccountWipeUseCase(marker, AccountWipeFinisher { wipes++ }, deleter),
        TestScope(testScheduler),
    )

    @Test
    fun startFinishesPendingWipe() = runTest(UnconfinedTestDispatcher()) {
        marker.current = PendingWipeState.SERVER_CLOSED

        task().start()
        testScheduler.advanceUntilIdle()

        assertThat(wipes).isEqualTo(1)
        assertThat(marker.current).isEqualTo(PendingWipeState.NONE)
    }

    @Test
    fun startWithRequestedMarkerAndClosedServerWipes() = runTest(UnconfinedTestDispatcher()) {
        marker.current = PendingWipeState.REQUESTED

        task().start()
        testScheduler.advanceUntilIdle()

        assertThat(wipes).isEqualTo(1)
        assertThat(marker.current).isEqualTo(PendingWipeState.NONE)
    }

    @Test
    fun startWithNoMarkerDoesNothing() = runTest(UnconfinedTestDispatcher()) {
        task().start()
        testScheduler.advanceUntilIdle()

        assertThat(wipes).isEqualTo(0)
    }
}
