package com.tailormyresume.core.data.mock

import javax.inject.Inject

class LegacyDataPurge @Inject constructor(
    private val store: MockStateStore,
) {
    suspend operator fun invoke() {
        LEGACY_PREFIXES.forEach { prefix -> store.removeWithPrefix(prefix) }
    }

    private companion object {
        val LEGACY_PREFIXES = listOf("coverletter.", "prep.plan.")
    }
}
