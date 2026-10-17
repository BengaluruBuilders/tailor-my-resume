package com.tailormyresume.core.data.account

import kotlinx.coroutines.CancellationException
import java.util.concurrent.atomic.AtomicLong
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LocalWorkFence @Inject constructor() {
    private val generation = AtomicLong()
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

    @Synchronized
    fun freeze() {
        kotlin.check(dirty.isEmpty()) { "Save your edits before changing accounts" }
        frozen = true
    }

    fun release() { frozen = false }

    fun invalidate() { generation.incrementAndGet() }
}
