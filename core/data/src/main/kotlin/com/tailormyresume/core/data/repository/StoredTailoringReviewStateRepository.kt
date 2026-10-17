package com.tailormyresume.core.data.repository

import com.tailormyresume.core.data.account.LocalWorkFence
import com.tailormyresume.core.data.mock.MockStateStore
import com.tailormyresume.core.model.TailoringReviewState
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
internal class StoredTailoringReviewStateRepository @Inject constructor(
    private val store: MockStateStore,
    private val workFence: LocalWorkFence = LocalWorkFence(),
) : TailoringReviewStateRepository {

    private val mutex = Mutex()

    override fun observe(applicationId: String): Flow<TailoringReviewState> =
        combine(store.observe(regenerationsKey(applicationId)), store.observe(editedKey(applicationId))) { used, edited ->
            TailoringReviewState(
                regenerationsBySection = decodeRegenerations(used),
                editedBulletIds = decodeIds(edited),
            )
        }.distinctUntilChanged()

    override suspend fun recordRegeneration(applicationId: String, section: String) = workFence.mutate {
        mutex.withLock {
            val key = regenerationsKey(applicationId)
            val current = decodeRegenerations(store.read(key))
            store.write(key, encodeRegenerations(current + (section to (current[section] ?: 0) + 1)))
        }
    }

    override suspend fun markEdited(applicationId: String, bulletId: String) = workFence.mutate {
        mutex.withLock {
            val key = editedKey(applicationId)
            store.write(key, encodeIds(decodeIds(store.read(key)) + bulletId))
        }
    }

    override suspend fun clearEdited(applicationId: String, bulletIds: Collection<String>) = workFence.mutate {
        mutex.withLock {
            val key = editedKey(applicationId)
            val left = decodeIds(store.read(key)) - bulletIds.toSet()
            if (left.isEmpty()) store.remove(key) else store.write(key, encodeIds(left))
        }
    }

    override suspend fun clearFor(applicationId: String) {
        mutex.withLock {
            store.remove(regenerationsKey(applicationId))
            store.remove(editedKey(applicationId))
        }
    }

    private fun regenerationsKey(applicationId: String) = "tailor.regenerations.$applicationId"

    private fun editedKey(applicationId: String) = "tailor.edited.$applicationId"

    private fun encodeIds(ids: Set<String>): String = ids.joinToString(SEPARATOR)

    private fun decodeIds(text: String?): Set<String> =
        text?.split(SEPARATOR)?.filter { it.isNotEmpty() }?.toSet().orEmpty()

    private fun encodeRegenerations(counts: Map<String, Int>): String =
        counts.entries.joinToString(SEPARATOR) { (section, count) -> "$section$PAIR$count" }

    private fun decodeRegenerations(text: String?): Map<String, Int> {
        val legacy = text?.toIntOrNull()
        if (legacy != null) return mapOf(LEGACY_SECTION to legacy)
        return decodeIds(text).mapNotNull { pair ->
            val count = pair.substringAfterLast(PAIR, "").toIntOrNull()
            count?.let { pair.substringBeforeLast(PAIR) to it }
        }.toMap()
    }

    private companion object {
        const val SEPARATOR = "|"
        const val PAIR = "="
        const val LEGACY_SECTION = ""
    }
}
