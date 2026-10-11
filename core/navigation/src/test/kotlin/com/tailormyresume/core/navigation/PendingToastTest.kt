package com.tailormyresume.core.navigation

import com.google.common.truth.Truth.assertThat
import org.junit.After
import org.junit.Before
import org.junit.Test

class PendingToastTest {

    @Before
    @After
    fun clear() {
        PendingToast.consume()
    }

    @Test
    fun nothingQueuedByDefault() {
        assertThat(PendingToast.queued).isNull()
        assertThat(PendingToast.consume()).isNull()
    }

    @Test
    fun consumeReturnsTheQueuedMessageOnce() {
        PendingToast.set(7)

        assertThat(PendingToast.queued).isEqualTo(7)
        assertThat(PendingToast.consume()).isEqualTo(7)
        assertThat(PendingToast.consume()).isNull()
        assertThat(PendingToast.queued).isNull()
    }

    @Test
    fun theLatestMessageReplacesTheEarlierOne() {
        PendingToast.set(1)
        PendingToast.set(2)

        assertThat(PendingToast.consume()).isEqualTo(2)
    }
}
