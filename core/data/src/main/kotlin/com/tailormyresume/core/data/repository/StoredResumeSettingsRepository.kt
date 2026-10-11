package com.tailormyresume.core.data.repository

import com.tailormyresume.core.data.mock.MockStateStore
import com.tailormyresume.core.data.mock.observeValue
import com.tailormyresume.core.data.mock.readValue
import com.tailormyresume.core.data.mock.writeValue
import com.tailormyresume.core.model.FileNameFormat
import com.tailormyresume.core.model.PageSize
import com.tailormyresume.core.model.ResumeSettings
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.Serializable
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class StoredResumeSettingsRepository @Inject constructor(
    private val store: MockStateStore,
) : ResumeSettingsRepository {

    private val mutex = Mutex()

    override fun observeSettings(): Flow<ResumeSettings> =
        store.observeValue(SETTINGS_KEY, ResumeSettingsDto.serializer()).map { it?.toModel() ?: ResumeSettings() }

    override suspend fun update(change: (ResumeSettings) -> ResumeSettings) {
        mutex.withLock {
            val current = store.readValue(SETTINGS_KEY, ResumeSettingsDto.serializer())?.toModel() ?: ResumeSettings()
            store.writeValue(SETTINGS_KEY, ResumeSettingsDto.serializer(), change(current).toDto())
        }
    }

    override suspend fun clear() {
        mutex.withLock { store.remove(SETTINGS_KEY) }
    }

    private companion object {
        const val SETTINGS_KEY = "resume.settings"
    }
}

@Serializable
private data class ResumeSettingsDto(
    val pageSize: String,
    val fileNameFormat: String,
    val productUpdates: Boolean,
) {
    fun toModel() = ResumeSettings(
        pageSize = PageSize.entries.find { it.name == pageSize } ?: PageSize.A4,
        fileNameFormat = FileNameFormat.entries.find { it.name == fileNameFormat } ?: FileNameFormat.NAME_COMPANY_ROLE,
        productUpdates = productUpdates,
    )
}

private fun ResumeSettings.toDto() = ResumeSettingsDto(pageSize.name, fileNameFormat.name, productUpdates)
