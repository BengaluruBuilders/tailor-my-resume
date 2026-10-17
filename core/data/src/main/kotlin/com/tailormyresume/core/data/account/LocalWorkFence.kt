package com.tailormyresume.core.data.account

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.concurrent.atomic.AtomicLong
import kotlin.coroutines.AbstractCoroutineContextElement
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.coroutineContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LocalWorkFence @Inject constructor() {
    private val generation = AtomicLong()
    private val mutations = Mutex()
    private val dirty = mutableSetOf<Any>()
    @Volatile private var frozen = false

    fun ticket(): Long {
        return generation.get()
    }

    fun check(ticket: Long) {
        if (frozen || ticket != generation.get()) throw CancellationException("Account work changed")
    }

    @Synchronized
    fun editing(owner: Any, hasUnsavedInput: Boolean) {
        if (hasUnsavedInput) dirty.add(owner) else dirty.remove(owner)
    }

    suspend fun freeze() = mutations.withLock {
        synchronized(this) {
            kotlin.check(dirty.isEmpty()) { "Save your edits before changing accounts" }
            frozen = true
        }
    }

    suspend fun freezeForErasure() = mutations.withLock {
        frozen = true
        generation.incrementAndGet()
    }

    fun context(ticket: Long): CoroutineContext = WorkTicket(this, ticket)

    private class WorkTicket(val fence: LocalWorkFence, val generation: Long) : AbstractCoroutineContextElement(Key) {
        companion object Key : CoroutineContext.Key<WorkTicket>
    }

    suspend fun <T> mutate(action: suspend () -> T): T = mutations.withLock {
        val ticket = coroutineContext[WorkTicket]?.takeIf { it.fence === this }?.generation ?: ticket()
        check(ticket)
        action().also { check(ticket) }
    }

    fun release() { frozen = false }

    fun invalidate() { generation.incrementAndGet() }
}
