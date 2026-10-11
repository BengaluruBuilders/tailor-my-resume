package com.tailormyresume.core.domain

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

internal object DecisionWriteLock {
    private val mutex = Mutex()

    suspend fun <T> serialised(block: suspend () -> T): T = mutex.withLock { block() }
}
