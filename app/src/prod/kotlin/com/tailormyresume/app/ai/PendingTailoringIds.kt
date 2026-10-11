package com.tailormyresume.app.ai

import com.tailormyresume.core.data.mock.MockStateStore
import com.tailormyresume.core.domain.IdGenerator
import javax.inject.Inject

class PendingTailoringIds @Inject constructor(
    private val store: MockStateStore,
    private val ids: IdGenerator,
) {
    suspend fun idFor(applicationId: String): String {
        val key = keyOf(applicationId)
        return store.read(key) ?: ids.newId().also { store.write(key, it) }
    }

    suspend fun clear(applicationId: String) = store.remove(keyOf(applicationId))

    private fun keyOf(applicationId: String) = "tailoring.request.$applicationId"
}
