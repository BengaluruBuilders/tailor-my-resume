package com.tailormyresume.core.data.repository

import com.tailormyresume.core.model.ResumeSettings
import kotlinx.coroutines.flow.Flow

interface ResumeSettingsRepository {
    fun observeSettings(): Flow<ResumeSettings>

    suspend fun update(change: (ResumeSettings) -> ResumeSettings)

    suspend fun clear()
}
