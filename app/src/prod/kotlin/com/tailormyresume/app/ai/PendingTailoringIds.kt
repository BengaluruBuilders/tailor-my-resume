package com.tailormyresume.app.ai

import com.tailormyresume.core.data.mock.MockStateStore
import com.tailormyresume.core.domain.IdGenerator
import javax.inject.Inject

class PendingTailoringIds @Inject constructor(
    private val store: MockStateStore,
    private val ids: IdGenerator,
) {
    suspend fun idFor(runId: String, applicationId: String): String {
        val runKey = keyOf(runId)
        store.read(runKey)?.let { return it }
        val id = store.read(unfinishedKeyOf(applicationId)) ?: ids.newId().also { store.write(unfinishedKeyOf(applicationId), it) }
        store.write(runKey, id)
        return id
    }

    suspend fun settle(applicationId: String) = store.remove(unfinishedKeyOf(applicationId))

    suspend fun clear(runId: String, applicationId: String) {
        settle(applicationId)
        store.remove(keyOf(runId))
    }

    private fun keyOf(runId: String) = "tailoring.request.$runId"

    private fun unfinishedKeyOf(applicationId: String) = "tailoring.request.unfinished.$applicationId"
}
