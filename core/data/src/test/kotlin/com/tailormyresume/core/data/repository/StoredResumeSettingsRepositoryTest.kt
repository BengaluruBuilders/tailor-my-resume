package com.tailormyresume.core.data.repository

import com.google.common.truth.Truth.assertThat
import com.tailormyresume.core.model.FileNameFormat
import com.tailormyresume.core.model.PageSize
import com.tailormyresume.core.model.ResumeSettings
import com.tailormyresume.core.testing.mock.TestMockStateStore
import com.tailormyresume.core.testing.repository.ResumeSettingsRepositoryContractTest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Test

class StoredResumeSettingsRepositoryTest : ResumeSettingsRepositoryContractTest() {

    override fun createResumeSettingsRepository(): ResumeSettingsRepository =
        StoredResumeSettingsRepository(TestMockStateStore())

    @Test
    fun theLocalWipeBringsTheDefaultsBack() = runTest {
        val store = TestMockStateStore()
        val repository = StoredResumeSettingsRepository(store)
        repository.update { it.copy(pageSize = PageSize.LETTER, productUpdates = true) }

        store.clear()

        assertThat(repository.observeSettings().first()).isEqualTo(ResumeSettings())
    }

    @Test
    fun settingsSurviveARestartOfTheRepository() = runTest {
        val store = TestMockStateStore()
        StoredResumeSettingsRepository(store).update { it.copy(fileNameFormat = FileNameFormat.NAME_ROLE) }

        assertThat(StoredResumeSettingsRepository(store).observeSettings().first().fileNameFormat)
            .isEqualTo(FileNameFormat.NAME_ROLE)
    }

    @Test
    fun anUnknownStoredValueFallsBackToTheDefault() = runTest {
        val store = TestMockStateStore()
        store.write("resume.settings", """{"pageSize":"TABLOID","fileNameFormat":"NAME_ROLE","productUpdates":true}""")

        assertThat(StoredResumeSettingsRepository(store).observeSettings().first())
            .isEqualTo(ResumeSettings(PageSize.A4, FileNameFormat.NAME_ROLE, productUpdates = true))
    }
}
