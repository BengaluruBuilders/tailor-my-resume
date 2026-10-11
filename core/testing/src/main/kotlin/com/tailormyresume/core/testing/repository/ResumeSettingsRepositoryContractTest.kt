package com.tailormyresume.core.testing.repository

import com.google.common.truth.Truth.assertThat
import com.tailormyresume.core.data.repository.ResumeSettingsRepository
import com.tailormyresume.core.model.FileNameFormat
import com.tailormyresume.core.model.PageSize
import com.tailormyresume.core.model.ResumeSettings
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Test

abstract class ResumeSettingsRepositoryContractTest {

    protected abstract fun createResumeSettingsRepository(): ResumeSettingsRepository

    @Test
    fun defaultsPersistAndReturnAfterClear() = runTest {
        val repository = createResumeSettingsRepository()
        assertThat(repository.observeSettings().first())
            .isEqualTo(ResumeSettings(PageSize.A4, FileNameFormat.NAME_COMPANY_ROLE, productUpdates = false))

        repository.update { it.copy(pageSize = PageSize.LETTER) }
        repository.update { it.copy(fileNameFormat = FileNameFormat.NAME_RESUME) }
        repository.update { it.copy(productUpdates = true) }

        assertThat(repository.observeSettings().first())
            .isEqualTo(ResumeSettings(PageSize.LETTER, FileNameFormat.NAME_RESUME, productUpdates = true))

        repository.clear()

        assertThat(repository.observeSettings().first()).isEqualTo(ResumeSettings())
    }
}
