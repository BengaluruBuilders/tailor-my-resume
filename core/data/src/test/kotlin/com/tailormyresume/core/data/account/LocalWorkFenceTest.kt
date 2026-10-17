package com.tailormyresume.core.data.account

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.CancellationException
import org.junit.Test

class LocalWorkFenceTest {
    @Test fun unsavedInputPreventsTransitionUntilSaved() = kotlinx.coroutines.test.runTest {
        val fence = LocalWorkFence()
        val editor = Any()
        fence.editing(editor, true)
        assertThat(runCatching { fence.freeze() }.exceptionOrNull()).isInstanceOf(IllegalStateException::class.java)
        fence.editing(editor, false)
        fence.freeze()
        assertThat(runCatching { fence.check(fence.ticket()) }.exceptionOrNull()).isInstanceOf(CancellationException::class.java)
    }

    @Test fun sameUidLinkCanReleaseTheOriginalTicketButSwitchInvalidatesIt() = kotlinx.coroutines.test.runTest {
        val fence = LocalWorkFence()
        val ticket = fence.ticket()
        fence.freeze()
        fence.release()
        fence.check(ticket)
        fence.freeze()
        fence.invalidate()
        fence.release()
        assertThat(runCatching { fence.check(ticket) }.exceptionOrNull()).isInstanceOf(CancellationException::class.java)
        fence.check(fence.ticket())
    }
    @Test fun oldCoroutineCannotAcquireANewMutationTicketAfterSwitch() = kotlinx.coroutines.test.runTest {
        val fence = LocalWorkFence()
        val old = fence.ticket()
        fence.invalidate()
        var wrote = false
        val failure = runCatching {
            kotlinx.coroutines.withContext(fence.context(old)) { fence.mutate { wrote = true } }
        }.exceptionOrNull()
        assertThat(failure).isInstanceOf(CancellationException::class.java)
        assertThat(wrote).isFalse()
    }

    @Test fun exportLeaseBlocksSwitchAndAlwaysReleasesAfterFailure() = kotlinx.coroutines.test.runTest {
        val fence = LocalWorkFence()
        runCatching {
            fence.protect {
                assertThat(runCatching { fence.freeze() }.isFailure).isTrue()
                error("disk write failed")
            }
        }
        fence.freeze()
        fence.release()
    }

}
