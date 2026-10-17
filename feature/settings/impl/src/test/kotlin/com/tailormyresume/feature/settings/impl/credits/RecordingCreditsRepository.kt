package com.tailormyresume.feature.settings.impl.credits

import com.tailormyresume.core.data.repository.CreditsRepository
import com.tailormyresume.core.testing.repository.TestCreditsRepository

internal class RecordingCreditsRepository(
    private val delegate: TestCreditsRepository = TestCreditsRepository(),
) : CreditsRepository by delegate {
    val calls = mutableListOf<String>()
    override suspend fun refresh() {
        calls += "refresh"
        delegate.refresh()
    }
}
