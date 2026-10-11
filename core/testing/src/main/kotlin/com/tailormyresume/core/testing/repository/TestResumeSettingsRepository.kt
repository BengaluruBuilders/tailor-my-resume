package com.tailormyresume.core.testing.repository

import com.tailormyresume.core.data.repository.ResumeSettingsRepository
import com.tailormyresume.core.model.ResumeSettings
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update

class TestResumeSettingsRepository : ResumeSettingsRepository {

    private val settings = MutableStateFlow(ResumeSettings())

    override fun observeSettings(): Flow<ResumeSettings> = settings

    override suspend fun update(change: (ResumeSettings) -> ResumeSettings) {
        settings.update(change)
    }

    override suspend fun clear() {
        settings.value = ResumeSettings()
    }
}
